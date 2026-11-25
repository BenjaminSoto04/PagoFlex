package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.PagoEntity
import com.example.pagoflex.data.model.ApiPagoRegistroRequest
import com.example.pagoflex.data.model.ApiPagoResponse
import com.example.pagoflex.data.remote.PagosApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import com.example.pagoflex.data.remote.fechaDesdeApi
import com.example.pagoflex.data.remote.fechaHoraDesdeApi
import com.example.pagoflex.data.remote.fechaHoraParaApi
import com.example.pagoflex.data.remote.fechaParaApi
import com.example.pagoflex.model.Pago
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

interface PagoRepository {
    fun getAll(): Flow<List<Pago>>
    suspend fun getAllSync(): List<Pago>
    suspend fun getByCompromiso(idCompromiso: Long): Pago?
    suspend fun insert(p: Pago)
    suspend fun update(p: Pago)
}

/**
 * Repositorio de pagos con integracion al microservicio del puerto 8084.
 *
 * - Lecturas: microservicio -> Room (upsert) -> flow local. Si falla, Room.
 * - Escrituras: Room (fuente de verdad) + replica best-effort al backend.
 */
class PagoRepositoryImpl(
    private val db: AppDatabase,
    private val apiService: PagosApiService = RetrofitClient.obtenerPagos()
) : PagoRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun toModel(e: PagoEntity) = Pago(
        id = e.id,
        numeroComprobante = e.numeroComprobante,
        idCompromiso = e.idCompromiso,
        fechaHora = e.fechaHora,
        montoPagado = e.montoPagado,
        canal = e.canal,
        medioPago = e.medioPago,
        folioUnico = e.folioUnico,
        cuotaPagada = e.cuotaPagada,
        fechaVencimientoCuota = e.fechaVencimientoCuota
    )

    private fun toEntity(p: Pago) = PagoEntity(
        id = p.id,
        numeroComprobante = p.numeroComprobante,
        idCompromiso = p.idCompromiso,
        fechaHora = p.fechaHora,
        montoPagado = p.montoPagado,
        canal = p.canal,
        medioPago = p.medioPago,
        folioUnico = p.folioUnico,
        cuotaPagada = p.cuotaPagada,
        fechaVencimientoCuota = p.fechaVencimientoCuota
    )

    override fun getAll(): Flow<List<Pago>> =
        db.pagoDao().getAll()
            .map { it.map(::toModel) }
            .onStart { refrescarEnSegundoPlano { sincronizar() } }

    override suspend fun getAllSync(): List<Pago> {
        // Sincronizacion en segundo plano; la lectura usa Room de inmediato.
        refrescarEnSegundoPlano { sincronizar() }
        return db.pagoDao().getAllSync().map(::toModel)
    }

    override suspend fun getByCompromiso(idCompromiso: Long): Pago? {
        val remotas = ejecutarRemotoConEspera { apiService.listarPorCompromiso(idCompromiso) }
        guardarRemotos(remotas)
        return db.pagoDao().getByCompromiso(idCompromiso)?.let(::toModel)
    }

    override suspend fun insert(p: Pago) {
        db.pagoDao().insert(toEntity(p))

        // El backend exige usuario y empresa; se resuelven desde el compromiso.
        val compromiso = db.compromisoDao().getById(p.idCompromiso) ?: return
        scope.launch {
            val respuesta = ejecutarRemoto {
                apiService.registrar(
                    ApiPagoRegistroRequest(
                        usuarioId = compromiso.idUsuario,
                        compromisoId = compromiso.id,
                        empresaId = compromiso.idEmpresa,
                        montoPagado = p.montoPagado,
                        cuotaPagada = p.cuotaPagada,
                        metodoPago = p.medioPago,
                        fechaPago = fechaHoraParaApi(p.fechaHora),
                        fechaVencimientoCuota = p.fechaVencimientoCuota?.let(::fechaParaApi),
                        folioUnico = p.folioUnico,
                        numeroComprobante = p.numeroComprobante,
                        canal = p.canal
                    )
                )
            }
            // Solo se replica la respuesta si conserva el mismo id local.
            if (respuesta?.id == p.id) {
                respuesta.aEntidad()?.let { db.pagoDao().insert(it) }
            }
        }
    }

    override suspend fun update(p: Pago) {
        // El microservicio de pagos no expone PUT: Room sigue siendo la fuente.
        db.pagoDao().update(toEntity(p))
    }

    private fun refrescarEnSegundoPlano(bloque: suspend () -> Unit) {
        scope.launch { bloque() }
    }

    private suspend fun sincronizar() {
        val remotas = ejecutarRemoto { apiService.listarTodos() }
        guardarRemotos(remotas)
    }

    private suspend fun guardarRemotos(remotas: List<ApiPagoResponse>?) {
        if (remotas.isNullOrEmpty()) return
        try {
            db.pagoDao().upsertAll(remotas.mapNotNull { it.aEntidad() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Respuesta del backend -> fila de Room, respetando el formato local de fechas. */
    private fun ApiPagoResponse.aEntidad(): PagoEntity? {
        val idLocal = id?.takeIf { it > 0 } ?: return null
        return PagoEntity(
            id = idLocal,
            numeroComprobante = numeroComprobante ?: "",
            idCompromiso = compromisoId ?: 0L,
            fechaHora = fechaHoraDesdeApi(fechaPago) ?: "",
            montoPagado = montoPagado ?: 0,
            canal = canal ?: "APP",
            medioPago = metodoPago ?: "",
            fotoUri = fotoUri,
            folioUnico = folioUnico,
            cuotaPagada = cuotaPagada,
            fechaVencimientoCuota = fechaDesdeApi(fechaVencimientoCuota)
        )
    }
}

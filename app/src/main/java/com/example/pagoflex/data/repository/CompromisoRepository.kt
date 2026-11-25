package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.CompromisoEntity
import com.example.pagoflex.data.model.ApiCompromisoActualizacionRequest
import com.example.pagoflex.data.model.ApiCompromisoPagoRequest
import com.example.pagoflex.data.model.ApiCompromisoRegistroRequest
import com.example.pagoflex.data.model.ApiCompromisoResponse
import com.example.pagoflex.data.model.ApiCompromisoVencimientoRequest
import com.example.pagoflex.data.model.ApiAnulacionRequest
import com.example.pagoflex.data.remote.CompromisosApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import com.example.pagoflex.data.remote.estadoCompromisoDesdeApi
import com.example.pagoflex.data.remote.estadoCompromisoParaApi
import com.example.pagoflex.data.remote.fechaDesdeApi
import com.example.pagoflex.data.remote.fechaParaApi
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

interface CompromisoRepository {
    fun getAll(): Flow<List<Compromiso>>
    suspend fun getAllSync(): List<Compromiso>
    fun getByUsuario(userId: Long): Flow<List<Compromiso>>
    suspend fun getById(id: Long): Compromiso?
    suspend fun nextId(): Long
    suspend fun insert(c: Compromiso)
    suspend fun update(c: Compromiso)
    suspend fun updateEstado(id: Long, estado: EstadoCompromiso)
    suspend fun anular(id: Long, motivo: String? = null)
    suspend fun delete(id: Long)
}

/**
 * Repositorio de compromisos con integracion al microservicio del puerto 8081.
 *
 * - Lecturas: microservicio -> Room (upsert) -> flow local. Si falla, Room.
 * - Escrituras: Room (fuente de verdad) + replica best-effort al backend.
 */
class CompromisoRepositoryImpl(
    private val db: AppDatabase,
    private val apiService: CompromisosApiService = RetrofitClient.obtenerCompromisos()
) : CompromisoRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun toModel(e: CompromisoEntity) = Compromiso(
        id = e.id,
        folio = e.folio,
        idUsuario = e.idUsuario,
        idEmpresa = e.idEmpresa,
        empresa = e.empresa,
        concepto = e.concepto,
        numeroCuota = e.numeroCuota,
        totalCuotas = e.totalCuotas,
        monto = e.monto,
        fechaVencimiento = e.fechaVencimiento,
        estado = runCatching { EstadoCompromiso.valueOf(e.estado) }
            .getOrDefault(EstadoCompromiso.PENDIENTE),
        recargoAplicado = e.recargoAplicado,
        motivoAnulacion = e.motivoAnulacion,
        folioUnico = e.folioUnico
    )

    private fun toEntity(c: Compromiso) = CompromisoEntity(
        id = c.id,
        folio = c.folio,
        idUsuario = c.idUsuario,
        idEmpresa = c.idEmpresa,
        empresa = c.empresa,
        concepto = c.concepto,
        numeroCuota = c.numeroCuota,
        totalCuotas = c.totalCuotas,
        monto = c.monto,
        fechaVencimiento = c.fechaVencimiento,
        estado = c.estado.name,
        recargoAplicado = c.recargoAplicado,
        motivoAnulacion = c.motivoAnulacion,
        folioUnico = c.folioUnico
    )

    override fun getAll(): Flow<List<Compromiso>> =
        db.compromisoDao().getAll()
            .map { it.map(::toModel) }
            .onStart { refrescarEnSegundoPlano { sincronizar() } }

    override suspend fun getAllSync(): List<Compromiso> {
        // La sincronizacion corre en segundo plano; la lectura usa Room de inmediato.
        refrescarEnSegundoPlano { sincronizar() }
        return db.compromisoDao().getAllSync().map(::toModel)
    }

    override fun getByUsuario(userId: Long): Flow<List<Compromiso>> =
        db.compromisoDao().getByUsuario(userId)
            .map { it.map(::toModel) }
            .onStart {
                refrescarEnSegundoPlano {
                    val remotas = ejecutarRemoto {
                        apiService.listarPorUsuario(userId.toInt())
                    }
                    guardarRemotos(remotas)
                }
            }

    override suspend fun getById(id: Long): Compromiso? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerPorId(id.toInt()) }
        if (remota != null) {
            val entidad = remota.aEntidad()
            if (entidad != null) db.compromisoDao().insert(entidad)
            entidad?.let { return toModel(it) }
        }
        return db.compromisoDao().getById(id)?.let(::toModel)
    }

    override suspend fun nextId(): Long = (db.compromisoDao().getMaxId() ?: 0L) + 1L

    override suspend fun insert(c: Compromiso) {
        db.compromisoDao().insert(toEntity(c))
        scope.launch {
            ejecutarRemoto {
                apiService.registrar(
                    ApiCompromisoRegistroRequest(
                        idUsuarioFinal = c.idUsuario.toInt(),
                        idEmpresa = c.idEmpresa.toInt(),
                        concepto = c.concepto,
                        numeroCuota = c.numeroCuota,
                        totalCuotas = c.totalCuotas,
                        monto = c.monto,
                        fechaVencimiento = fechaParaApi(c.fechaVencimiento),
                        sistemaOrigen = "ANDROID",
                        idInstanciaOrigen = c.folioUnico
                    )
                )
            }
        }
    }

    override suspend fun update(c: Compromiso) {
        val anterior = db.compromisoDao().getById(c.id)
        db.compromisoDao().update(toEntity(c))

        scope.launch {
            ejecutarRemoto {
                apiService.actualizar(
                    c.id.toInt(),
                    ApiCompromisoActualizacionRequest(
                        concepto = c.concepto,
                        numeroCuota = c.numeroCuota,
                        totalCuotas = c.totalCuotas,
                        monto = c.monto,
                        fechaVencimiento = fechaParaApi(c.fechaVencimiento)
                    )
                )
            }
            if (anterior?.estado != c.estado.name) {
                enviarEstado(c)
            }
        }
    }

    override suspend fun updateEstado(id: Long, estado: EstadoCompromiso) {
        db.compromisoDao().updateEstado(id, estado.name)
        val entidad = db.compromisoDao().getById(id) ?: return
        scope.launch { enviarEstado(toModel(entidad)) }
    }

    override suspend fun anular(id: Long, motivo: String?) {
        db.compromisoDao().updateEstadoConMotivo(
            id,
            EstadoCompromiso.ANULADO.name,
            motivo?.trim()?.takeIf { it.isNotEmpty() }
        )
        val motivoFinal = motivo?.trim()?.takeIf { it.isNotEmpty() } ?: "Anulación desde app PagoFlex"
        scope.launch {
            val idEjecutivo = (db.userDao().getDefaultByRol("EJECUTIVO")?.id ?: 1L).toInt()
            ejecutarRemoto {
                apiService.anular(
                    id.toInt(),
                    ApiAnulacionRequest(idEjecutivo = idEjecutivo, motivo = motivoFinal)
                )
            }
        }
    }

    override suspend fun delete(id: Long) = db.compromisoDao().deleteById(id)

    private fun refrescarEnSegundoPlano(bloque: suspend () -> Unit) {
        scope.launch { bloque() }
    }

    /** Trae todos los compromisos del backend y los persiste en Room. */
    private suspend fun sincronizar() {
        val remotas = ejecutarRemoto { apiService.listarTodos() }
        guardarRemotos(remotas)
    }

    private suspend fun guardarRemotos(remotas: List<ApiCompromisoResponse>?) {
        if (remotas.isNullOrEmpty()) return
        try {
            val entidades = remotas.mapNotNull { it.aEntidad() }
            if (entidades.isNotEmpty()) db.compromisoDao().upsertAll(entidades)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Notifica el cambio de estado al microservicio segun el endpoint que corresponde. */
    private suspend fun enviarEstado(c: Compromiso) {
        val id = c.id.toInt()
        when (c.estado) {
            EstadoCompromiso.EN_REVISION -> ejecutarRemoto { apiService.actualizarRevision(id) }

            EstadoCompromiso.PAGADO,
            EstadoCompromiso.VENCIDO -> ejecutarRemoto {
                apiService.actualizarPorPago(
                    id,
                    ApiCompromisoPagoRequest(
                        estado = estadoCompromisoParaApi(c.estado),
                        numeroCuota = c.numeroCuota,
                        fechaVencimiento = fechaParaApi(c.fechaVencimiento),
                        recargoAplicado = c.recargoAplicado
                    )
                )
            }

            EstadoCompromiso.PENDIENTE -> ejecutarRemoto {
                apiService.actualizarPorVencimiento(
                    id,
                    ApiCompromisoVencimientoRequest(
                        estado = estadoCompromisoParaApi(c.estado),
                        recargoAplicado = c.recargoAplicado
                    )
                )
            }

            EstadoCompromiso.ANULADO -> Unit // se envia por POST /{id}/anulacion
        }
    }

    /**
     * Convierte la respuesta del backend. Conserva `folioUnico` y
     * `motivoAnulacion`, que son datos solo locales.
     */
    private suspend fun ApiCompromisoResponse.aEntidad(): CompromisoEntity? {
        val idLocal = idCompromiso?.toLong()?.takeIf { it > 0 } ?: return null
        val existente = db.compromisoDao().getById(idLocal)
        val empresaId = idEmpresa?.toLong() ?: existente?.idEmpresa ?: 0L
        val nombreEmpresa = existente?.empresa
            ?: db.empresaDao().getById(empresaId)?.nombreFantasia
            ?: ""

        return CompromisoEntity(
            id = idLocal,
            folio = folio ?: existente?.folio ?: "",
            idUsuario = idUsuarioFinal?.toLong() ?: existente?.idUsuario ?: 0L,
            idEmpresa = empresaId,
            empresa = nombreEmpresa,
            concepto = concepto ?: existente?.concepto ?: "",
            numeroCuota = numeroCuota ?: existente?.numeroCuota,
            totalCuotas = totalCuotas ?: existente?.totalCuotas,
            monto = monto ?: existente?.monto ?: 0,
            fechaVencimiento = fechaDesdeApi(fechaVencimiento)
                ?: existente?.fechaVencimiento
                ?: "",
            estado = estadoCompromisoDesdeApi(estado).name,
            recargoAplicado = recargoAplicado ?: existente?.recargoAplicado ?: 0,
            motivoAnulacion = existente?.motivoAnulacion,
            folioUnico = existente?.folioUnico
        )
    }
}

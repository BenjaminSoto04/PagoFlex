package com.example.pagoflex.data.repository

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch

interface PagoRepository {
    fun getAll(): Flow<List<Pago>>
    suspend fun getAllSync(): List<Pago>
    suspend fun getByCompromiso(idCompromiso: Long): Pago?
    suspend fun insert(p: Pago)
    suspend fun update(p: Pago)
}

class PagoRepositoryImpl(
    private val apiService: PagosApiService = RetrofitClient.obtenerPagos()
) : PagoRepository {

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
            folioUnico = folioUnico,
            cuotaPagada = cuotaPagada,
            fechaVencimientoCuota = fechaDesdeApi(fechaVencimientoCuota)
        )
    }

    override fun getAll(): Flow<List<Pago>> = flow {
        val response = apiService.listarTodos()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.mapNotNull { it.aEntidad() }.map(::toModel))
        } else {
            emit(emptyList())
        }
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(emptyList())
    }

    override suspend fun getAllSync(): List<Pago> {
        val list = mutableListOf<Pago>()
        getAll().collect { list.addAll(it) }
        return list
    }

    override suspend fun getByCompromiso(idCompromiso: Long): Pago? {
        return try {
            val response = apiService.listarPorCompromiso(idCompromiso)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.firstOrNull()?.aEntidad()?.let(::toModel)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun insert(p: Pago) {
        val compromisoResponse = RetrofitClient.obtenerCompromisos().obtenerPorId(p.idCompromiso.toInt())
        val compromiso = compromisoResponse.body()
            ?: throw IllegalStateException("Compromiso no encontrado para el pago")

        val response = apiService.registrar(
            ApiPagoRegistroRequest(
                usuarioId = (compromiso.idUsuarioFinal ?: 0).toLong(),
                compromisoId = (compromiso.idCompromiso ?: p.idCompromiso.toInt()).toLong(),
                empresaId = (compromiso.idEmpresa ?: 0).toLong(),
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
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("No se pudo registrar el pago en el backend")
        }
    }

    override suspend fun update(p: Pago) {
        // El microservicio de pagos no expone PUT; se omite la actualizacion remota.
    }
}




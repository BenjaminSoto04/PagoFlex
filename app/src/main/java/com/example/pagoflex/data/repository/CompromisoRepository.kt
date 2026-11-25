package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.CompromisoEntity
import com.example.pagoflex.data.model.ApiAnulacionRequest
import com.example.pagoflex.data.model.ApiCompromisoActualizacionRequest
import com.example.pagoflex.data.model.ApiCompromisoPagoRequest
import com.example.pagoflex.data.model.ApiCompromisoRegistroRequest
import com.example.pagoflex.data.model.ApiCompromisoResponse
import com.example.pagoflex.data.model.ApiCompromisoVencimientoRequest
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch

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

class CompromisoRepositoryImpl(
    private val apiService: CompromisosApiService = RetrofitClient.obtenerCompromisos()
) : CompromisoRepository {

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
        estado = runCatching { EstadoCompromiso.valueOf(e.estado) }.getOrDefault(EstadoCompromiso.PENDIENTE),
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

    private fun ApiCompromisoResponse.aEntidad(): CompromisoEntity? {
        val idLocal = idCompromiso?.toLong()?.takeIf { it > 0 } ?: return null
        return CompromisoEntity(
            id = idLocal,
            folio = folio ?: "",
            idUsuario = idUsuarioFinal?.toLong() ?: 0L,
            idEmpresa = idEmpresa?.toLong() ?: 0L,
            empresa = "",
            concepto = concepto ?: "",
            numeroCuota = numeroCuota,
            totalCuotas = totalCuotas,
            monto = monto ?: 0,
            fechaVencimiento = fechaDesdeApi(fechaVencimiento) ?: "",
            estado = estadoCompromisoDesdeApi(estado).name,
            recargoAplicado = recargoAplicado ?: 0,
            motivoAnulacion = null,
            folioUnico = idInstanciaOrigen
        )
    }

    override fun getAll(): Flow<List<Compromiso>> = flow {
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

    override suspend fun getAllSync(): List<Compromiso> = getAll().let { flow ->
        val list = mutableListOf<Compromiso>()
        flow.collect { list.addAll(it) }
        list
    }

    override fun getByUsuario(userId: Long): Flow<List<Compromiso>> = flow {
        val response = apiService.listarPorUsuario(userId.toInt())
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

    override suspend fun getById(id: Long): Compromiso? {
        return try {
            val response = apiService.obtenerPorId(id.toInt())
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.aEntidad()?.let(::toModel)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun nextId(): Long = 0L

    override suspend fun insert(c: Compromiso) {
        val response = apiService.registrar(
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
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("No se pudo registrar el compromiso en el backend")
        }
    }

    override suspend fun update(c: Compromiso) {
        val response = apiService.actualizar(
            c.id.toInt(),
            ApiCompromisoActualizacionRequest(
                concepto = c.concepto,
                numeroCuota = c.numeroCuota,
                totalCuotas = c.totalCuotas,
                monto = c.monto,
                fechaVencimiento = fechaParaApi(c.fechaVencimiento)
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo actualizar el compromiso en el backend")
        }
    }

    override suspend fun updateEstado(id: Long, estado: EstadoCompromiso) {
        val compromiso = getById(id) ?: return
        when (estado) {
            EstadoCompromiso.EN_REVISION -> ejecutarRemoto { apiService.actualizarRevision(id.toInt()) }
            EstadoCompromiso.PAGADO, EstadoCompromiso.VENCIDO -> ejecutarRemoto {
                apiService.actualizarPorPago(
                    id.toInt(),
                    ApiCompromisoPagoRequest(
                        estado = estadoCompromisoParaApi(estado),
                        numeroCuota = compromiso.numeroCuota,
                        fechaVencimiento = fechaParaApi(compromiso.fechaVencimiento),
                        recargoAplicado = compromiso.recargoAplicado
                    )
                )
            }
            EstadoCompromiso.PENDIENTE -> ejecutarRemoto {
                apiService.actualizarPorVencimiento(
                    id.toInt(),
                    ApiCompromisoVencimientoRequest(
                        estado = estadoCompromisoParaApi(estado),
                        recargoAplicado = compromiso.recargoAplicado
                    )
                )
            }
            EstadoCompromiso.ANULADO -> Unit
        }
    }

    override suspend fun anular(id: Long, motivo: String?) {
        val motivoFinal = motivo?.trim()?.takeIf { it.isNotEmpty() } ?: "Anulacion desde app PagoFlex"
        val idEjecutivo = 1
        val response = apiService.anular(
            id.toInt(),
            ApiAnulacionRequest(idEjecutivo = idEjecutivo, motivo = motivoFinal)
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo anular el compromiso en el backend")
        }
    }

    override suspend fun delete(id: Long) {
        // No existe endpoint de eliminacion en el microservicio.
    }
}




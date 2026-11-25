package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.ReporteEntity
import com.example.pagoflex.data.local.entity.ReporteMensajeEntity
import com.example.pagoflex.data.model.ApiReporteEstadoRequest
import com.example.pagoflex.data.model.ApiReporteMensajeRequest
import com.example.pagoflex.data.model.ApiReporteMensajeResponse
import com.example.pagoflex.data.model.ApiReporteRegistroRequest
import com.example.pagoflex.data.model.ApiReporteResponse
import com.example.pagoflex.data.remote.ReportesApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import java.util.Locale

data class Reporte(
    val id: Long = 0,
    val tipo: String,
    val compromiso: String,
    val descripcion: String,
    val fecha: String,
    val idUsuario: Long,
    val estado: String = "PENDIENTE"
)

data class ReporteMensaje(
    val id: Long = 0,
    val idReporte: Long,
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)

interface ReporteRepository {
    suspend fun insert(reporte: Reporte): Long
    fun getAll(): Flow<List<Reporte>>
    fun getByUsuario(idUsuario: Long): Flow<List<Reporte>>
    fun observeById(id: Long): Flow<Reporte?>
    suspend fun getById(id: Long): Reporte?
    suspend fun actualizarEstado(id: Long, estado: String)
    fun getMensajes(idReporte: Long): Flow<List<ReporteMensaje>>
    suspend fun enviarMensaje(
        idReporte: Long,
        idRemitente: Long,
        rolRemitente: String,
        contenido: String,
        fechaHora: String
    )
}

class ReporteRepositoryImpl(
    private val apiService: ReportesApiService = RetrofitClient.obtenerReportes()
) : ReporteRepository {

    private val estadosValidos = setOf("PENDIENTE", "EN_PROCESO", "FINALIZADO")

    private fun toModel(e: ReporteEntity) = Reporte(
        id = e.id,
        tipo = e.tipo,
        compromiso = e.compromiso,
        descripcion = e.descripcion,
        fecha = e.fecha,
        idUsuario = e.idUsuario,
        estado = e.estado
    )

    private fun toModel(e: ReporteMensajeEntity) = ReporteMensaje(
        id = e.id,
        idReporte = e.idReporte,
        idRemitente = e.idRemitente,
        rolRemitente = e.rolRemitente,
        contenido = e.contenido,
        fechaHora = e.fechaHora
    )

    private fun ApiReporteResponse.aEntidad(): ReporteEntity? {
        val idLocal = id?.takeIf { it > 0 } ?: return null
        return ReporteEntity(
            id = idLocal,
            tipo = tipo ?: "",
            compromiso = compromiso ?: "",
            descripcion = descripcion ?: "",
            fecha = fecha ?: "",
            idUsuario = idUsuario ?: 0L,
            estado = normalizarEstado(estado ?: "PENDIENTE")
        )
    }

    private fun ApiReporteMensajeResponse.aEntidad(): ReporteMensajeEntity? {
        val idLocal = id?.takeIf { it > 0 } ?: return null
        return ReporteMensajeEntity(
            id = idLocal,
            idReporte = idReporte ?: 0L,
            idRemitente = idRemitente ?: 0L,
            rolRemitente = rolRemitente ?: "USUARIO",
            contenido = contenido ?: "",
            fechaHora = fechaHora ?: ""
        )
    }

    private fun normalizarEstado(estado: String): String {
        val valor = estado.trim().uppercase(Locale.ROOT).replace(' ', '_')
        return if (valor in estadosValidos) valor else "PENDIENTE"
    }

    override suspend fun insert(reporte: Reporte): Long {
        val response = apiService.crear(
            ApiReporteRegistroRequest(
                tipo = reporte.tipo,
                compromiso = reporte.compromiso,
                descripcion = reporte.descripcion,
                fecha = reporte.fecha,
                idUsuario = reporte.idUsuario
            )
        )
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("No se pudo registrar el reporte en el backend")
        }
        val remoto = response.body()!!
        val id = remoto.id ?: 0L

        val entidad = remoto.aEntidad() ?: ReporteEntity(
            id = id,
            tipo = reporte.tipo,
            compromiso = reporte.compromiso,
            descripcion = reporte.descripcion,
            fecha = reporte.fecha,
            idUsuario = reporte.idUsuario,
            estado = reporte.estado
        )
        // No Room: se conserva solo en memoria de la respuesta.
        return id
    }

    override fun getAll(): Flow<List<Reporte>> = flow {
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

    override fun getByUsuario(idUsuario: Long): Flow<List<Reporte>> = flow {
        val response = apiService.listarPorUsuario(idUsuario)
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

    override fun observeById(id: Long): Flow<Reporte?> = flow {
        val response = apiService.obtenerPorId(id)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.aEntidad()?.let(::toModel))
        } else {
            emit(null)
        }
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(null)
    }

    override suspend fun getById(id: Long): Reporte? {
        return try {
            val response = apiService.obtenerPorId(id)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.aEntidad()?.let(::toModel)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun actualizarEstado(id: Long, estado: String) {
        val estadoLocal = normalizarEstado(estado)
        val response = apiService.actualizarEstado(id, ApiReporteEstadoRequest(estadoLocal))
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo actualizar el estado del reporte en el backend")
        }
    }

    override fun getMensajes(idReporte: Long): Flow<List<ReporteMensaje>> = flow {
        val response = apiService.listarMensajes(idReporte)
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

    override suspend fun enviarMensaje(
        idReporte: Long,
        idRemitente: Long,
        rolRemitente: String,
        contenido: String,
        fechaHora: String
    ) {
        val response = apiService.registrarMensaje(
            idReporte,
            ApiReporteMensajeRequest(
                idRemitente = idRemitente,
                rolRemitente = rolRemitente,
                contenido = contenido,
                fechaHora = fechaHora
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo enviar el mensaje del reporte en el backend")
        }
    }
}




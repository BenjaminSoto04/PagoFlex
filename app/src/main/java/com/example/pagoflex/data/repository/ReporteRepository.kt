package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
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

/**
 * Repositorio de reportes con integracion al microservicio del puerto 8086.
 *
 * - Lecturas: microservicio -> Room (upsert) -> flow local. Si falla, Room.
 * - Escrituras: Room (fuente de verdad) + replica best-effort al backend.
 */
class ReporteRepositoryImpl(
    private val db: AppDatabase,
    private val apiService: ReportesApiService = RetrofitClient.obtenerReportes()
) : ReporteRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    override suspend fun insert(reporte: Reporte): Long {
        val id = db.reporteDao().insert(
            ReporteEntity(
                id = reporte.id,
                tipo = reporte.tipo,
                compromiso = reporte.compromiso,
                descripcion = reporte.descripcion,
                fecha = reporte.fecha,
                idUsuario = reporte.idUsuario,
                estado = reporte.estado
            )
        )
        // La descripcion inicial forma parte del hilo del reporte.
        db.reporteDao().insertMensaje(
            ReporteMensajeEntity(
                idReporte = id,
                idRemitente = reporte.idUsuario,
                rolRemitente = "USUARIO",
                contenido = reporte.descripcion,
                fechaHora = reporte.fecha
            )
        )

        scope.launch {
            ejecutarRemoto {
                apiService.crear(
                    ApiReporteRegistroRequest(
                        tipo = reporte.tipo,
                        compromiso = reporte.compromiso,
                        descripcion = reporte.descripcion,
                        fecha = reporte.fecha,
                        idUsuario = reporte.idUsuario
                    )
                )
            }
        }
        return id
    }

    override fun getAll(): Flow<List<Reporte>> =
        db.reporteDao().getAll()
            .map { list -> list.map(::toModel) }
            .onStart { refrescarEnSegundoPlano { sincronizarTodos() } }

    override fun getByUsuario(idUsuario: Long): Flow<List<Reporte>> =
        db.reporteDao().getByUsuario(idUsuario)
            .map { list -> list.map(::toModel) }
            .onStart { refrescarEnSegundoPlano { sincronizarPorUsuario(idUsuario) } }

    override fun observeById(id: Long): Flow<Reporte?> =
        db.reporteDao().observeById(id)
            .map { it?.let(::toModel) }
            .onStart { refrescarEnSegundoPlano { sincronizarTodos() } }

    override suspend fun getById(id: Long): Reporte? {
        val remoto = ejecutarRemotoConEspera { apiService.obtenerPorId(id) }
        if (remoto != null) {
            val entidad = remoto.aEntidad()
            if (entidad != null) db.reporteDao().upsert(entidad)
            entidad?.let { return toModel(it) }
        }
        return db.reporteDao().getById(id)?.let(::toModel)
    }

    override suspend fun actualizarEstado(id: Long, estado: String) {
        val estadoLocal = normalizarEstado(estado)
        db.reporteDao().updateEstado(id, estadoLocal)
        scope.launch {
            ejecutarRemoto {
                apiService.actualizarEstado(id, ApiReporteEstadoRequest(estadoLocal))
            }
        }
    }

    override fun getMensajes(idReporte: Long): Flow<List<ReporteMensaje>> =
        db.reporteDao().getMensajes(idReporte)
            .map { list -> list.map(::toModel) }
            .onStart { refrescarEnSegundoPlano { sincronizarMensajes(idReporte) } }

    override suspend fun enviarMensaje(
        idReporte: Long,
        idRemitente: Long,
        rolRemitente: String,
        contenido: String,
        fechaHora: String
    ) {
        db.reporteDao().insertMensaje(
            ReporteMensajeEntity(
                idReporte = idReporte,
                idRemitente = idRemitente,
                rolRemitente = rolRemitente,
                contenido = contenido,
                fechaHora = fechaHora
            )
        )
        scope.launch {
            ejecutarRemoto {
                apiService.registrarMensaje(
                    idReporte,
                    ApiReporteMensajeRequest(
                        idRemitente = idRemitente,
                        rolRemitente = rolRemitente,
                        contenido = contenido,
                        fechaHora = fechaHora
                    )
                )
            }
        }
    }

    private fun refrescarEnSegundoPlano(bloque: suspend () -> Unit) {
        scope.launch { bloque() }
    }

    private suspend fun sincronizarTodos() {
        val remotos = ejecutarRemoto { apiService.listarTodos() }
        guardarReportes(remotos)
    }

    private suspend fun sincronizarPorUsuario(idUsuario: Long) {
        val remotos = ejecutarRemoto { apiService.listarPorUsuario(idUsuario) }
        guardarReportes(remotos)
    }

    private suspend fun guardarReportes(remotos: List<ApiReporteResponse>?) {
        if (remotos.isNullOrEmpty()) return
        try {
            db.reporteDao().upsertAll(remotos.mapNotNull { it.aEntidad() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun sincronizarMensajes(idReporte: Long) {
        val remotos = ejecutarRemoto { apiService.listarMensajes(idReporte) } ?: return
        try {
            val mensajes = remotos.mapNotNull { it.aEntidad() }
            if (mensajes.isNotEmpty()) db.reporteDao().upsertMensajes(mensajes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun normalizarEstado(estado: String): String {
        val valor = estado.trim().uppercase(Locale.ROOT).replace(' ', '_')
        return if (valor in estadosValidos) valor else "PENDIENTE"
    }

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
}

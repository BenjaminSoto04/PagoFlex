package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.ReporteEntity
import com.example.pagoflex.data.local.entity.ReporteMensajeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

class ReporteRepositoryImpl(private val db: AppDatabase) : ReporteRepository {

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
        return id
    }

    override fun getAll(): Flow<List<Reporte>> =
        db.reporteDao().getAll().map { list -> list.map(::toModel) }

    override fun getByUsuario(idUsuario: Long): Flow<List<Reporte>> =
        db.reporteDao().getByUsuario(idUsuario).map { list -> list.map(::toModel) }

    override fun observeById(id: Long): Flow<Reporte?> =
        db.reporteDao().observeById(id).map { it?.let(::toModel) }

    override suspend fun getById(id: Long): Reporte? =
        db.reporteDao().getById(id)?.let(::toModel)

    override suspend fun actualizarEstado(id: Long, estado: String) {
        db.reporteDao().updateEstado(id, estado)
    }

    override fun getMensajes(idReporte: Long): Flow<List<ReporteMensaje>> =
        db.reporteDao().getMensajes(idReporte).map { list -> list.map(::toModel) }

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
    }
}

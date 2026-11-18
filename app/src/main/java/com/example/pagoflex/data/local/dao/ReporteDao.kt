package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pagoflex.data.local.entity.ReporteEntity
import com.example.pagoflex.data.local.entity.ReporteMensajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Insert
    suspend fun insert(r: ReporteEntity): Long

    @Query("SELECT * FROM reportes ORDER BY id DESC")
    fun getAll(): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes WHERE idUsuario = :idUsuario ORDER BY id DESC")
    fun getByUsuario(idUsuario: Long): Flow<List<ReporteEntity>>

    @Query("SELECT * FROM reportes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReporteEntity?

    @Query("SELECT * FROM reportes WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ReporteEntity?>

    @Query("SELECT COUNT(*) FROM reportes WHERE idUsuario = :idUsuario")
    suspend fun countByUsuario(idUsuario: Long): Int

    @Query("UPDATE reportes SET estado = :estado WHERE id = :id")
    suspend fun updateEstado(id: Long, estado: String)

    @Insert
    suspend fun insertMensaje(m: ReporteMensajeEntity): Long

    @Query("SELECT * FROM reporte_mensajes WHERE idReporte = :idReporte ORDER BY id ASC")
    fun getMensajes(idReporte: Long): Flow<List<ReporteMensajeEntity>>

    @Query("SELECT * FROM reporte_mensajes WHERE idReporte = :idReporte ORDER BY id ASC")
    suspend fun getMensajesSync(idReporte: Long): List<ReporteMensajeEntity>
}

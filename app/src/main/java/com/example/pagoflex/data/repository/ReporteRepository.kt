package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.ReporteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Reporte(val id: Long = 0, val tipo: String, val compromiso: String, val descripcion: String, val fecha: String, val idUsuario: Long)

interface ReporteRepository {
    suspend fun insert(reporte: Reporte): Long
    fun getAll(): Flow<List<Reporte>>
}

class ReporteRepositoryImpl(private val db: AppDatabase) : ReporteRepository {
    override suspend fun insert(reporte: Reporte): Long =
        db.reporteDao().insert(ReporteEntity(id = reporte.id, tipo = reporte.tipo, compromiso = reporte.compromiso, descripcion = reporte.descripcion, fecha = reporte.fecha, idUsuario = reporte.idUsuario))
    override fun getAll(): Flow<List<Reporte>> = db.reporteDao().getAll().map { list -> list.map { Reporte(it.id, it.tipo, it.compromiso, it.descripcion, it.fecha, it.idUsuario) } }
}


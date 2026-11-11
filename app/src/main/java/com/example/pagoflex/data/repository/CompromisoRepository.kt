package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.CompromisoEntity
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface CompromisoRepository {
    fun getAll(): Flow<List<Compromiso>>
    fun getByUsuario(userId: Long): Flow<List<Compromiso>>
    suspend fun getById(id: Long): Compromiso?
    suspend fun insert(c: Compromiso)
    suspend fun update(c: Compromiso)
    suspend fun updateEstado(id: Long, estado: EstadoCompromiso)
    suspend fun anular(id: Long)
    suspend fun delete(id: Long)
}

class CompromisoRepositoryImpl(private val db: AppDatabase) : CompromisoRepository {
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
        estado = EstadoCompromiso.valueOf(e.estado),
        recargoAplicado = e.recargoAplicado
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
        recargoAplicado = c.recargoAplicado
    )

    override fun getAll(): Flow<List<Compromiso>> = db.compromisoDao().getAll().map { it.map(::toModel) }
    override fun getByUsuario(userId: Long): Flow<List<Compromiso>> = db.compromisoDao().getByUsuario(userId).map { it.map(::toModel) }
    override suspend fun getById(id: Long): Compromiso? = db.compromisoDao().getById(id)?.let(::toModel)
    override suspend fun insert(c: Compromiso) = db.compromisoDao().insert(toEntity(c))
    override suspend fun update(c: Compromiso) = db.compromisoDao().update(toEntity(c))
    override suspend fun updateEstado(id: Long, estado: EstadoCompromiso) = db.compromisoDao().updateEstado(id, estado.name)
    override suspend fun anular(id: Long) = db.compromisoDao().updateEstado(id, EstadoCompromiso.ANULADO.name)
    override suspend fun delete(id: Long) = db.compromisoDao().deleteById(id)
}

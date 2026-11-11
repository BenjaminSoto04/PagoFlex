package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.PagoEntity
import com.example.pagoflex.model.Pago
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PagoRepository {
    fun getAll(): Flow<List<Pago>>
    suspend fun getByCompromiso(idCompromiso: Long): Pago?
    suspend fun insert(p: Pago)
    suspend fun updateFoto(id: Long, uri: String)
}

class PagoRepositoryImpl(private val db: AppDatabase) : PagoRepository {
    private fun toModel(e: PagoEntity) = Pago(e.id, e.numeroComprobante, e.idCompromiso, e.fechaHora, e.montoPagado, e.canal, e.medioPago, e.fotoUri)
    private fun toEntity(p: Pago) = PagoEntity(p.id, p.numeroComprobante, p.idCompromiso, p.fechaHora, p.montoPagado, p.canal, p.medioPago, p.fotoUri)

    override fun getAll(): Flow<List<Pago>> = db.pagoDao().getAll().map { it.map(::toModel) }
    override suspend fun getByCompromiso(idCompromiso: Long): Pago? = db.pagoDao().getByCompromiso(idCompromiso)?.let(::toModel)
    override suspend fun insert(p: Pago) = db.pagoDao().insert(toEntity(p))
    override suspend fun updateFoto(id: Long, uri: String) = db.pagoDao().updateFoto(id, uri)
}


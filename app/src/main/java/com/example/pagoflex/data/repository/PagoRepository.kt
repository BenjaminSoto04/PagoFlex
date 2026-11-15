package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.PagoEntity
import com.example.pagoflex.model.Pago
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PagoRepository {
    fun getAll(): Flow<List<Pago>>
    suspend fun getAllSync(): List<Pago>
    suspend fun getByCompromiso(idCompromiso: Long): Pago?
    suspend fun insert(p: Pago)
    suspend fun update(p: Pago)
}

class PagoRepositoryImpl(private val db: AppDatabase) : PagoRepository {
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

    override fun getAll(): Flow<List<Pago>> = db.pagoDao().getAll().map { it.map(::toModel) }
    override suspend fun getAllSync(): List<Pago> = db.pagoDao().getAllSync().map(::toModel)
    override suspend fun getByCompromiso(idCompromiso: Long): Pago? = db.pagoDao().getByCompromiso(idCompromiso)?.let(::toModel)
    override suspend fun insert(p: Pago) = db.pagoDao().insert(toEntity(p))
    override suspend fun update(p: Pago) = db.pagoDao().update(toEntity(p))
}


package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pagos")
data class PagoEntity(
    @PrimaryKey val id: Long,
    val numeroComprobante: String,
    val idCompromiso: Long,
    val fechaHora: String,
    val montoPagado: Int,
    val canal: String,
    val medioPago: String,
    val fotoUri: String? = null
)

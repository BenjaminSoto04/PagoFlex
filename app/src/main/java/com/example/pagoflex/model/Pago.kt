package com.example.pagoflex.model

data class Pago(
    val id: Long,
    val numeroComprobante: String,
    val idCompromiso: Long,
    val fechaHora: String,
    val montoPagado: Int,
    val canal: String,
    val medioPago: String
)

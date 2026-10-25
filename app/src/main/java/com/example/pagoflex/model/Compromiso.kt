package com.example.pagoflex.model

data class Compromiso(
    val id: Long,
    val folio: String,
    val idUsuario: Long,
    val idEmpresa: Long,
    val empresa: String,
    val concepto: String,
    val numeroCuota: Int?,
    val totalCuotas: Int?,
    val monto: Int,
    val fechaVencimiento: String,
    val estado: EstadoCompromiso,
    val recargoAplicado: Int = 0
)

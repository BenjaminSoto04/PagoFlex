package com.example.pagoflex.data.local.entity


data class CompromisoEntity(
    val id: Long = 0,
    val folio: String,
    val idUsuario: Long,
    val idEmpresa: Long,
    val empresa: String,
    val concepto: String,
    val numeroCuota: Int?,
    val totalCuotas: Int?,
    val monto: Int,
    val fechaVencimiento: String,
    val estado: String,
    val recargoAplicado: Int = 0,
    val motivoAnulacion: String? = null,
    val folioUnico: String? = null
)

package com.example.pagoflex.model

data class Compromiso(
    val id: Int,
    val folio: String,
    val usuarioId: Int,
    val empresaId: Int,
    val nombreEmpresa: String,
    val concepto: String,
    val numeroCuota: Int?,
    val totalCuotas: Int?,
    val monto: Long,
    val fechaVencimiento: String,
    val estado: EstadoCompromiso,
    val recargoAplicado: Long = 0
)

enum class EstadoCompromiso {
    PENDIENTE,
    VENCIDO,
    PAGADO,
    EN_REVISION,
    ANULADO
}

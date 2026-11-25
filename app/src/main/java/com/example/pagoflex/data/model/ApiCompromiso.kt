package com.example.pagoflex.data.model

/**
 * DTOs del microservicio de compromisos (puerto 8081).
 * Mapeados 1:1 con `CompromisoResponse`, `CompromisoRegistroRequest`,
 * `CompromisoActualizacionRequest`, `CompromisoActualizacionPagoRequest`,
 * `CompromisoActualizacionVencimientoRequest`, `AnulacionRequest` y
 * `AnulacionResponse` de PagoFlexMS.
 */
data class ApiCompromisoResponse(
    val idCompromiso: Int? = null,
    val folio: String? = null,
    val idUsuarioFinal: Int? = null,
    val idEmpresa: Int? = null,
    val concepto: String? = null,
    val numeroCuota: Int? = null,
    val totalCuotas: Int? = null,
    val monto: Int? = null,
    val fechaVencimiento: String? = null,
    val estado: String? = null,
    val recargoAplicado: Int? = null,
    val fechaCambioEstado: String? = null,
    val idEjecutivoRegistro: Int? = null,
    val fechaRegistro: String? = null,
    val sistemaOrigen: String? = null,
    val idInstanciaOrigen: String? = null,
    val idCompromisoOrigen: Long? = null
)

data class ApiCompromisoRegistroRequest(
    val idUsuarioFinal: Int,
    val idEmpresa: Int,
    val concepto: String,
    val numeroCuota: Int? = null,
    val totalCuotas: Int? = null,
    val monto: Int,
    val fechaVencimiento: String,
    val idEjecutivoRegistro: Int? = null,
    val sistemaOrigen: String? = null,
    val idInstanciaOrigen: String? = null,
    val idCompromisoOrigen: Long? = null
)

data class ApiCompromisoActualizacionRequest(
    val concepto: String,
    val numeroCuota: Int? = null,
    val totalCuotas: Int? = null,
    val monto: Int,
    val fechaVencimiento: String
)

data class ApiCompromisoPagoRequest(
    val estado: String,
    val numeroCuota: Int? = null,
    val fechaVencimiento: String,
    val recargoAplicado: Int
)

data class ApiCompromisoVencimientoRequest(
    val estado: String,
    val recargoAplicado: Int
)

data class ApiAnulacionRequest(
    val idEjecutivo: Int,
    val motivo: String
)

data class ApiAnulacionResponse(
    val idCompromiso: Int? = null,
    val idEjecutivo: Int? = null,
    val motivo: String? = null,
    val comentario: String? = null,
    val fecha: String? = null,
    val estadoCompromiso: String? = null
)

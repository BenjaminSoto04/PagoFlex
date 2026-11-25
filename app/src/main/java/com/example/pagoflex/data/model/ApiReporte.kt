package com.example.pagoflex.data.model

/**
 * DTOs del microservicio de reportes (puerto 8086).
 * Mapeados 1:1 con `ReporteResponse`, `ReporteMensajeResponse`,
 * `ReporteRegistroRequest`, `ReporteEstadoRequest` y `ReporteMensajeRequest`
 * de PagoFlexMS.
 */
data class ApiReporteResponse(
    val id: Long? = null,
    val tipo: String? = null,
    val compromiso: String? = null,
    val descripcion: String? = null,
    val fecha: String? = null,
    val idUsuario: Long? = null,
    val estado: String? = null
)

data class ApiReporteRegistroRequest(
    val tipo: String,
    val compromiso: String,
    val descripcion: String,
    val fecha: String,
    val idUsuario: Long
)

data class ApiReporteEstadoRequest(
    val estado: String
)

data class ApiReporteMensajeResponse(
    val id: Long? = null,
    val idReporte: Long? = null,
    val idRemitente: Long? = null,
    val rolRemitente: String? = null,
    val contenido: String? = null,
    val fechaHora: String? = null
)

data class ApiReporteMensajeRequest(
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)

package com.example.pagoflex.data.local.entity


/**
 * Mensajes del hilo de un reporte. La descripcion original del reporte se
 * guarda como primer mensaje del remitente USUARIO.
 */
data class ReporteMensajeEntity(
    val id: Long = 0,
    val idReporte: Long,
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)

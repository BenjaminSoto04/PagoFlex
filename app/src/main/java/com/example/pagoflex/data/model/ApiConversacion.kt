package com.example.pagoflex.data.model

/**
 * DTOs del microservicio de agente / conversaciones (puerto 8082).
 * Mapeados 1:1 con `ConversacionResponse`, `MensajeResponse`,
 * `ConversacionRegistroRequest`, `ConversacionEstadoRequest` y
 * `MensajeRegistroRequest` de PagoFlexMS.
 */
data class ApiConversacionResponse(
    val id: Long? = null,
    val idUsuario: Long? = null,
    val fechaCreacion: String? = null,
    val estado: String? = null
)

data class ApiConversacionRegistroRequest(
    val idUsuario: Long,
    val fechaCreacion: String,
    val estado: String? = null
)

data class ApiConversacionEstadoRequest(
    val estado: String
)

data class ApiMensajeResponse(
    val id: Long? = null,
    val idConversacion: Long? = null,
    val idRemitente: Long? = null,
    val rolRemitente: String? = null,
    val contenido: String? = null,
    val fechaHora: String? = null
)

data class ApiMensajeRegistroRequest(
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)

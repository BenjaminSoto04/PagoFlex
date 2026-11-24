package com.example.pagoflex.data.remote

data class ConversacionDto(
    val id: Long = 0,
    val idUsuario: Long,
    val fechaCreacion: String,
    val estado: String = "PENDIENTE"
)

data class CrearConversacionRequest(
    val idUsuario: Long,
    val fechaCreacion: String,
    val estado: String? = null
)

data class CambiarEstadoConversacionRequest(
    val estado: String
)
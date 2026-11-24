package com.example.pagoflex.data.remote

data class MensajeDto(
    val id: Long = 0,
    val idConversacion: Long,
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)

data class CrearMensajeRequest(
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)
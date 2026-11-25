package com.example.pagoflex.data.local.entity


data class MensajeEntity(
    val id: Long = 0,
    val idConversacion: Long,
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)

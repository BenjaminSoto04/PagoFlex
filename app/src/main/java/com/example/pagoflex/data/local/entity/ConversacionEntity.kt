package com.example.pagoflex.data.local.entity


data class ConversacionEntity(
    val id: Long = 0,
    val idUsuario: Long,
    val fechaCreacion: String,
    val estado: String = "PENDIENTE"
)

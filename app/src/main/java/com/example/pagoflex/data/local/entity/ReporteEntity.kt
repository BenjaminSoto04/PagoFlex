package com.example.pagoflex.data.local.entity


data class ReporteEntity(
    val id: Long = 0,
    val tipo: String,
    val compromiso: String,
    val descripcion: String,
    val fecha: String,
    val idUsuario: Long,
    val estado: String = "PENDIENTE"
)

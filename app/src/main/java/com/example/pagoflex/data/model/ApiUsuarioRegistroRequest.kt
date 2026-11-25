package com.example.pagoflex.data.model

import java.util.UUID

/**
 * DTO para el registro de usuario en la API remota.
 */
data class ApiUsuarioRegistroRequest(
    val nombre: String,
    val rut: String,
    val email: String,
    val password: String,
    val telefono: String,
    val comuna: String,
    val empresa: String,
    val preguntaSeguridad: String,
    val respuestaSeguridad: String
)

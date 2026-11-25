package com.example.pagoflex.data.model

import java.util.UUID

/**
 * DTO para el registro de usuario en la API remota.
 */
data class ApiUsuarioRegistroRequest(
    val nombre: String,
    val rut: String? = null,
    val email: String,
    val password: String,
    val telefono: String? = null,
    val comuna: String? = null,
    val empresa: String? = null,
    val preguntaSeguridad: String? = null,
    val respuestaSeguridad: String? = null
)

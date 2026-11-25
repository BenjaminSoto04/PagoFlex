package com.example.pagoflex.data.model

/**
 * DTO para la petición de login a la API remota.
 */
data class ApiUsuarioLoginRequest(
    val email: String,
    val password: String
)

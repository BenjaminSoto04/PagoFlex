package com.example.pagoflex.data.model

/**
 * DTO para actualizar perfil de usuario en la API remota.
 */
data class ApiUsuarioPerfilRequest(
    val nombre: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val preguntaSeguridad: String? = null,
    val respuestaSeguridad: String? = null,
    val fotoUri: String? = null
)

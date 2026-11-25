package com.example.pagoflex.data.model

/**
 * DTO para cambiar rol de usuario en la API remota.
 */
data class ApiUsuarioRolRequest(
    val rol: String,
    val empresaId: Long? = null,
    val empresa: String? = null
)

package com.example.pagoflex.data.model

data class ApiUsuarioResetContrasenaRequest(
    val email: String,
    val nuevaPassword: String
)
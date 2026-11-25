package com.example.pagoflex.data.model

/**
 * DTO para registro/actualización de empresa desde la API backend.
 * Mapeado con EmpresasRegistroRequest del backend Spring.
 */
data class ApiEmpresaRegistroRequest(
    val codigo: String,
    val razonSocial: String,
    val nombreFantasia: String,
    val rut: String,
    val rubro: String,
    val email: String,
    val telefono: String,
    val recargo: Int = 0
)

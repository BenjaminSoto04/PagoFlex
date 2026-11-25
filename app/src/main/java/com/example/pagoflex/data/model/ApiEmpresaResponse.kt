package com.example.pagoflex.data.model

/**
 * DTO para respuesta de empresa desde la API backend.
 * Mapeado con EmpresasResponse del backend Spring.
 */
data class ApiEmpresaResponse(
    val id: Long,
    val codigo: String,
    val razonSocial: String,
    val nombreFantasia: String,
    val rut: String,
    val rubro: String,
    val email: String,
    val telefono: String,
    val recargo: Int = 0
)

object ApiEmpresaResponseEmpty {
    fun empty(): ApiEmpresaResponse = ApiEmpresaResponse(
        id = -1L,
        codigo = "",
        razonSocial = "",
        nombreFantasia = "",
        rut = "",
        rubro = "",
        email = "",
        telefono = "",
        recargo = 0
    )
}

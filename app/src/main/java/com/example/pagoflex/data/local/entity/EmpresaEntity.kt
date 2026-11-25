package com.example.pagoflex.data.local.entity


data class EmpresaEntity(
    val id: Long = 0,
    val codigo: String,
    val razonSocial: String,
    val nombreFantasia: String,
    val rut: String,
    val rubro: String,
    val email: String,
    val telefono: String,
    val recargo: Int
)

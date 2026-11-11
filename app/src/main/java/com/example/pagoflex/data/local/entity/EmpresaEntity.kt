package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "empresas")
data class EmpresaEntity(
    @PrimaryKey val id: Long,
    val codigo: String,
    val razonSocial: String,
    val nombreFantasia: String,
    val rut: String,
    val rubro: String,
    val email: String,
    val telefono: String,
    val recargo: Int
)
package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String? = null,
    val nombre: String,
    val rut: String? = null,
    val email: String,
    val passwordHash: String = "",
    val salt: String = "",
    val rol: String,
    val telefono: String? = null,
    val comuna: String? = null,
    val empresaId: Long? = null,
    val area: String? = null,
    val cargo: String? = null,
    val activo: Boolean = true,
    val fechaIncorporacion: String? = null,
    val avisosDias: Int? = 3,
    val avisosActivos: Boolean = true,
    val empresa: String? = null,
    val fotoUri: String? = null
)

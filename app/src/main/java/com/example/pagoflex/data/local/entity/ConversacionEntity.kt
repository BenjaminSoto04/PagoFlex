package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversaciones")
data class ConversacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idUsuario: Long,
    val fechaCreacion: String,
    val estado: String = "PENDIENTE"
)
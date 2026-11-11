package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comprobantes")
data class ComprobanteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idPago: Long,
    val uri: String,
    val fecha: String
)

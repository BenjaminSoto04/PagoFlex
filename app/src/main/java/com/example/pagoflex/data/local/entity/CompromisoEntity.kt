package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "compromisos")
data class CompromisoEntity(
    @PrimaryKey val id: Long,
    val folio: String,
    val idUsuario: Long,
    val idEmpresa: Long,
    val empresa: String,
    val concepto: String,
    val numeroCuota: Int?,
    val totalCuotas: Int?,
    val monto: Int,
    val fechaVencimiento: String,
    val estado: String,
    val recargoAplicado: Int = 0
)

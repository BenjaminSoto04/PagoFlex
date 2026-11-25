package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mensajes del hilo de un reporte. La descripcion original del reporte se
 * guarda como primer mensaje del remitente USUARIO.
 */
@Entity(tableName = "reporte_mensajes")
data class ReporteMensajeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val idReporte: Long,
    val idRemitente: Long,
    val rolRemitente: String,
    val contenido: String,
    val fechaHora: String
)

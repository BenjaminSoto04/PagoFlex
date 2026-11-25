package com.example.pagoflex.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pagoflex.data.repository.ReporteMensaje

/** Etiqueta visible del estado de un reporte. */
fun etiquetaEstadoReporte(estado: String): String = when (estado.uppercase()) {
    "PENDIENTE" -> "Pendiente"
    "EN_PROCESO" -> "En proceso"
    "FINALIZADO" -> "Finalizado"
    else -> estado.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
fun EstadoReporteBadge(estado: String) {
    val color = when (estado.uppercase()) {
        "FINALIZADO" -> MaterialTheme.colorScheme.error
        "EN_PROCESO" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = Modifier.widthIn(min = 96.dp),
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = etiquetaEstadoReporte(estado),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1
            )
        }
    }
}

/** Burbuja del hilo de un reporte. */
@Composable
fun BurbujaMensajeReporte(
    mensaje: ReporteMensaje,
    esMio: Boolean,
    modifier: Modifier = Modifier
) {
    val etiquetaRemitente = when {
        esMio -> "Tú"
        mensaje.rolRemitente == "AGENTE" -> "Agente"
        mensaje.rolRemitente == "SISTEMA" -> "Sistema"
        else -> "Usuario"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (esMio) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = etiquetaRemitente,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (esMio) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = mensaje.contenido,
                    color = if (esMio) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = mensaje.fechaHora,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (esMio) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    }
                )
            }
        }
    }
}

package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.components.EstadoReporteBadge
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.formatearFecha
import com.example.pagoflex.viewmodel.ReportesViewModel

@Composable
fun MisReportesScreen(
    onVolver: () -> Unit = {},
    onVerDetalle: (Long) -> Unit = {},
    onCrearNuevo: () -> Unit = {},
    vm: ReportesViewModel = viewModel()
) {
    val userId = SessionManager.userId(LocalContext.current)
    val reportes by vm.reportes.collectAsStateWithLifecycle()
    val misReportes = remember(reportes, userId) {
        reportes.filter { it.idUsuario == userId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onVolver) { Text("← Volver") }
            Text(
                text = "${misReportes.size} reporte(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Mis reportes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Revisa el estado y el hilo de respuestas de tus reportes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        BotonPrimario(text = "+ Crear nuevo reporte", onClick = onCrearNuevo)

        HorizontalDivider()

        if (misReportes.isEmpty()) {
            EstilosTarjeta.Tarjeta(centrada = false) {
                Text(
                    text = "Aún no tienes reportes registrados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(misReportes, key = { it.id }) { reporte ->
                    TarjetaReporteUsuario(
                        reporte = reporte,
                        onClick = { onVerDetalle(reporte.id) }
                    )
                }
            }
        }

        BotonSecundario(text = "Volver al inicio", onClick = onVolver)
    }
}

@Composable
private fun TarjetaReporteUsuario(
    reporte: Reporte,
    onClick: () -> Unit
) {
    EstilosTarjeta.Tarjeta(
        centrada = false,
        relleno = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reporte.tipo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            EstadoReporteBadge(estado = reporte.estado)
        }

        Text(
            text = reporte.descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3
        )
        Text(
            text = "Compromiso: ${reporte.compromiso}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Fecha: ${formatearFecha(reporte.fecha)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Abrir conversación →",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

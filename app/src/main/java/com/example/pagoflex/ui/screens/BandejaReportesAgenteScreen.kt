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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.ui.components.EstadoReporteBadge
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.BotonTexto
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.formatearFecha
import com.example.pagoflex.viewmodel.DashboardAgenteViewModel

@Composable
fun BandejaReportesAgenteScreen(
    onVolver: () -> Unit = {},
    onVerBandejaAtencion: () -> Unit = {},
    onVerDetalle: (Long) -> Unit = {},
    onVerPerfil: () -> Unit = {},
    dashboardViewModel: DashboardAgenteViewModel = viewModel()
) {
    val estado by dashboardViewModel.estado.collectAsStateWithLifecycle()

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bandeja de reportes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${estado.reportes.size} reportes creados por los usuarios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BotonTexto(text = "Mi perfil", onClick = onVerPerfil)
        }

        HorizontalDivider()

        if (estado.reportes.isEmpty()) {
            EstilosTarjeta.Tarjeta(centrada = false) {
                Text(
                    text = "No hay reportes registrados por ahora.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "Toca un reporte para abrir la conversación y responder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(estado.reportes, key = { it.id }) { reporte ->
                    TarjetaReporteAgente(
                        reporte = reporte,
                        nombreUsuario = estado.nombresUsuarios[reporte.idUsuario]
                            ?: "Usuario #${reporte.idUsuario}",
                        esAsignado = reporte.idUsuario in estado.usuariosAsignados,
                        onClick = { onVerDetalle(reporte.id) }
                    )
                }
            }
        }

        BotonPrimario(text = "Ir a bandeja de atención", onClick = onVerBandejaAtencion)
        BotonSecundario(text = "Volver al panel", onClick = onVolver)
    }
}

@Composable
private fun TarjetaReporteAgente(
    reporte: Reporte,
    nombreUsuario: String,
    esAsignado: Boolean,
    onClick: () -> Unit
) {
    EstilosTarjeta.Tarjeta(
        centrada = false,
        relleno = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Text(
            text = reporte.tipo,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EstadoReporteBadge(estado = reporte.estado)
            Surface(
                shape = RoundedCornerShape(50),
                color = if (esAsignado) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = if (esAsignado) "Asignado a ti" else "Otro usuario",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esAsignado) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Text(
            text = reporte.descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3
        )

        Text(
            text = "Usuario: $nombreUsuario",
            style = MaterialTheme.typography.bodySmall
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
            text = "Abrir reporte →",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

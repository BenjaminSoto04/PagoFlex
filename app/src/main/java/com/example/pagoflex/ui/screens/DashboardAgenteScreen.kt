package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.BotonTexto
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.viewmodel.DashboardAgenteViewModel

@Composable
fun DashboardAgenteScreen(
    onVerBandejaAtencion: () -> Unit = {},
    onVerBandejaReportes: () -> Unit = {},
    onVerGestionRoles: () -> Unit = {},
    onVerPerfil: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    dashboardViewModel: DashboardAgenteViewModel = viewModel()
) {
    val estado by dashboardViewModel.estado.collectAsStateWithLifecycle()
    val resumen = estado.resumen
    val usuariosRecientes = estado.usuarios.take(3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Panel del agente",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Resumen en tiempo real de tus bandejas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BotonTexto(text = "Mi perfil", onClick = onVerPerfil)
        }

        HorizontalDivider()

        EstilosTarjeta.Tarjeta(centrada = false) {
            Text(
                text = "Bandeja de atención",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${resumen.solicitudesActivas} conversaciones activas",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${resumen.solicitudesPendientes} pendientes de respuesta",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${resumen.usuariosAsignados} usuarios asignados a tu atención",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            BotonPrimario(
                text = "Abrir atención",
                onClick = onVerBandejaAtencion
            )
        }

        EstilosTarjeta.Tarjeta(centrada = false) {
            Text(
                text = "Bandeja de reportes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${resumen.reportesTotales} reportes recibidos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${resumen.reportesAsignados} creados por tus usuarios asignados",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Los contadores se actualizan automáticamente al recibir nuevos reportes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            BotonPrimario(
                text = "Abrir reportes",
                onClick = onVerBandejaReportes
            )
        }

        EstilosTarjeta.Tarjeta(centrada = false) {
            Text(
                text = "Administración de usuarios",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${resumen.usuariosRegistrados} usuarios registrados",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (usuariosRecientes.isEmpty()) {
                    "Aún no hay usuarios registrados."
                } else {
                    "Últimos registros: " + usuariosRecientes.joinToString { "${it.nombre} (${it.rol})" }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            BotonPrimario(
                text = "Gestionar usuarios",
                onClick = onVerGestionRoles
            )
        }

        Spacer(Modifier.height(4.dp))
        BotonSecundario(text = "Cerrar sesión", onClick = onCerrarSesion)
    }
}

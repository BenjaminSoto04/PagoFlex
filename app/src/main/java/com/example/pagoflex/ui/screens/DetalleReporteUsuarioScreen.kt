package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.repository.ReporteMensaje
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.components.BurbujaMensajeReporte
import com.example.pagoflex.ui.components.EstadoReporteBadge
import com.example.pagoflex.ui.styles.BotonCompacto
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.formatearFecha
import com.example.pagoflex.viewmodel.ReportesViewModel

@Composable
fun DetalleReporteUsuarioScreen(
    reporteId: Long,
    onVolver: () -> Unit = {},
    onCrearNuevo: () -> Unit = {},
    vm: ReportesViewModel = viewModel()
) {
    val userId = SessionManager.userId(LocalContext.current)
    val reporte by vm.reporteActual.collectAsStateWithLifecycle()
    val mensajes by vm.mensajes.collectAsStateWithLifecycle()

    var texto by remember { mutableStateOf("") }
    var errorEnvio by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reporteId) { vm.abrirReporte(reporteId) }

    val actual = reporte
    val estaFinalizado = actual?.estado.equals("FINALIZADO", ignoreCase = true)
    val hilo = remember(mensajes, actual) {
        if (mensajes.isEmpty() && actual != null) {
            listOf(
                ReporteMensaje(
                    id = 0,
                    idReporte = actual.id,
                    idRemitente = actual.idUsuario,
                    rolRemitente = "USUARIO",
                    contenido = actual.descripcion,
                    fechaHora = actual.fecha
                )
            )
        } else {
            mensajes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver a mis reportes") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = actual?.tipo ?: "Reporte",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (actual != null) {
                    Text(
                        text = "Compromiso: ${actual.compromiso}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Fecha: ${formatearFecha(actual.fecha)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            EstadoReporteBadge(estado = actual?.estado ?: "PENDIENTE")
        }

        HorizontalDivider()

        if (actual == null) {
            EstilosTarjeta.Tarjeta(centrada = false) {
                Text("Cargando reporte...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(hilo, key = { it.id }) { mensaje ->
                    BurbujaMensajeReporte(
                        mensaje = mensaje,
                        esMio = mensaje.rolRemitente == "USUARIO"
                    )
                }
            }

            HorizontalDivider()

            if (estaFinalizado) {
                EstilosTarjeta.Tarjeta(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    relleno = PaddingValues(12.dp)
                ) {
                    Text(
                        text = "Este reporte está FINALIZADO. El historial completo permanece disponible para lectura, pero no admite nuevos mensajes.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                BotonPrimario(
                    text = "Crear nuevo reporte",
                    onClick = onCrearNuevo
                )
            } else {
                Text(
                    text = "Conversación con el agente",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        EstiloCampo.CampoTexto(
                            value = texto,
                            onValueChange = {
                                texto = it
                                errorEnvio = null
                            },
                            label = "Mensaje",
                            placeholder = { Text("Escribe una respuesta para el agente...") },
                            singleLine = false,
                            minLines = 1,
                            maxLines = 3,
                            isError = errorEnvio != null
                        )
                        EstiloCampo.MensajeError(errorEnvio)
                    }
                    Spacer(Modifier.width(8.dp))
                    BotonCompacto(
                        text = "Enviar",
                        onClick = {
                            when {
                                userId <= 0 -> errorEnvio = "No pudimos identificar tu usuario."
                                texto.isBlank() -> errorEnvio = "Escribe un mensaje antes de enviar"
                                else -> {
                                    vm.enviarMensaje(userId, "USUARIO", texto)
                                    texto = ""
                                    errorEnvio = null
                                }
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

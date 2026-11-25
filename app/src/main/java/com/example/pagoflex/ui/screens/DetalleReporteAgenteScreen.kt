package com.example.pagoflex.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
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
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.formatearFecha
import com.example.pagoflex.viewmodel.ReportesViewModel

private val estadosReporte = listOf(
    "PENDIENTE" to "Pendiente",
    "EN_PROCESO" to "En proceso",
    "FINALIZADO" to "Finalizado"
)

@Composable
fun DetalleReporteAgenteScreen(
    reporteId: Long,
    onVolver: () -> Unit = {},
    vm: ReportesViewModel = viewModel()
) {
    val agenteId = SessionManager.userId(LocalContext.current)
    val reporte by vm.reporteActual.collectAsStateWithLifecycle()
    val mensajes by vm.mensajes.collectAsStateWithLifecycle()
    val nombresUsuarios by vm.nombresUsuarios.collectAsStateWithLifecycle()

    var texto by remember { mutableStateOf("") }
    var errorEnvio by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reporteId) { vm.abrirReporte(reporteId) }

    val actual = reporte
    val estaFinalizado = actual?.estado.equals("FINALIZADO", ignoreCase = true)
    val nombreUsuario = actual?.let {
        nombresUsuarios[it.idUsuario] ?: "Usuario #${it.idUsuario}"
    } ?: "Usuario"

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
        TextButton(onClick = onVolver) { Text("← Volver a reportes") }

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
                Text(
                    text = "Usuario: $nombreUsuario",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (actual != null) {
                    Text(
                        text = "Compromiso: ${actual.compromiso} · ${formatearFecha(actual.fecha)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            EstadoReporteBadge(estado = actual?.estado ?: "PENDIENTE")
        }

        EstilosTarjeta.Tarjeta(
            centrada = false,
            color = MaterialTheme.colorScheme.surfaceVariant,
            relleno = PaddingValues(12.dp)
        ) {
            Text(
                text = "Estado del reporte",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                estadosReporte.forEach { (valor, etiqueta) ->
                    FilterChip(
                        selected = actual?.estado.equals(valor, ignoreCase = true),
                        onClick = {
                            if (actual != null) vm.cambiarEstado(actual.id, valor)
                        },
                        label = { Text(etiqueta) }
                    )
                }
            }
            Text(
                text = "Al cambiar a Finalizado se cierra el hilo para el usuario, conservando el historial.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hilo, key = { it.id }) { mensaje ->
                BurbujaMensajeReporte(
                    mensaje = mensaje,
                    esMio = mensaje.rolRemitente == "AGENTE"
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
                    text = "Reporte FINALIZADO. El historial queda visible para el usuario y no admite más respuestas.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
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
                        label = "Respuesta al usuario",
                        placeholder = { Text("Escribe tu respuesta...") },
                        singleLine = false,
                        minLines = 1,
                        maxLines = 3,
                        isError = errorEnvio != null
                    )
                    EstiloCampo.MensajeError(errorEnvio)
                }
                Spacer(Modifier.width(8.dp))
                BotonCompacto(
                    text = "Responder",
                    onClick = {
                        when {
                            agenteId <= 0 -> errorEnvio = "No pudimos identificar al agente."
                            texto.isBlank() -> errorEnvio = "Escribe una respuesta antes de enviar"
                            else -> {
                                vm.enviarMensaje(agenteId, "AGENTE", texto)
                                texto = ""
                                errorEnvio = null
                            }
                        }
                    }
                )
            }
        }
    }
}

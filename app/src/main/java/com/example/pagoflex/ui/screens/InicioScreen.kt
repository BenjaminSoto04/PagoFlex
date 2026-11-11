package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.ui.components.*
import com.example.pagoflex.ui.theme.*
import com.example.pagoflex.viewmodel.PagosViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InicioScreen(
    pagosViewModel: PagosViewModel,
    nombreUsuario: String? = null,
    onVerCompromisos: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerPerfil: () -> Unit = {},
    onVerAgente: () -> Unit = {},
    onVerReportarProblema: () -> Unit = {},
    onVerAsistente: () -> Unit = {},
    onVerPanelEjecutivo: () -> Unit = {},
    onVerGestionCompromisos: () -> Unit = {},
    onVerReportes: () -> Unit = {}
) {
    val compromisos by pagosViewModel.compromisos.collectAsStateWithLifecycle()
    val currentUserId = SessionManager.userId(LocalContext.current)
    val misCompromisos = compromisos.filter { it.idUsuario == currentUserId }
    val activos = misCompromisos.filter { it.estado == EstadoCompromiso.PENDIENTE || it.estado == EstadoCompromiso.VENCIDO }
    val totalPendiente = activos.sumOf { it.monto + it.recargoAplicado }
    val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-uuuu")
    val mesActual = YearMonth.now()
    val compromisosDelMes = activos.filter { YearMonth.from(LocalDate.parse(it.fechaVencimiento, formatoFecha)) == mesActual }
    val totalDelMes = compromisosDelMes.sumOf { it.monto + it.recargoAplicado }
    val cantidadVencidos = activos.count { it.estado == EstadoCompromiso.VENCIDO }
    val proximoCompromiso = activos.filter { it.estado == EstadoCompromiso.PENDIENTE }.minByOrNull { LocalDate.parse(it.fechaVencimiento, formatoFecha) }
    val formatoPesos = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CL"))

    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Hola, ${nombreUsuario ?: "Usuario"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                Text("Resumen de tus compromisos", style = MaterialTheme.typography.bodyMedium, color = GrisDeshabilitado)
            }
        }
        PagoFlexCard {
            Text("Total pendiente", style = MaterialTheme.typography.titleMedium)
            Text("$ ${formatoPesos.format(totalPendiente)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Primario)
            Text("${activos.size} compromisos activos", style = MaterialTheme.typography.bodyMedium)
        }
        PagoFlexCard {
            Text("Proximo vencimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(proximoCompromiso?.fechaVencimiento ?: "Sin vencimientos proximos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            proximoCompromiso?.let { Text(it.empresa, style = MaterialTheme.typography.bodyMedium) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PagoFlexCard(Modifier.weight(1f)) {
                Text("Este mes", style = MaterialTheme.typography.titleSmall)
                Text("$ ${formatoPesos.format(totalDelMes)}", fontWeight = FontWeight.Bold)
                Text("${compromisosDelMes.size} compromisos", style = MaterialTheme.typography.bodySmall)
            }
            PagoFlexCard(Modifier.weight(1f)) {
                Text("Vencidos", style = MaterialTheme.typography.titleSmall)
                Text(cantidadVencidos.toString(), fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                Text(if (cantidadVencidos == 0) "Al dia" else "Revisar", style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionHeader("Accesos rapidos")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PagoFlexPrimaryButton("Pagar", onVerCompromisos, Modifier.weight(1f))
            PagoFlexSecondaryButton("Compromisos", onVerCompromisos, Modifier.weight(1f))
        }
        PagoFlexSecondaryButton("Reportar problema", onVerReportarProblema)
        PagoFlexSecondaryButton("Asistente virtual", onVerAsistente)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PagoFlexSecondaryButton("Historial", onVerHistorial, Modifier.weight(1f))
            PagoFlexSecondaryButton("Mi perfil", onVerPerfil, Modifier.weight(1f))
        }
        if (activos.isEmpty()) EmptyState("No tienes compromisos pendientes")
    }
}

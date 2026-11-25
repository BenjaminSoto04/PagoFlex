package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.model.IndicadorViewModel
import com.example.pagoflex.ui.components.*
import com.example.pagoflex.ui.styles.formatearCLP
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
    indicadorViewModel: IndicadorViewModel = remember { 
        IndicadorViewModel(com.example.pagoflex.data.remote.RetrofitClient.obtenerIndicadores()) 
    },
    nombreUsuario: String? = null,
    onVerCompromisos: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerPerfil: () -> Unit = {},
    onVerAgente: () -> Unit = {},
    onVerReportarProblema: () -> Unit = {},
    onVerMisReportes: () -> Unit = {},
    onVerAsistente: () -> Unit = {},
    onVerPanelEjecutivo: () -> Unit = {},
    onVerGestionCompromisos: () -> Unit = {},
    onVerReportes: () -> Unit = {}
) {
    val compromisos by pagosViewModel.compromisos.collectAsStateWithLifecycle()
    val indicadorState by indicadorViewModel.state.collectAsStateWithLifecycle()
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

    // Función local para mostrar fila de indicador
    @Composable
    fun IndicadorRow(label: String, value: String) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF37474F))
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Hola, ${nombreUsuario ?: "Usuario"}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextoOscuro)
                Text("Resumen de tus compromisos y del mercado", style = MaterialTheme.typography.bodyMedium, color = GrisDeshabilitado)
            }
        }
        
        // Tarjeta de indicadores económicos
        if (indicadorState.mostrarIndicadores) {
            PagoFlexCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Indicadores Económicos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextoOscuro)
                        if (indicadorState.loading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val indicadores = indicadorState.indicador
                    if (indicadores == null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                if (indicadorState.loading) "Cargando indicadores..." else "Sin datos disponibles",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        // UF y Dólar siempre en pesos chilenos: $ 39.843 CLP
                        IndicadorRow("UF", formatearCLP(indicadores.uf))
                        indicadores.uf12?.let { IndicadorRow("UF - 12 meses", formatearCLP(it)) }
                        IndicadorRow("Dólar USA", formatearCLP(indicadores.dolar_america))
                        indicadores.dolar_canada?.let { IndicadorRow("Dólar Canadá", formatearCLP(it)) }
                        indicadores.moneda_colombia?.let { IndicadorRow("Dólar Colombia", formatearCLP(it, decimales = 2)) }

                        Spacer(Modifier.height(4.dp))
                        Text("Valores en pesos chilenos (CLP)", style = MaterialTheme.typography.bodySmall, color = GrisDeshabilitado)

                        if (indicadorState.usandoRespaldo) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "⚠️ ${indicadorState.error ?: "mindicador.cl no está disponible."}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE53935)
                            )
                            Spacer(Modifier.height(4.dp))
                            PagoFlexSecondaryButton("Reintentar", indicadorViewModel::refrescar)
                        }
                    }
                }
            }
        } else {
            PagoFlexCard(Modifier.fillMaxWidth()) {
                Text("⚠️ Sin conexión a internet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("No se pudieron cargar los indicadores del mercado.", style = MaterialTheme.typography.bodySmall, color = GrisDeshabilitado)
            }
        }
        
        PagoFlexCard {
            Text("Total pendiente", style = MaterialTheme.typography.titleMedium)
            Text("$ ${formatoPesos.format(totalPendiente)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Primario)
            Text("${activos.size} compromisos activos", style = MaterialTheme.typography.bodyMedium)
        }
        PagoFlexCard {
            Text("Próximo vencimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(proximoCompromiso?.fechaVencimiento ?: "Sin vencimientos próximos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                Text(if (cantidadVencidos == 0) "Al día" else "Revisar", style = MaterialTheme.typography.bodySmall)
            }
        }
        SectionHeader("Accesos rápidos")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PagoFlexPrimaryButton("Pagar", onVerCompromisos, Modifier.weight(1f))
            PagoFlexSecondaryButton("Compromisos", onVerCompromisos, Modifier.weight(1f))
        }
        PagoFlexSecondaryButton("Reportar problema", onVerReportarProblema)
        PagoFlexSecondaryButton("Mis reportes", onVerMisReportes)
        PagoFlexSecondaryButton("Asistente virtual", onVerAsistente)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PagoFlexSecondaryButton("Historial", onVerHistorial, Modifier.weight(1f))
            PagoFlexSecondaryButton("Mi perfil", onVerPerfil, Modifier.weight(1f))
        }
        if (activos.isEmpty()) EmptyState("No tienes compromisos pendientes")
    }
}

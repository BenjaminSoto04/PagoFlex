package com.example.pagoflex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.model.Pago
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.formatearFecha
import com.example.pagoflex.ui.styles.formatearPesos
import com.example.pagoflex.ui.styles.parsearFecha
import com.example.pagoflex.viewmodel.EjecutivoViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val periodos = listOf("Semanal", "Mensual", "Anual")

private data class ResumenPeriodo(
    val recaudado: Long,
    val pendiente: Long,
    val vencido: Long,
    val pagados: Int,
    val pendientes: Int,
    val vencidos: Int,
    val desde: LocalDate,
    val hasta: LocalDate
)

private fun calcularResumen(
    periodo: String,
    compromisos: List<Compromiso>,
    pagos: List<Pago>
): ResumenPeriodo {
    val hoy = LocalDate.now()
    val desde = when (periodo) {
        "Semanal" -> hoy.minusDays(6)
        "Mensual" -> hoy.minusDays(29)
        else -> hoy.minusDays(364)
    }

    val pagosPeriodo = pagos.filter { pago ->
        val fecha = parsearFecha(pago.fechaHora)
        fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hoy)
    }

    val pendientes = compromisos.filter { it.estado == EstadoCompromiso.PENDIENTE }
    val vencidos = compromisos.filter { it.estado == EstadoCompromiso.VENCIDO }

    return ResumenPeriodo(
        recaudado = pagosPeriodo.sumOf { it.montoPagado.toLong() },
        pendiente = pendientes.sumOf { (it.monto + it.recargoAplicado).toLong() },
        vencido = vencidos.sumOf { (it.monto + it.recargoAplicado).toLong() },
        pagados = pagosPeriodo.size,
        pendientes = pendientes.size,
        vencidos = vencidos.size,
        desde = desde,
        hasta = hoy
    )
}

@Composable
fun ResumenRecaudacionReportesScreen(
    user: UserEntity? = null,
    onVolver: () -> Unit = {},
    ejecutivoViewModel: EjecutivoViewModel = viewModel()
) {
    val compromisosTodos by ejecutivoViewModel.compromisos.collectAsStateWithLifecycle()
    val pagosTodos by ejecutivoViewModel.pagos.collectAsStateWithLifecycle()
    val reportesTodos by ejecutivoViewModel.reportes.collectAsStateWithLifecycle()
    val usuarioActual by ejecutivoViewModel.usuarioActual.collectAsStateWithLifecycle()

    val ejecutivo = user ?: usuarioActual
    val empresaId = ejecutivo?.empresaId

    val compromisos = remember(compromisosTodos, empresaId) {
        if (empresaId == null) emptyList()
        else compromisosTodos.filter { it.idEmpresa == empresaId }
    }

    val pagos = remember(pagosTodos, compromisos) {
        val idsCompromisos = compromisos.map { it.id }.toSet()
        pagosTodos.filter { it.idCompromiso in idsCompromisos }
    }

    val reportes = remember(reportesTodos, compromisos) {
        val folios = compromisos.map { it.folio }.toSet()
        reportesTodos.filter { it.compromiso in folios }
    }

    val nombreEmpresa = ejecutivo?.empresa?.takeIf { it.isNotBlank() }
        ?: compromisos.firstOrNull()?.empresa

    var periodoSeleccionado by remember {
        mutableStateOf("Mensual")
    }

    var mensajeReporte by remember {
        mutableStateOf("")
    }

    val resumen = calcularResumen(periodoSeleccionado, compromisos, pagos)

    val totalGestionado = resumen.recaudado + resumen.pendiente + resumen.vencido

    val porcentajeRecaudado =
        if (totalGestionado > 0L) {
            resumen.recaudado.toFloat() / totalGestionado.toFloat()
        } else {
            0f
        }

    val formatoDiaMes = DateTimeFormatter.ofPattern("dd/MM")
    val rangoPeriodo = "${resumen.desde.format(formatoDiaMes)} al ${resumen.hasta.format(formatoDiaMes)}"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingInterno ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterno)
        ) {

            // Encabezado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "PF",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Reportes internos",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = nombreEmpresa ?: "Resumen de recaudación",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }

                TextButton(onClick = onVolver) {
                    Text("Volver", color = Color.White)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                item {
                    Text(
                        text = "Indicadores financieros",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Consulta el estado de la recaudación por periodo.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    if (empresaId == null) {
                        Text(
                            text = "Tu usuario no tiene una empresa asignada, por lo que no hay datos para resumir.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Selector de periodo
                item {
                    Text(
                        text = "Periodo del reporte",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        periodos.forEach { periodo ->
                            FilterChip(
                                selected = periodoSeleccionado == periodo,
                                onClick = {
                                    periodoSeleccionado = periodo
                                    mensajeReporte = ""
                                },
                                label = { Text(periodo) }
                            )
                        }
                    }

                    Text(
                        text = "Recaudación considerada: $rangoPeriodo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Total recaudado
                item {
                    TarjetaIndicadorRecaudacion(
                        titulo = "Total recaudado",
                        monto = formatearPesos(resumen.recaudado),
                        descripcion = if (resumen.pagados == 1) {
                            "1 pago registrado en el periodo"
                        } else {
                            "${resumen.pagados} pagos registrados en el periodo"
                        },
                        colorIndicador = Color(0xFF2E7D32)
                    )
                }

                // Montos pendientes y vencidos
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TarjetaMontoPequena(
                            titulo = "Pendiente",
                            monto = formatearPesos(resumen.pendiente),
                            colorIndicador = Color(0xFFB26A00),
                            modifier = Modifier.weight(1f)
                        )

                        TarjetaMontoPequena(
                            titulo = "Vencido",
                            monto = formatearPesos(resumen.vencido),
                            colorIndicador = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Saldos vigentes a la fecha, de los compromisos de la empresa.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Porcentaje de recaudacion
                item {
                    EstilosTarjeta.Tarjeta(
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Cumplimiento de recaudación",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            text = "${(porcentajeRecaudado * 100).toInt()}%",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        LinearProgressIndicator(
                            progress = { porcentajeRecaudado },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Text(
                            text = "Monto gestionado: ${formatearPesos(totalGestionado)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }

                // Resumen de estados
                item {
                    Text(
                        text = "Estado de los compromisos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    FilaEstadoReporte(
                        nombre = "Pagados",
                        cantidad = resumen.pagados,
                        colorIndicador = Color(0xFF2E7D32)
                    )

                    FilaEstadoReporte(
                        nombre = "Pendientes",
                        cantidad = resumen.pendientes,
                        colorIndicador = Color(0xFFB26A00)
                    )

                    FilaEstadoReporte(
                        nombre = "Vencidos",
                        cantidad = resumen.vencidos,
                        colorIndicador = Color(0xFFC62828)
                    )
                }

                // Reportes internos
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Reportes de usuarios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Reportes enviados por los usuarios de la empresa.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                if (reportes.isEmpty()) {
                    item {
                        Text(
                            text = "No hay reportes registrados para esta empresa.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    items(reportes, key = { it.id }) { reporte ->
                        TarjetaReporteInterno(reporte)
                    }
                }

                // Resumen del periodo
                item {
                    BotonPrimario(
                        text = "Generar resumen",
                        onClick = {
                            mensajeReporte = "Resumen ${periodoSeleccionado.lowercase()} " +
                                    "($rangoPeriodo): recaudado ${formatearPesos(resumen.recaudado)}, " +
                                    "pendiente ${formatearPesos(resumen.pendiente)}, " +
                                    "vencido ${formatearPesos(resumen.vencido)}."
                        }
                    )

                    if (mensajeReporte.isNotEmpty()) {
                        Text(
                            text = mensajeReporte,
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Información obtenida de la base de datos local de PagoFlex.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaIndicadorRecaudacion(
    titulo: String,
    monto: String,
    descripcion: String,
    colorIndicador: Color
) {
    EstilosTarjeta.Tarjeta {
        Text(
            text = titulo,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Text(
            text = monto,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = colorIndicador
        )

        Text(
            text = descripcion,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TarjetaMontoPequena(
    titulo: String,
    monto: String,
    colorIndicador: Color,
    modifier: Modifier = Modifier
) {
    EstilosTarjeta.Tarjeta(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        relleno = PaddingValues(12.dp)
    ) {
        Text(
            text = titulo,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = monto,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorIndicador
        )
    }
}

@Composable
private fun FilaEstadoReporte(
    nombre: String,
    cantidad: Int,
    colorIndicador: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = colorIndicador
        ) {}

        Text(
            text = nombre,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            fontSize = 14.sp
        )

        Text(
            text = cantidad.toString(),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TarjetaReporteInterno(
    reporte: Reporte
) {
    EstilosTarjeta.Tarjeta(elevacion = 1.dp) {
        Text(
            text = reporte.tipo,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Text(
            text = reporte.descripcion,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = reporte.compromiso,
                    modifier = Modifier.padding(
                        horizontal = 8.dp,
                        vertical = 5.dp
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = formatearFecha(reporte.fecha),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

package com.example.pagoflex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private data class ResumenPeriodo(
    val nombre: String,
    val recaudado: Long,
    val pendiente: Long,
    val vencido: Long,
    val pagados: Int,
    val pendientes: Int,
    val vencidos: Int
)

private data class ReporteInterno(
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val tipo: String
)

@Composable
fun ResumenRecaudacionReportesScreen(
    onVolver: () -> Unit = {}
) {
    var periodoSeleccionado by remember {
        mutableStateOf("Mensual")
    }

    var mensajeReporte by remember {
        mutableStateOf("")
    }

    val resumenes = remember {
        mapOf(
            "Semanal" to ResumenPeriodo(
                nombre = "Semanal",
                recaudado = 480000L,
                pendiente = 210000L,
                vencido = 95000L,
                pagados = 8,
                pendientes = 4,
                vencidos = 2
            ),
            "Mensual" to ResumenPeriodo(
                nombre = "Mensual",
                recaudado = 2450000L,
                pendiente = 870000L,
                vencido = 320000L,
                pagados = 35,
                pendientes = 12,
                vencidos = 5
            ),
            "Anual" to ResumenPeriodo(
                nombre = "Anual",
                recaudado = 18500000L,
                pendiente = 6200000L,
                vencido = 1900000L,
                pagados = 260,
                pendientes = 78,
                vencidos = 24
            )
        )
    }

    val resumen = resumenes[periodoSeleccionado]!!

    val totalGestionado = resumen.recaudado +
            resumen.pendiente + resumen.vencido

    val porcentajeRecaudado =
        if (totalGestionado > 0L) {
            resumen.recaudado.toFloat() / totalGestionado.toFloat()
        } else {
            0f
        }

    val reportes = remember {
        listOf(
            ReporteInterno(
                titulo = "Reporte de recaudación",
                descripcion = "Resumen de pagos recibidos y montos recaudados.",
                fecha = "20/10/2026",
                tipo = "Financiero"
            ),
            ReporteInterno(
                titulo = "Compromisos pendientes",
                descripcion = "Seguimiento de compromisos aún no pagados.",
                fecha = "20/10/2026",
                tipo = "Seguimiento"
            ),
            ReporteInterno(
                titulo = "Compromisos vencidos",
                descripcion = "Listado de compromisos con estado vencido.",
                fecha = "20/10/2026",
                tipo = "Control"
            )
        )
    }

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
                        text = "Resumen de recaudación",
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
                contentPadding = androidx.compose.foundation.layout
                    .PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                item {
                    Text(
                        text = "Indicadores financieros",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Consulta el estado de la recaudación por período.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                // Selector de período
                item {
                    Text(
                        text = "Período del reporte",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Semanal", "Mensual", "Anual").forEach {
                                periodo ->
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
                }

                // Total recaudado
                item {
                    TarjetaIndicadorRecaudacion(
                        titulo = "Total recaudado",
                        monto = formatearMonto(resumen.recaudado),
                        descripcion = "${resumen.pagados} compromisos pagados",
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
                            monto = formatearMonto(resumen.pendiente),
                            colorIndicador = Color(0xFFB26A00),
                            modifier = Modifier.weight(1f)
                        )

                        TarjetaMontoPequena(
                            titulo = "Vencido",
                            monto = formatearMonto(resumen.vencido),
                            colorIndicador = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Porcentaje de recaudación
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                trackColor =
                                    MaterialTheme.colorScheme.surface
                            )

                            Text(
                                text = "Monto gestionado: ${
                                    formatearMonto(totalGestionado)
                                }",
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
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
                        text = "Reportes internos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Reportes disponibles para consulta.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                items(reportes) { reporte ->
                    TarjetaReporteInterno(reporte)
                }

                // Generar reporte de demostración
                item {
                    Button(
                        onClick = {
                            mensajeReporte =
                                "Reporte ${periodoSeleccionado.lowercase()} generado correctamente (demostración)."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generar reporte")
                    }

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
                        text = "Información demostrativa con datos ficticios.",
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
}

@Composable
private fun TarjetaMontoPequena(
    titulo: String,
    monto: String,
    colorIndicador: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            .padding(vertical = 7.dp),
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
    reporte: ReporteInterno
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = reporte.titulo,
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
                        text = reporte.tipo,
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = reporte.fecha,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatearMonto(monto: Long): String {
    return "$" + monto.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}

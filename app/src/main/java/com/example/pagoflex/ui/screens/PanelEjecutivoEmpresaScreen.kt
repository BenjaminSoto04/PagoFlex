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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

private data class CompromisoEjecutivo(
    val folio: String,
    val cliente: String,
    val monto: Int,
    val fecha: String,
    val estado: String
)

@Composable
fun PanelEjecutivoEmpresaScreen(
    onVolver: () -> Unit = {}
) {
    val compromisos = remember {
        listOf(
            CompromisoEjecutivo(
                "PF-1001", "Cliente 001", 150000,
                "22/10/2026", "Pendiente"
            ),
            CompromisoEjecutivo(
                "PF-1002", "Cliente 002", 85000,
                "18/10/2026", "Pagado"
            ),
            CompromisoEjecutivo(
                "PF-1003", "Cliente 003", 220000,
                "15/10/2026", "Vencido"
            ),
            CompromisoEjecutivo(
                "PF-1004", "Cliente 004", 120000,
                "25/10/2026", "Pendiente"
            ),
            CompromisoEjecutivo(
                "PF-1005", "Cliente 005", 95000,
                "19/10/2026", "Pagado"
            )
        )
    }

    var filtroSeleccionado by remember {
        mutableStateOf("Todos")
    }

    var actualizado by remember {
        mutableStateOf(false)
    }

    val compromisosFiltrados = compromisos.filter {
        filtroSeleccionado == "Todos" ||
                it.estado == filtroSeleccionado
    }

    val totalComprometido = compromisos.sumOf { it.monto }

    val totalRecaudado = compromisos
        .filter { it.estado == "Pagado" }
        .sumOf { it.monto }

    val pendientes = compromisos.count {
        it.estado == "Pendiente"
    }

    val vencidos = compromisos.count {
        it.estado == "Vencido"
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
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PF",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = "Panel ejecutivo",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Gestión de PagoFlex",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onVolver,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Volver")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Resumen general",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Estado de los compromisos de la empresa",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                // Tarjetas de indicadores
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IndicadorEjecutivo(
                            titulo = "Comprometido",
                            valor = formatearPesos(totalComprometido),
                            modifier = Modifier.weight(1f)
                        )

                        IndicadorEjecutivo(
                            titulo = "Recaudado",
                            valor = formatearPesos(totalRecaudado),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IndicadorEjecutivo(
                            titulo = "Pendientes",
                            valor = pendientes.toString(),
                            modifier = Modifier.weight(1f)
                        )

                        IndicadorEjecutivo(
                            titulo = "Vencidos",
                            valor = vencidos.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Button(
                        onClick = {
                            actualizado = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Actualizar información")
                    }

                    if (actualizado) {
                        Text(
                            text = "Resumen actualizado con los datos de prueba.",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Compromisos registrados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Filtros por estado
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Todos",
                            "Pendiente",
                            "Pagado",
                            "Vencido"
                        ).forEach { estado ->
                            FilterChip(
                                selected = filtroSeleccionado == estado,
                                onClick = {
                                    filtroSeleccionado = estado
                                },
                                label = {
                                    Text(
                                        text = estado,
                                        fontSize = 11.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // Lista de compromisos
                if (compromisosFiltrados.isEmpty()) {
                    item {
                        Text("No hay compromisos para este filtro.")
                    }
                } else {
                    items(
                        items = compromisosFiltrados,
                        key = { it.folio }
                    ) { compromiso ->
                        TarjetaCompromisoEjecutivo(compromiso)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vista demostrativa con datos ficticios.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicadorEjecutivo(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Text(
                text = valor,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TarjetaCompromisoEjecutivo(
    compromiso: CompromisoEjecutivo
) {
    val colorEstado = when (compromiso.estado) {
        "Pagado" -> Color(0xFF2E7D32)
        "Vencido" -> Color(0xFFC62828)
        else -> Color(0xFFB26A00)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = compromiso.cliente,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Folio: ${compromiso.folio}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorEstado.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = compromiso.estado,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        color = colorEstado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = formatearPesos(compromiso.monto),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Fecha de compromiso: ${compromiso.fecha}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatearPesos(monto: Int): String {
    return "$" + monto.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}

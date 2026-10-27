package com.example.pagoflex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CompromisoGestion(
    val folio: String,
    val cliente: String,
    val monto: Int,
    val fecha: String,
    val estado: String,
    val motivoAnulacion: String = ""
)

@Composable
fun GestionAnulacionCompromisosScreen(
    onVolver: () -> Unit = {}
) {
    val compromisos = remember {
        mutableStateListOf(
            CompromisoGestion(
                "PF-2001", "Cliente 001", 150000,
                "22/10/2026", "Pendiente"
            ),
            CompromisoGestion(
                "PF-2002", "Cliente 002", 85000,
                "18/10/2026", "Pagado"
            ),
            CompromisoGestion(
                "PF-2003", "Cliente 003", 220000,
                "15/10/2026", "Vencido"
            ),
            CompromisoGestion(
                "PF-2004", "Cliente 004", 120000,
                "25/10/2026", "Pendiente"
            ),
            CompromisoGestion(
                "PF-2005", "Cliente 005", 95000,
                "19/10/2026", "Anulado",
                "Solicitud de ejemplo"
            )
        )
    }

    var filtroSeleccionado by remember {
        mutableStateOf("Todos")
    }

    var compromisoSeleccionado by remember {
        mutableStateOf<CompromisoGestion?>(null)
    }

    var motivo by remember {
        mutableStateOf("")
    }

    var mostrarDialogo by remember {
        mutableStateOf(false)
    }

    var mensajeResultado by remember {
        mutableStateOf("")
    }

    val compromisosFiltrados = compromisos.filter {
        filtroSeleccionado == "Todos" ||
                it.estado == filtroSeleccionado
    }

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
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center
                    ) {
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
                        text = "Gestión de compromisos",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Consulta y anulación",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Text(
                        text = "Resumen de gestión",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Administra los compromisos registrados en PagoFlex.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                // Indicadores
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IndicadorGestion(
                            titulo = "Pendientes",
                            cantidad = pendientes,
                            colorIndicador = Color(0xFFB26A00),
                            modifier = Modifier.weight(1f)
                        )

                        IndicadorGestion(
                            titulo = "Vencidos",
                            cantidad = vencidos,
                            colorIndicador = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )

                        IndicadorGestion(
                            titulo = "Total",
                            cantidad = compromisos.size,
                            colorIndicador = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text(
                        text = "Filtrar compromisos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Todos", "Pendiente", "Pagado").forEach {
                                    estado ->
                                FilterChip(
                                    selected = filtroSeleccionado == estado,
                                    onClick = {
                                        filtroSeleccionado = estado
                                        mensajeResultado = ""
                                    },
                                    label = { Text(estado) }
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Vencido", "Anulado").forEach {
                                    estado ->
                                FilterChip(
                                    selected = filtroSeleccionado == estado,
                                    onClick = {
                                        filtroSeleccionado = estado
                                        mensajeResultado = ""
                                    },
                                    label = { Text(estado) }
                                )
                            }
                        }
                    }
                }

                if (mensajeResultado.isNotEmpty()) {
                    item {
                        Text(
                            text = mensajeResultado,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
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

                if (compromisosFiltrados.isEmpty()) {
                    item {
                        Text("No hay compromisos en este estado.")
                    }
                } else {
                    items(
                        items = compromisosFiltrados,
                        key = { it.folio }
                    ) { compromiso ->
                        TarjetaGestionCompromiso(
                            compromiso = compromiso,
                            onAnular = {
                                compromisoSeleccionado = compromiso
                                motivo = ""
                                mostrarDialogo = true
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Datos ficticios para pruebas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    // Diálogo para confirmar la anulación
    if (mostrarDialogo && compromisoSeleccionado != null) {
        val compromiso = compromisoSeleccionado!!

        AlertDialog(
            onDismissRequest = {
                mostrarDialogo = false
                compromisoSeleccionado = null
            },
            title = {
                Text("Anular compromiso")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Folio: ${compromiso.folio}\n" +
                                "Cliente: ${compromiso.cliente}\n" +
                                "Monto: ${formatearMontoGestion(compromiso.monto)}"
                    )

                    Text(
                        text = "Indica el motivo de la anulación:"
                    )

                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Escribe el motivo...")
                        },
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val indice = compromisos.indexOfFirst {
                            it.folio == compromiso.folio
                        }

                        if (indice >= 0) {
                            val actual = compromisos[indice]

                            if (
                                actual.estado == "Pendiente" ||
                                actual.estado == "Vencido"
                            ) {
                                compromisos[indice] = actual.copy(
                                    estado = "Anulado",
                                    motivoAnulacion = motivo.trim()
                                )

                                mensajeResultado =
                                    "El compromiso ${actual.folio} fue anulado."
                            } else {
                                mensajeResultado =
                                    "Este compromiso ya no se puede anular."
                            }
                        }

                        mostrarDialogo = false
                        compromisoSeleccionado = null
                    },
                    enabled = motivo.isNotBlank() &&
                            (
                                    compromiso.estado == "Pendiente" ||
                                            compromiso.estado == "Vencido"
                                    )
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogo = false
                        compromisoSeleccionado = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun IndicadorGestion(
    titulo: String,
    cantidad: Int,
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = cantidad.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorIndicador
            )
        }
    }
}

@Composable
private fun TarjetaGestionCompromiso(
    compromiso: CompromisoGestion,
    onAnular: () -> Unit
) {
    val colorEstado = when (compromiso.estado) {
        "Pagado" -> Color(0xFF2E7D32)
        "Vencido" -> Color(0xFFC62828)
        "Anulado" -> Color(0xFF757575)
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
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = compromiso.cliente,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Folio: ${compromiso.folio}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = colorEstado.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = compromiso.estado,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        ),
                        color = colorEstado,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = formatearMontoGestion(compromiso.monto),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Fecha: ${compromiso.fecha}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (compromiso.motivoAnulacion.isNotBlank()) {
                Text(
                    text = "Motivo: ${compromiso.motivoAnulacion}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (
                compromiso.estado == "Pendiente" ||
                compromiso.estado == "Vencido"
            ) {
                Button(
                    onClick = onAnular,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Anular compromiso")
                }
            }
        }
    }
}

private fun formatearMontoGestion(monto: Int): String {
    return "$" + monto.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}

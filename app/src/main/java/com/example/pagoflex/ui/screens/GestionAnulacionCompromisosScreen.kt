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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.Validaciones
import com.example.pagoflex.ui.styles.etiquetaEstado
import com.example.pagoflex.ui.styles.formatearFecha
import com.example.pagoflex.ui.styles.formatearPesos
import com.example.pagoflex.viewmodel.EjecutivoViewModel

private val filtrosGestion = listOf(
    "Todos",
    "Pendiente",
    "Pagado",
    "Vencido",
    "En revisión",
    "Anulado"
)

@Composable
fun GestionAnulacionCompromisosScreen(
    user: UserEntity? = null,
    onVolver: () -> Unit = {},
    ejecutivoViewModel: EjecutivoViewModel = viewModel()
) {
    val compromisosTodos by ejecutivoViewModel.compromisos.collectAsStateWithLifecycle()
    val usuarios by ejecutivoViewModel.usuarios.collectAsStateWithLifecycle()
    val usuarioActual by ejecutivoViewModel.usuarioActual.collectAsStateWithLifecycle()
    val mensaje by ejecutivoViewModel.success.collectAsStateWithLifecycle()
    val errorGeneral by ejecutivoViewModel.error.collectAsStateWithLifecycle()

    val empresaId = (user ?: usuarioActual)?.empresaId
    val nombresUsuarios = remember(usuarios) { usuarios.associate { it.id to it.nombre } }

    val compromisos = remember(compromisosTodos, empresaId) {
        if (empresaId == null) emptyList()
        else compromisosTodos.filter { it.idEmpresa == empresaId }
    }

    var filtroSeleccionado by remember { mutableStateOf("Todos") }

    var compromisoSeleccionado by remember {
        mutableStateOf<Compromiso?>(null)
    }

    var motivo by remember {
        mutableStateOf("")
    }

    var errorMotivo by remember {
        mutableStateOf<String?>(null)
    }

    var mostrarDialogo by remember {
        mutableStateOf(false)
    }

    val compromisosFiltrados = compromisos.filter {
        filtroSeleccionado == "Todos" || etiquetaEstado(it.estado) == filtroSeleccionado
    }

    val pendientes = compromisos.count { it.estado == EstadoCompromiso.PENDIENTE }
    val vencidos = compromisos.count { it.estado == EstadoCompromiso.VENCIDO }

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
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            filtrosGestion.take(3).forEach { estado ->
                                FilterChip(
                                    selected = filtroSeleccionado == estado,
                                    onClick = { filtroSeleccionado = estado },
                                    label = { Text(estado) }
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filtrosGestion.drop(3).forEach { estado ->
                                FilterChip(
                                    selected = filtroSeleccionado == estado,
                                    onClick = { filtroSeleccionado = estado },
                                    label = { Text(estado) }
                                )
                            }
                        }
                    }
                }

                mensaje?.let { texto ->
                    item {
                        Text(
                            text = texto,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                }

                errorGeneral?.let { texto ->
                    item {
                        Text(
                            text = texto,
                            color = MaterialTheme.colorScheme.error,
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

                if (compromisos.isEmpty()) {
                    item {
                        Text(
                            text = if (empresaId == null) {
                                "Tu usuario no tiene una empresa asignada, por lo que no hay compromisos para gestionar."
                            } else {
                                "No hay compromisos registrados para esta empresa."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (compromisosFiltrados.isEmpty()) {
                    item {
                        Text(
                            text = "No hay compromisos con el estado \"$filtroSeleccionado\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(
                        items = compromisosFiltrados,
                        key = { it.id }
                    ) { compromiso ->
                        TarjetaGestionCompromiso(
                            compromiso = compromiso,
                            nombreCliente = nombresUsuarios[compromiso.idUsuario]
                                ?: "Usuario #${compromiso.idUsuario}",
                            onAnular = {
                                compromisoSeleccionado = compromiso
                                motivo = ""
                                errorMotivo = null
                                mostrarDialogo = true
                            }
                        )
                    }
                }

                item {
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

    // Dialogo para confirmar la anulacion
    if (mostrarDialogo && compromisoSeleccionado != null) {
        val compromiso = compromisoSeleccionado!!

        AlertDialog(
            onDismissRequest = {
                mostrarDialogo = false
                compromisoSeleccionado = null
            },
            modifier = Modifier.imePadding(),
            title = {
                Text("Anular compromiso")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Folio: ${compromiso.folio}\n" +
                                "Cliente: ${nombresUsuarios[compromiso.idUsuario] ?: "Usuario #${compromiso.idUsuario}"}\n" +
                                "Monto: ${formatearPesos(compromiso.monto + compromiso.recargoAplicado)}"
                    )

                    Text(
                        text = "Indica el motivo de la anulación:"
                    )

                    EstiloCampo.CampoMultilinea(
                        value = motivo,
                        onValueChange = {
                            motivo = it
                            errorMotivo = null
                        },
                        label = "Motivo",
                        placeholder = { Text("Escribe el motivo...") },
                        minLines = 2,
                        maxLines = 4,
                        isError = errorMotivo != null
                    )
                    EstiloCampo.MensajeError(errorMotivo)
                }
            },
            confirmButton = {
                BotonPrimario(
                    text = "Confirmar",
                    anchoCompleto = false,
                    onClick = {
                        val error = Validaciones.motivoAnulacion(motivo)
                        errorMotivo = error
                        if (error != null) return@BotonPrimario

                        ejecutivoViewModel.limpiarMensajes()
                        ejecutivoViewModel.anularCompromiso(compromiso.id, motivo)

                        mostrarDialogo = false
                        compromisoSeleccionado = null
                    },
                    enabled = compromiso.estado == EstadoCompromiso.PENDIENTE ||
                            compromiso.estado == EstadoCompromiso.VENCIDO
                )
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
    EstilosTarjeta.Tarjeta(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        relleno = PaddingValues(12.dp),
        separacion = 4.dp
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

private fun colorEstadoGestion(estado: EstadoCompromiso): Color = when (estado) {
    EstadoCompromiso.PAGADO -> Color(0xFF2E7D32)
    EstadoCompromiso.VENCIDO -> Color(0xFFC62828)
    EstadoCompromiso.ANULADO -> Color(0xFF757575)
    else -> Color(0xFFB26A00)
}

@Composable
private fun TarjetaGestionCompromiso(
    compromiso: Compromiso,
    nombreCliente: String,
    onAnular: () -> Unit
) {
    val colorEstado = colorEstadoGestion(compromiso.estado)

    EstilosTarjeta.Tarjeta {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = nombreCliente,
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
                    text = etiquetaEstado(compromiso.estado),
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
            text = formatearPesos(compromiso.monto + compromiso.recargoAplicado),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = compromiso.concepto,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Vencimiento: ${formatearFecha(compromiso.fechaVencimiento)}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        compromiso.motivoAnulacion?.takeIf { it.isNotBlank() }?.let { texto ->
            Text(
                text = "Motivo de anulación: $texto",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (
            compromiso.estado == EstadoCompromiso.PENDIENTE ||
            compromiso.estado == EstadoCompromiso.VENCIDO
        ) {
            BotonPrimario(text = "Anular compromiso", onClick = onAnular)
        }
    }
}

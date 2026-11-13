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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.etiquetaEstado
import com.example.pagoflex.ui.styles.formatearFecha
import com.example.pagoflex.ui.styles.formatearPesos
import com.example.pagoflex.viewmodel.EjecutivoViewModel

private val filtrosPanel = listOf(
    "Todos",
    "Pendiente",
    "Pagado",
    "Vencido",
    "En revisión",
    "Anulado"
)

@Composable
fun PanelEjecutivoEmpresaScreen(
    user: UserEntity? = null,
    onVolver: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    onVerPerfil: () -> Unit = {},
    ejecutivoViewModel: EjecutivoViewModel = viewModel()
) {
    val compromisosTodos by ejecutivoViewModel.compromisos.collectAsStateWithLifecycle()
    val pagos by ejecutivoViewModel.pagos.collectAsStateWithLifecycle()
    val usuarios by ejecutivoViewModel.usuarios.collectAsStateWithLifecycle()
    val usuarioActual by ejecutivoViewModel.usuarioActual.collectAsStateWithLifecycle()
    val mensaje by ejecutivoViewModel.success.collectAsStateWithLifecycle()

    val ejecutivo = user ?: usuarioActual
    val empresaId = ejecutivo?.empresaId
    val nombresUsuarios = remember(usuarios) { usuarios.associate { it.id to it.nombre } }

    val compromisos = remember(compromisosTodos, empresaId) {
        if (empresaId == null) emptyList()
        else compromisosTodos.filter { it.idEmpresa == empresaId }
    }

    val pagosEmpresa = remember(pagos, compromisos) {
        val idsCompromisos = compromisos.map { it.id }.toSet()
        pagos.filter { it.idCompromiso in idsCompromisos }
    }

    var filtroSeleccionado by remember { mutableStateOf("Todos") }

    val compromisosFiltrados = compromisos.filter {
        filtroSeleccionado == "Todos" || etiquetaEstado(it.estado) == filtroSeleccionado
    }

    val totalComprometido = compromisos.sumOf { it.monto }
    val totalRecaudado = pagosEmpresa.sumOf { it.montoPagado }
    val pendientes = compromisos.count { it.estado == EstadoCompromiso.PENDIENTE }
    val vencidos = compromisos.count { it.estado == EstadoCompromiso.VENCIDO }

    val nombreEmpresa = ejecutivo?.empresa?.takeIf { it.isNotBlank() }
        ?: compromisos.firstOrNull()?.empresa

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
                    Box(contentAlignment = Alignment.Center) {
                        val foto = ejecutivo?.fotoUri
                        val file = if (foto != null) java.io.File(foto) else null
                        if (file != null && file.exists()) {
                            val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                            if (bmp != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("PF", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        } else {
                            Text("PF", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
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
                        text = nombreEmpresa ?: "Gestión de PagoFlex",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onVerPerfil,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Mi perfil")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            text = if (nombreEmpresa != null) {
                                "Compromisos registrados para $nombreEmpresa"
                            } else {
                                "Compromisos registrados en la base local"
                            },
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
                    BotonPrimario(
                        text = "Actualizar información",
                        onClick = { ejecutivoViewModel.refrescar() }
                    )

                    mensaje?.let {
                        Text(
                            text = it,
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            filtrosPanel.take(3).forEach { estado ->
                                FilterChip(
                                    selected = filtroSeleccionado == estado,
                                    onClick = { filtroSeleccionado = estado },
                                    label = { Text(text = estado, fontSize = 11.sp) }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            filtrosPanel.drop(3).forEach { estado ->
                                FilterChip(
                                    selected = filtroSeleccionado == estado,
                                    onClick = { filtroSeleccionado = estado },
                                    label = { Text(text = estado, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Lista de compromisos
                if (compromisos.isEmpty()) {
                    item {
                        Text(
                            text = if (empresaId == null) {
                                "Tu usuario no tiene una empresa asignada, por lo que no hay compromisos para mostrar."
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
                        TarjetaCompromisoEjecutivo(
                            compromiso = compromiso,
                            nombreCliente = nombresUsuarios[compromiso.idUsuario]
                                ?: "Usuario #${compromiso.idUsuario}"
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
}

private fun colorEstadoEjecutivo(estado: EstadoCompromiso): Color = when (estado) {
    EstadoCompromiso.PAGADO -> Color(0xFF2E7D32)
    EstadoCompromiso.VENCIDO -> Color(0xFFC62828)
    EstadoCompromiso.ANULADO -> Color(0xFF757575)
    else -> Color(0xFFB26A00)
}

@Composable
private fun IndicadorEjecutivo(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    EstilosTarjeta.Tarjeta(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        relleno = PaddingValues(12.dp)
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

@Composable
private fun TarjetaCompromisoEjecutivo(
    compromiso: Compromiso,
    nombreCliente: String
) {
    val colorEstado = colorEstadoEjecutivo(compromiso.estado)

    EstilosTarjeta.Tarjeta {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombreCliente,
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
                    text = etiquetaEstado(compromiso.estado),
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
    }
}

package com.example.pagoflex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pagoflex.data.DatosSimulados
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.viewmodel.PagosViewModel

import java.text.NumberFormat
import java.util.Locale

@Composable
fun CompromisosScreen(
    pagosViewModel: PagosViewModel,
    onVolver: () -> Unit,
    onVerDetalle: (Long) -> Unit
) {

    // Observar los compromisos
    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // Estado del filtro seleccionado
    var filtroSeleccionado by remember {
        mutableStateOf<EstadoCompromiso?>(null)
    }

    // Mostrar solamente los compromisos
    // correspondientes al usuario actual
    val compromisosUsuario = compromisos.filter {
        it.idUsuario == DatosSimulados.ID_USUARIO_ACTUAL
    }

    // Aplicar el filtro seleccionado
    val compromisosFiltrados = compromisosUsuario.filter {

        filtroSeleccionado == null ||
                it.estado == filtroSeleccionado
    }

    // Lista de filtros disponibles
    val filtros = listOf(
        null,
        EstadoCompromiso.PENDIENTE,
        EstadoCompromiso.VENCIDO,
        EstadoCompromiso.PAGADO,
        EstadoCompromiso.EN_REVISION,
        EstadoCompromiso.ANULADO
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Botón para regresar
        TextButton(
            onClick = onVolver
        ) {
            Text("← Volver")
        }

        // Título
        Text(
            text = "Mis compromisos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Consulta tus compromisos de pago",
            style = MaterialTheme.typography.bodyMedium
        )

        // Filtros horizontales
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),

            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            filtros.forEach { estado ->

                val nombreFiltro = when (estado) {
                    null -> "Todos"
                    EstadoCompromiso.PENDIENTE -> "Pendientes"
                    EstadoCompromiso.VENCIDO -> "Vencidos"
                    EstadoCompromiso.PAGADO -> "Pagados"
                    EstadoCompromiso.EN_REVISION -> "En revisión"
                    EstadoCompromiso.ANULADO -> "Anulados"
                }

                FilterChip(
                    selected = filtroSeleccionado == estado,

                    onClick = {
                        filtroSeleccionado = estado
                    },

                    label = {
                        Text(nombreFiltro)
                    }
                )
            }
        }

        // Cantidad de resultados
        Text(
            text = "${compromisosFiltrados.size} compromisos encontrados",
            style = MaterialTheme.typography.bodySmall
        )

        // Lista de compromisos
        LazyColumn(
            modifier = Modifier.fillMaxSize(),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(
                items = compromisosFiltrados,
                key = { it.id }
            ) { compromiso ->

                TarjetaCompromiso(
                    compromiso = compromiso,

                    onClick = {
                        onVerDetalle(compromiso.id)
                    }
                )
            }
        }
    }
}

@Composable
fun TarjetaCompromiso(
    compromiso: Compromiso,
    onClick: () -> Unit
) {

    val formatoPesos = NumberFormat.getNumberInstance(
        Locale("es", "CL")
    )

    val total = compromiso.monto +
            compromiso.recargoAplicado

    val nombreEstado = when (compromiso.estado) {

        EstadoCompromiso.PENDIENTE -> "Pendiente"

        EstadoCompromiso.VENCIDO -> "Vencido"

        EstadoCompromiso.PAGADO -> "Pagado"

        EstadoCompromiso.EN_REVISION -> "En revisión"

        EstadoCompromiso.ANULADO -> "Anulado"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {

        Column(
            modifier = Modifier.padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Nombre de la empresa
            Text(
                text = compromiso.empresa,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Concepto
            Text(
                text = compromiso.concepto,
                style = MaterialTheme.typography.bodyMedium
            )

            // Monto
            Text(
                text = "$ ${formatoPesos.format(total)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Fecha de vencimiento
            Text(
                text = "Vencimiento: ${compromiso.fechaVencimiento}",
                style = MaterialTheme.typography.bodySmall
            )

            // Estado
            Text(
                text = "Estado: $nombreEstado",
                style = MaterialTheme.typography.bodyMedium
            )

            // Folio
            Text(
                text = "Folio: ${compromiso.folio}",
                style = MaterialTheme.typography.bodySmall
            )

            TextButton(
                onClick = onClick
            ) {

                Text("Ver detalle")
            }
        }
    }
}
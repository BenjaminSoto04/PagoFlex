package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pagoflex.data.DatosSimulados
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.Pago
import com.example.pagoflex.viewmodel.PagosViewModel

import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.util.Locale

@Composable
fun HistorialScreen(
    pagosViewModel: PagosViewModel,
    onVolver: () -> Unit,
    onVerComprobante: (Long) -> Unit
) {

    // Observar pagos y compromisos
    val pagos by pagosViewModel
        .pagos
        .collectAsStateWithLifecycle()

    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // Relacionar cada pago con su compromiso
    val historial = pagos.mapNotNull { pago ->

        val compromiso = compromisos.find {
            it.id == pago.idCompromiso &&
                    it.idUsuario == DatosSimulados.ID_USUARIO_ACTUAL
        }

        if (compromiso != null) {
            pago to compromiso
        } else {
            null
        }
    }

    // Estados de los filtros
    var empresaSeleccionada by remember {
        mutableStateOf<Long?>(null)
    }

    var periodoSeleccionado by remember {
        mutableStateOf<String?>(null)
    }

    var menuEmpresaAbierto by remember {
        mutableStateOf(false)
    }

    var menuPeriodoAbierto by remember {
        mutableStateOf(false)
    }

    // Empresas presentes en el historial
    val empresas = historial
        .map { it.second }
        .distinctBy { it.idEmpresa }
        .sortedBy { it.empresa }

    // Períodos disponibles: MM-AAAA
    val periodos = historial
        .map { it.first.fechaHora.substring(3, 10) }
        .distinct()
        .sortedByDescending {
            YearMonth.parse(
                it,
                DateTimeFormatter.ofPattern("MM-uuuu")
            )
        }

    // Aplicar los filtros
    val historialFiltrado = historial.filter { registro ->

        val pago = registro.first
        val compromiso = registro.second

        val coincideEmpresa =
            empresaSeleccionada == null ||
                    compromiso.idEmpresa == empresaSeleccionada

        val coincidePeriodo =
            periodoSeleccionado == null ||
                    pago.fechaHora.substring(3, 10) ==
                    periodoSeleccionado

        coincideEmpresa && coincidePeriodo
    }

    // Ordenar desde el pago más reciente
    val formatoFechaHora =
        DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

    val historialOrdenado = historialFiltrado.sortedByDescending {
        LocalDateTime.parse(
            it.first.fechaHora,
            formatoFechaHora
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        TextButton(onClick = onVolver) {
            Text("← Volver")
        }

        Text(
            text = "Historial de pagos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Consulta tus pagos y comprobantes anteriores",
            style = MaterialTheme.typography.bodyMedium
        )

        // FILTROS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Filtro por empresa
            Box(
                modifier = Modifier.weight(1f)
            ) {

                OutlinedButton(
                    onClick = {
                        menuEmpresaAbierto = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = empresas
                            .find {
                                it.idEmpresa == empresaSeleccionada
                            }
                            ?.empresa ?: "Empresas",
                        maxLines = 1
                    )
                }

                DropdownMenu(
                    expanded = menuEmpresaAbierto,
                    onDismissRequest = {
                        menuEmpresaAbierto = false
                    }
                ) {

                    DropdownMenuItem(
                        text = { Text("Todas") },
                        onClick = {
                            empresaSeleccionada = null
                            menuEmpresaAbierto = false
                        }
                    )

                    empresas.forEach { empresa ->

                        DropdownMenuItem(
                            text = {
                                Text(empresa.empresa)
                            },

                            onClick = {
                                empresaSeleccionada =
                                    empresa.idEmpresa

                                menuEmpresaAbierto = false
                            }
                        )
                    }
                }
            }

            // Filtro por período
            Box(
                modifier = Modifier.weight(1f)
            ) {

                OutlinedButton(
                    onClick = {
                        menuPeriodoAbierto = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = periodoSeleccionado ?: "Período"
                    )
                }

                DropdownMenu(
                    expanded = menuPeriodoAbierto,
                    onDismissRequest = {
                        menuPeriodoAbierto = false
                    }
                ) {

                    DropdownMenuItem(
                        text = { Text("Todos") },
                        onClick = {
                            periodoSeleccionado = null
                            menuPeriodoAbierto = false
                        }
                    )

                    periodos.forEach { periodo ->

                        DropdownMenuItem(
                            text = {
                                Text(periodo)
                            },

                            onClick = {
                                periodoSeleccionado = periodo
                                menuPeriodoAbierto = false
                            }
                        )
                    }
                }
            }
        }

        Text(
            text = "${historialOrdenado.size} pagos encontrados",
            style = MaterialTheme.typography.bodySmall
        )

        // LISTA DE PAGOS
        LazyColumn(
            modifier = Modifier.weight(1f),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(
                items = historialOrdenado,
                key = { it.first.id }
            ) { registro ->

                TarjetaPagoHistorial(
                    pago = registro.first,
                    compromiso = registro.second,

                    onVerComprobante = {
                        onVerComprobante(
                            registro.second.id
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun TarjetaPagoHistorial(
    pago: Pago,
    compromiso: Compromiso,
    onVerComprobante: () -> Unit
) {

    val formatoPesos = NumberFormat.getNumberInstance(
        Locale("es", "CL")
    )

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = compromiso.empresa,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = compromiso.concepto
            )

            Text(
                text = "$ ${
                    formatoPesos.format(pago.montoPagado)
                }",

                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Fecha: ${pago.fechaHora}"
            )

            Text(
                text = "Canal: ${pago.canal}"
            )

            Text(
                text = "Comprobante: ${pago.numeroComprobante}"
            )

            Text(
                text = "Estado: Pagado",
                color = MaterialTheme.colorScheme.primary
            )

            TextButton(
                onClick = onVerComprobante
            ) {

                Text("Ver comprobante")
            }
        }
    }
}
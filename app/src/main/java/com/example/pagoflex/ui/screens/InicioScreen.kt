package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pagoflex.data.DatosSimulados
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.viewmodel.PagosViewModel

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InicioScreen(
    pagosViewModel: PagosViewModel,
    onVerCompromisos: () -> Unit,
    onVerHistorial: () -> Unit
) {

    // 1. Observar los compromisos del ViewModel
    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // 2. Obtener compromisos del usuario actual
    val misCompromisos = compromisos.filter {
        it.idUsuario == DatosSimulados.ID_USUARIO_ACTUAL
    }

    // 3. Obtener compromisos activos
    val activos = misCompromisos.filter {
        it.estado == EstadoCompromiso.PENDIENTE ||
                it.estado == EstadoCompromiso.VENCIDO
    }

    // 4. Calcular el total general pendiente
    val totalPendiente = activos.sumOf {
        it.monto + it.recargoAplicado
    }

    // 5. Formato de las fechas
    val formatoFecha =
        DateTimeFormatter.ofPattern("dd-MM-uuuu")

    // 6. Obtener el mes actual
    val mesActual = YearMonth.now()

    // 7. Filtrar compromisos del mes actual
    val compromisosDelMes = activos.filter { compromiso ->

        val fechaVencimiento = LocalDate.parse(
            compromiso.fechaVencimiento,
            formatoFecha
        )

        YearMonth.from(fechaVencimiento) == mesActual
    }

    // 8. Calcular el total del mes
    val totalDelMes = compromisosDelMes.sumOf {
        it.monto + it.recargoAplicado
    }

    // 9. Contar compromisos vencidos
    val cantidadVencidos = activos.count {
        it.estado == EstadoCompromiso.VENCIDO
    }

    // 10. Obtener el próximo vencimiento
    val proximoCompromiso = activos
        .filter {
            it.estado == EstadoCompromiso.PENDIENTE
        }
        .minByOrNull {
            LocalDate.parse(
                it.fechaVencimiento,
                formatoFecha
            )
        }

    // 11. Formato de pesos chilenos
    val formatoPesos = NumberFormat.getNumberInstance(
        Locale("es", "CL")
    )

    val totalGeneralFormateado =
        "$ ${formatoPesos.format(totalPendiente)}"

    val totalMesFormateado =
        "$ ${formatoPesos.format(totalDelMes)}"

    // 12. Interfaz de usuario
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // NOMBRE DE LA APLICACIÓN
        Text(
            text = "PagoFlex",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tus compromisos en un solo lugar",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // SALUDO AL USUARIO
        Text(
            text = "¡Hola, Camila! 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Este es el resumen de tus compromisos",
            style = MaterialTheme.typography.bodyMedium
        )

        // TARJETA DEL TOTAL GENERAL
        Card(
            modifier = Modifier.fillMaxWidth(),

            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Total general por pagar",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = totalGeneralFormateado,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${activos.size} compromisos activos",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // NUEVA TARJETA: TOTAL DEL MES ACTUAL
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Compromisos del mes actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = totalMesFormateado,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${compromisosDelMes.size} compromisos con vencimiento este mes",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Período: ${mesActual.monthValue.toString().padStart(2, '0')}-${mesActual.year}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // TARJETA DEL PRÓXIMO VENCIMIENTO
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Próximo vencimiento",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = proximoCompromiso
                        ?.fechaVencimiento
                        ?: "Sin vencimientos próximos",

                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (proximoCompromiso != null) {

                    Text(
                        text = proximoCompromiso.empresa,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // TARJETA DE COMPROMISOS VENCIDOS
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "Compromisos vencidos",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = cantidadVencidos.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (cantidadVencidos == 0)
                        "¡Estás al día!"
                    else
                        "Puedes revisar tus compromisos pendientes",

                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        // BOTÓN MIS COMPROMISOS
        Button(
            onClick = onVerCompromisos,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Mis compromisos")
        }

        // BOTÓN HISTORIAL
        OutlinedButton(
            onClick = onVerHistorial,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Historial de pagos")
        }
    }
}
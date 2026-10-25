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
import java.time.format.DateTimeFormatter
import java.util.Locale


@Composable
fun InicioScreen(
    pagosViewModel: PagosViewModel,
    onVerCompromisos: () -> Unit,
    onVerHistorial: () -> Unit
) {

    // Observar los compromisos del ViewModel
    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // Compromisos del usuario actual
    val misCompromisos = compromisos.filter {
        it.idUsuario == DatosSimulados.ID_USUARIO_ACTUAL
    }

    // Compromisos que todavía se deben pagar
    val activos = misCompromisos.filter {
        it.estado == EstadoCompromiso.PENDIENTE ||
                it.estado == EstadoCompromiso.VENCIDO
    }

    // Total por pagar
    val totalPendiente = activos.sumOf {
        it.monto + it.recargoAplicado
    }

    // Cantidad de compromisos vencidos
    val cantidadVencidos = activos.count {
        it.estado == EstadoCompromiso.VENCIDO
    }

    // Formato de las fechas
    val formatoFecha =
        DateTimeFormatter.ofPattern("dd-MM-uuuu")

    // Próximo compromiso que vencerá
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

    // Formato de pesos chilenos
    val formatoPesos = NumberFormat.getNumberInstance(
        Locale("es", "CL")
    )

    val totalFormateado =
        "$ ${formatoPesos.format(totalPendiente)}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Nombre de la aplicación
        Text(
            text = "PagoFlex",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tus compromisos en un solo lugar",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Saludo al usuario
        Text(
            text = "¡Hola, Camila! 👋",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Este es el resumen de tus compromisos",
            style = MaterialTheme.typography.bodyMedium
        )

        // Tarjeta del total pendiente
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
                    text = "Total por pagar",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = totalFormateado,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${activos.size} compromisos activos",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Tarjeta del próximo vencimiento
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Próximo vencimiento",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

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

        // Tarjeta de compromisos vencidos
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Compromisos vencidos",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

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

        Spacer(modifier = Modifier.height(8.dp))

        // Botón de compromisos
        Button(
            onClick = onVerCompromisos,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Mis compromisos")
        }

        // Botón del historial
        OutlinedButton(
            onClick = onVerHistorial,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Historial de pagos")
        }
    }
}
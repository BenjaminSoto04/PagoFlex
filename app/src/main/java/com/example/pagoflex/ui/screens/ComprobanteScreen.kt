package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pagoflex.data.DatosSimulados
import com.example.pagoflex.viewmodel.PagosViewModel

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ComprobanteScreen(
    idCompromiso: Long,
    pagosViewModel: PagosViewModel,
    onVolver: () -> Unit,
    onIrInicio: () -> Unit
) {

    // Observar los pagos registrados
    val pagos by pagosViewModel
        .pagos
        .collectAsStateWithLifecycle()

    // Observar los compromisos
    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // Buscar el compromiso del usuario
    val compromiso = compromisos.find {
        it.id == idCompromiso &&
                it.idUsuario == DatosSimulados.ID_USUARIO_ACTUAL
    }

    // Buscar el pago confirmado
    val pago = pagos.find {
        it.idCompromiso == idCompromiso
    }

    // Formato de pesos chilenos
    val formatoPesos = NumberFormat.getNumberInstance(
        Locale("es", "CL")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        TextButton(onClick = onVolver) {
            Text("← Volver")
        }

        Text(
            text = "Comprobante de pago",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Validar que existan los datos
        if (compromiso == null || pago == null) {

            Text(
                text = "No se encontró un comprobante para este compromiso."
            )

            Button(
                onClick = onIrInicio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al inicio")
            }

        } else {

            // Información de la cuota
            val cuota = if (
                compromiso.numeroCuota != null &&
                compromiso.totalCuotas != null
            ) {
                "${compromiso.numeroCuota} de ${compromiso.totalCuotas}"
            } else {
                "No aplica"
            }

            // Determinar si se pagó a tiempo
            val formatoFecha =
                DateTimeFormatter.ofPattern("dd-MM-uuuu")

            val fechaPago = LocalDate.parse(
                pago.fechaHora.substringBefore(" "),
                formatoFecha
            )

            val fechaVencimiento = LocalDate.parse(
                compromiso.fechaVencimiento,
                formatoFecha
            )

            val pagoATiempo =
                !fechaPago.isAfter(fechaVencimiento)

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    // Confirmación visual
                    Text(
                        text = "✓ Pago confirmado",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Total pagado",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "$ ${
                            formatoPesos.format(pago.montoPagado)
                        }",

                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    // Identificación del comprobante
                    Text(
                        text = "N° comprobante: ${pago.numeroComprobante}"
                    )

                    Text(
                        text = "Fecha y hora: ${pago.fechaHora}"
                    )

                    HorizontalDivider()

                    // Información de la empresa
                    Text(
                        text = "Empresa",
                        fontWeight = FontWeight.Bold
                    )

                    Text(compromiso.empresa)

                    Text(
                        text = "Concepto: ${compromiso.concepto}"
                    )

                    Text(
                        text = "Folio: ${compromiso.folio}"
                    )

                    Text(
                        text = "Cuota: $cuota"
                    )

                    Text(
                        text = "Vencimiento: ${compromiso.fechaVencimiento}"
                    )

                    HorizontalDivider()

                    // Desglose del pago
                    Text(
                        text = "Detalle del pago",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Monto original: $ ${
                            formatoPesos.format(compromiso.monto)
                        }"
                    )

                    Text(
                        text = "Recargo: $ ${
                            formatoPesos.format(
                                compromiso.recargoAplicado
                            )
                        }"
                    )

                    Text(
                        text = "Total pagado: $ ${
                            formatoPesos.format(pago.montoPagado)
                        }"
                    )

                    HorizontalDivider()

                    // Información del pago
                    Text(
                        text = "Canal: ${pago.canal}"
                    )

                    Text(
                        text = "Medio de pago: ${pago.medioPago}"
                    )

                    Text(
                        text = if (pagoATiempo)
                            "Pago realizado a tiempo"
                        else
                            "Pago realizado después del vencimiento"
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Text(
                    text = "Este comprobante corresponde a un pago simulado con fines académicos. No representa una transacción bancaria real.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onIrInicio,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Volver al inicio")
            }
        }
    }
}
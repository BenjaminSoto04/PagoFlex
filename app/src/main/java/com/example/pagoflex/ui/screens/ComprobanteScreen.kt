package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstilosTarjeta
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

    val pagos by pagosViewModel
        .pagos
        .collectAsStateWithLifecycle()

    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    val currentUserId = SessionManager.userId(LocalContext.current)
    val compromiso = compromisos.find {
        it.id == idCompromiso &&
                it.idUsuario == currentUserId
    }

    val pago = pagos.find {
        it.idCompromiso == idCompromiso
    }

    val formatoPesos = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("es-CL")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        TextButton(onClick = onVolver) {
            Text("← Volver")
        }

        Text(
            text = "Comprobante de pago",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (compromiso == null || pago == null) {

            Text(
                text = "No se encontró un comprobante para este compromiso."
            )

            BotonPrimario(text = "Volver al inicio", onClick = onIrInicio)

        } else {

            val cuota = if (
                compromiso.numeroCuota != null &&
                compromiso.totalCuotas != null
            ) {
                "${compromiso.numeroCuota} de ${compromiso.totalCuotas}"
            } else {
                "No aplica"
            }

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

            EstilosTarjeta.Tarjeta {
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

                Text(
                    text = "Nº comprobante: ${pago.numeroComprobante}"
                )

                Text(
                    text = "Fecha y hora: ${pago.fechaHora}"
                )

                HorizontalDivider()

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

            BotonPrimario(text = "Volver al inicio", onClick = onIrInicio)
        }
    }
}
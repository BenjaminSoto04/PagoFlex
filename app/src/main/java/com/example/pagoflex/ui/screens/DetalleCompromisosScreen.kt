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

import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.viewmodel.PagosViewModel
import androidx.compose.ui.platform.LocalContext

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun DetalleCompromisoScreen(
    idCompromiso: Long,
    pagosViewModel: PagosViewModel,
    onVolver: () -> Unit,
    onPagar: (Long) -> Unit
) {

    // Observar los cambios del ViewModel
    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // Buscar el compromiso seleccionado
    val currentUserId = SessionManager.userId(LocalContext.current)
    val compromiso = compromisos.find {
        it.id == idCompromiso &&
                it.idUsuario == currentUserId
    }

    // Comprobar si existe
    if (compromiso == null) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Text("Compromiso no encontrado")

            BotonPrimario(text = "Volver", onClick = onVolver, anchoCompleto = false)
        }

        return
    }

    // Formato de pesos chilenos
    val formatoPesos = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("es-CL")
    )

    // Total del compromiso
    val total = compromiso.monto +
            compromiso.recargoAplicado

    // Estado del compromiso
    val nombreEstado = when (compromiso.estado) {

        EstadoCompromiso.PENDIENTE -> "Pendiente"

        EstadoCompromiso.VENCIDO -> "Vencido"

        EstadoCompromiso.PAGADO -> "Pagado"

        EstadoCompromiso.EN_REVISION -> "En revisión"

        EstadoCompromiso.ANULADO -> "Anulado"
    }

    // Informacion de la cuota
    val cuota = if (
        compromiso.numeroCuota != null &&
        compromiso.totalCuotas != null
    ) {

        "${compromiso.numeroCuota} de ${compromiso.totalCuotas}"

    } else {

        "No aplica"
    }

    // Dias de atraso
    val formatoFecha =
        DateTimeFormatter.ofPattern("dd-MM-uuuu")

    val vencimiento = LocalDate.parse(
        compromiso.fechaVencimiento,
        formatoFecha
    )

    val diasAtraso = if (
        compromiso.estado == EstadoCompromiso.VENCIDO
    ) {

        ChronoUnit.DAYS.between(
            vencimiento,
            LocalDate.now()
        ).coerceAtLeast(0L)

    } else {

        0L
    }

    // Validacion de pago
    val mensajeValidacion =
        pagosViewModel.validarPago(compromiso)

    val puedePagar = mensajeValidacion == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        // Boton de Volver
        TextButton(onClick = onVolver) {
            Text("← Volver")
        }

        // Titulo
        Text(
            text = "Detalle del compromiso",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Informacion principal
        EstilosTarjeta.Tarjeta {
                Text(
                    text = compromiso.empresa,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = compromiso.concepto,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "Estado: $nombreEstado",
                    style = MaterialTheme.typography.titleMedium
                )

                HorizontalDivider()

                Text(
                    text = if (
                        compromiso.estado == EstadoCompromiso.PAGADO
                    ) "Monto original"
                    else "Monto total del compromiso",

                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = if (
                        compromiso.estado == EstadoCompromiso.PAGADO
                    ) "$ ${formatoPesos.format(compromiso.monto)}"
                    else "$ ${formatoPesos.format(total)}",

                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                Text(
                    text = "Folio: ${compromiso.folio}"
                )

                Text(
                    text = "Cuota: $cuota"
                )

                Text(
                    text = "Vencimiento: ${compromiso.fechaVencimiento}"
                )

                Text(
                    text = "Monto original: $ ${
                        formatoPesos.format(compromiso.monto)
                    }"
                )

                if (
                    compromiso.estado == EstadoCompromiso.VENCIDO
                ) {

                    Text(
                        text = "Días de atraso: $diasAtraso"
                    )

                    Text(
                        text = "Recargo: $ ${
                            formatoPesos.format(
                                compromiso.recargoAplicado
                            )
                        }"
                    )
                }
        }

        // Mensaje de validacion
        if (mensajeValidacion != null) {
            EstilosTarjeta.Tarjeta(
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(text = mensajeValidacion)
            }
        }

        // Boton de pago
        BotonPrimario(
            text = "Continuar al pago",
            onClick = { onPagar(compromiso.id) },
            enabled = puedePagar
        )
    }
}

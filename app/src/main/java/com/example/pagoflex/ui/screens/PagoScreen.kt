package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pagoflex.data.DatosSimulados
import com.example.pagoflex.viewmodel.PagosViewModel
import com.example.pagoflex.model.EstadoCompromiso

import java.text.NumberFormat
import java.util.Locale

@Composable
fun PagoScreen(
    idCompromiso: Long,
    pagosViewModel: PagosViewModel,
    onVolver: () -> Unit,
    onVerComprobante: (Long) -> Unit
) {

    // Observar los compromisos
    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // Observar los pagos
    val pagos by pagosViewModel
        .pagos
        .collectAsStateWithLifecycle()

    // Buscar el compromiso seleccionado
    val compromiso = compromisos.find {
        it.id == idCompromiso &&
                it.idUsuario == DatosSimulados.ID_USUARIO_ACTUAL
    }

    // Controlar el resultado
    var mensajeError by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var pagoExitoso by rememberSaveable {
        mutableStateOf(false)
    }

    // Formatear montos
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
            text = "Pago simulado",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (compromiso == null) {

            Text("Compromiso no encontrado")

        } else {

            // Buscar el comprobante generado
            val comprobante = pagos.find {
                it.idCompromiso == compromiso.id
            }

            // Comprobar si el compromiso ya fue pagado
            val yaEstaPagado =
                compromiso.estado == EstadoCompromiso.PAGADO

            // Comprobar si puede realizarse un nuevo pago
            val mensajeValidacion =
                pagosViewModel.validarPago(compromiso)

            if (yaEstaPagado && comprobante != null) {

                // Pantalla de éxito
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
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = if (pagoExitoso)
                                "¡Pago registrado!"
                            else
                                "Este compromiso ya fue pagado",

                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Tu pago simulado fue confirmado correctamente."
                        )

                        HorizontalDivider()

                        Text(
                            text = "Comprobante: ${comprobante.numeroComprobante}"
                        )

                        Text(
                            text = "Fecha: ${comprobante.fechaHora}"
                        )

                        Text(
                            text = "Total pagado: $ ${
                                formatoPesos.format(
                                    comprobante.montoPagado
                                )
                            }"
                        )

                        Text(
                            text = "Canal: ${comprobante.canal}"
                        )
                    }
                }

                Button(
                    onClick = {
                        onVerComprobante(compromiso.id)
                    },

                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text("Ver comprobante completo")
                }

                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver al compromiso")
                }

            } else if (mensajeValidacion != null) {

                // Mostrar por qué no puede pagar
                Card(
                    modifier = Modifier.fillMaxWidth(),

                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = "No es posible realizar el pago",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = mensajeValidacion
                        )

                        TextButton(
                            onClick = onVolver
                        ) {
                            Text("Volver al compromiso")
                        }
                    }
                }

            } else {

                // Información del compromiso
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            text = compromiso.empresa,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(compromiso.concepto)

                        HorizontalDivider()

                        Text("Monto del compromiso")

                        Text(
                            text = "$ ${
                                formatoPesos.format(
                                    compromiso.monto
                                )
                            }"
                        )

                        Text(
                            text = "Recargo: $ ${
                                formatoPesos.format(
                                    compromiso.recargoAplicado
                                )
                            }"
                        )

                        HorizontalDivider()

                        val total = compromiso.monto +
                                compromiso.recargoAplicado

                        Text(
                            text = "Total a pagar: $ ${
                                formatoPesos.format(total)
                            }",

                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Folio: ${compromiso.folio}"
                        )

                        Text("Medio: Pago simulado")
                    }
                }

                Text(
                    text = "Este pago es una simulación. No se cobrará dinero real.",
                    style = MaterialTheme.typography.bodySmall
                )

                // Mostrar errores
                if (mensajeError != null) {

                    Text(
                        text = mensajeError ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Confirmar el pago
                Button(
                    modifier = Modifier.fillMaxWidth(),

                    onClick = {

                        val resultado =
                            pagosViewModel.pagarCompromiso(
                                compromiso.id
                            )

                        if (resultado == null) {

                            pagoExitoso = true
                            mensajeError = null

                        } else {

                            mensajeError = resultado
                        }
                    }
                ) {

                    Text("Confirmar pago simulado")
                }

                // Cancelar sin registrar
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onVolver
                ) {

                    Text("Cancelar")
                }
            }
        }
    }
}
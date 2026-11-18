package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.styles.formatearPesos
import com.example.pagoflex.viewmodel.NuevoCompromisoViewModel

private val tiposCompromiso = listOf(
    "Crédito de consumo",
    "Compra en cuotas",
    "Arriendo",
    "Cuota social",
    "Mensualidad",
    "Aporte",
    "Seguro",
    "Otro"
)

@Composable
fun NuevoCompromisoScreen(
    onVolver: () -> Unit = {},
    onGuardado: () -> Unit = {},
    vm: NuevoCompromisoViewModel = viewModel()
) {
    val context = LocalContext.current
    val userId = SessionManager.userId(context)

    val empresas by vm.empresas.collectAsStateWithLifecycle()
    val errorEmpresa by vm.errorEmpresa.collectAsStateWithLifecycle()
    val errorConcepto by vm.errorConcepto.collectAsStateWithLifecycle()
    val errorMonto by vm.errorMonto.collectAsStateWithLifecycle()
    val errorCuotas by vm.errorCuotas.collectAsStateWithLifecycle()
    val errorFecha by vm.errorFecha.collectAsStateWithLifecycle()
    val errorGeneral by vm.errorGeneral.collectAsStateWithLifecycle()
    val guardando by vm.guardando.collectAsStateWithLifecycle()
    val guardado by vm.guardado.collectAsStateWithLifecycle()

    var empresaSeleccionada by remember { mutableStateOf<EmpresaEntity?>(null) }
    var tipo by remember { mutableStateOf(tiposCompromiso.first()) }
    var detalle by remember { mutableStateOf("") }
    var modoMonto by remember { mutableStateOf("POR_CUOTA") }
    var montoTexto by remember { mutableStateOf("") }
    var cuotasTexto by remember { mutableStateOf("1") }
    var fecha by remember { mutableStateOf("") }
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.reset() }
    LaunchedEffect(guardado) {
        if (guardado) {
            mostrarConfirmacion = true
        }
    }

    val montoIngresado = montoTexto.filter { it.isDigit() }.toIntOrNull()
    val cuotasIngresadas = cuotasTexto.filter { it.isDigit() }.toIntOrNull()
    val montoPorCuota = when {
        montoIngresado == null || cuotasIngresadas == null || cuotasIngresadas < 1 -> null
        modoMonto == "TOTAL" -> montoIngresado / cuotasIngresadas
        else -> montoIngresado
    }
    val totalEstimado = montoPorCuota?.let { it * (cuotasIngresadas ?: 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver") }

        Text(
            text = "Nuevo compromiso",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Registra un compromiso de pago. Se guardará como pendiente en la cuota 1 con un folio único.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EstiloCampo.CampoSelector(
            label = "Empresa",
            opciones = empresas,
            textoOpcion = { it.nombreFantasia },
            seleccionado = empresaSeleccionada?.nombreFantasia,
            placeholder = if (empresas.isEmpty()) "No hay empresas registradas" else "Selecciona una empresa",
            enabled = empresas.isNotEmpty(),
            isError = errorEmpresa != null,
            onSeleccionar = {
                empresaSeleccionada = it
                vm.limpiarErrorEmpresa()
            }
        )
        EstiloCampo.MensajeError(errorEmpresa)

        EstiloCampo.CampoSelector(
            label = "Tipo de compromiso",
            opciones = tiposCompromiso,
            textoOpcion = { it },
            seleccionado = tipo,
            isError = errorConcepto != null,
            onSeleccionar = {
                tipo = it
                vm.limpiarErrorConcepto()
            }
        )
        EstiloCampo.MensajeError(errorConcepto)

        EstiloCampo.CampoTexto(
            value = detalle,
            onValueChange = { detalle = it },
            label = "Descripción (opcional)",
            placeholder = { Text("Ej.: cuota 1 de 12 del crédito") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        EstiloCampo.CampoSelector(
            label = "Forma de ingresar el monto",
            opciones = listOf("POR_CUOTA", "TOTAL"),
            textoOpcion = { if (it == "TOTAL") "Monto total" else "Monto por cuota" },
            seleccionado = if (modoMonto == "TOTAL") "Monto total" else "Monto por cuota",
            onSeleccionar = { modoMonto = it }
        )

        EstiloCampo.CampoTexto(
            value = montoTexto,
            onValueChange = {
                montoTexto = it.filter { c -> c.isDigit() }
                vm.limpiarErrorMonto()
            },
            label = if (modoMonto == "TOTAL") "Monto total" else "Monto por cuota",
            placeholder = { Text("Ej.: 45000") },
            isError = errorMonto != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        EstiloCampo.MensajeError(errorMonto)

        EstiloCampo.CampoTexto(
            value = cuotasTexto,
            onValueChange = {
                cuotasTexto = it.filter { c -> c.isDigit() }
                vm.limpiarErrorCuotas()
            },
            label = "Número de cuotas",
            placeholder = { Text("Ej.: 12") },
            isError = errorCuotas != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )
        EstiloCampo.MensajeError(errorCuotas)

        EstiloCampo.CampoTexto(
            value = fecha,
            onValueChange = {
                fecha = it
                vm.limpiarErrorFecha()
            },
            label = "Fecha de vencimiento",
            placeholder = { Text("dd-MM-aaaa") },
            isError = errorFecha != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            )
        )
        EstiloCampo.MensajeError(errorFecha)

        if (montoPorCuota != null) {
            EstilosTarjeta.Tarjeta(
                centrada = false,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Resumen del compromiso",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${cuotasIngresadas ?: 0} cuota(s) de ${formatearPesos(montoPorCuota)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Total: ${formatearPesos(totalEstimado ?: 0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        EstiloCampo.MensajeError(errorGeneral)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonSecundario(
                text = "Cancelar",
                onClick = onVolver,
                modifier = Modifier.weight(1f)
            )
            BotonPrimario(
                text = if (guardando) "Guardando..." else "Guardar compromiso",
                onClick = {
                    vm.guardar(
                        userId = userId,
                        empresaId = empresaSeleccionada?.id,
                        tipo = tipo,
                        detalle = detalle,
                        modoMonto = modoMonto,
                        montoTexto = montoTexto,
                        cuotasTexto = cuotasTexto,
                        fecha = fecha
                    )
                },
                enabled = !guardando,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (mostrarConfirmacion) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                mostrarConfirmacion = false
                vm.reset()
                onGuardado()
            },
            title = { Text("Compromiso creado") },
            text = {
                Text("El compromiso quedó registrado como pendiente y ya aparece en Mis compromisos.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarConfirmacion = false
                        vm.reset()
                        onGuardado()
                    }
                ) { Text("Aceptar") }
            }
        )
    }
}

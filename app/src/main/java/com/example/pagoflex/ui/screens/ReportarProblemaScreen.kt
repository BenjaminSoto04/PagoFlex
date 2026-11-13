package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.R
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.theme.PagoFlexTheme
import com.example.pagoflex.viewmodel.PagosViewModel
import com.example.pagoflex.viewmodel.ReportarProblemaViewModel

@Composable
fun ReportarProblemaScreen(
    modifier: Modifier = Modifier,
    onVolver: () -> Unit = {},
    reportarViewModel: ReportarProblemaViewModel? = null,
    pagosViewModel: PagosViewModel = viewModel()
) {
    val vm = reportarViewModel ?: viewModel()
    val enviado by vm.enviado.collectAsStateWithLifecycle()
    val currentUserId = SessionManager.userId(LocalContext.current)
    var tipoProblema by remember { mutableStateOf("") }
    var compromiso by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    val errorTipo by vm.errorTipo.collectAsStateWithLifecycle()
    val errorCompromiso by vm.errorCompromiso.collectAsStateWithLifecycle()
    val errorDescripcion by vm.errorDescripcion.collectAsStateWithLifecycle()
    val errorGeneral by vm.errorGeneral.collectAsStateWithLifecycle()

    val todosLosCompromisos by pagosViewModel.compromisos.collectAsStateWithLifecycle()
    val misCompromisos = remember(todosLosCompromisos, currentUserId) {
        todosLosCompromisos.filter { it.idUsuario == currentUserId }
    }

    LaunchedEffect(Unit) { vm.reset() }

    val tiposProblema = listOf(
        "Problema con un pago",
        "Error en un comprobante",
        "Compromiso incorrecto",
        "Problema con la aplicación",
        "Otro"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onVolver) {
            Text("← Volver")
        }

        Text(
            text = stringResource(R.string.titulo_app),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = stringResource(R.string.centro_ayuda),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = stringResource(R.string.reportar_problema),
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = stringResource(R.string.reportar_descripcion)
        )

        EstiloCampo.CampoSelector(
            label = stringResource(R.string.tipo_problema),
            opciones = tiposProblema,
            textoOpcion = { it },
            seleccionado = tipoProblema.ifEmpty { null },
            placeholder = stringResource(R.string.selecciona_opcion),
            isError = errorTipo != null,
            onSeleccionar = {
                tipoProblema = it
                vm.limpiarErrorTipo()
            }
        )
        EstiloCampo.MensajeError(errorTipo)

        EstiloCampo.CampoSelector(
            label = stringResource(R.string.compromiso_relacionado),
            opciones = misCompromisos,
            textoOpcion = { "${it.folio} · ${it.empresa}" },
            seleccionado = compromiso.ifEmpty { null },
            placeholder = stringResource(R.string.selecciona_compromiso),
            isError = errorCompromiso != null,
            enabled = misCompromisos.isNotEmpty(),
            onSeleccionar = {
                compromiso = it.folio
                vm.limpiarErrorCompromiso()
            }
        )
        EstiloCampo.MensajeError(errorCompromiso)
        if (misCompromisos.isEmpty()) {
            Text(
                text = "No tienes compromisos registrados para asociar al reporte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        EstiloCampo.CampoMultilinea(
            value = descripcion,
            onValueChange = {
                descripcion = it
                vm.limpiarErrorDescripcion()
            },
            label = stringResource(R.string.descripcion_problema),
            placeholder = { Text(stringResource(R.string.placeholder_descripcion)) },
            minLines = 4,
            isError = errorDescripcion != null
        )
        EstiloCampo.MensajeError(errorDescripcion)

        EstiloCampo.MensajeError(errorGeneral)

        BotonPrimario(
            text = stringResource(R.string.enviar_reporte),
            onClick = {
                vm.validarYEnviar(tipoProblema, compromiso, descripcion, currentUserId)
            }
        )
    }

    if (enviado) {
        AlertDialog(
            onDismissRequest = {
                vm.reset()
                tipoProblema = ""
                compromiso = ""
                descripcion = ""
            },
            title = {
                Text(stringResource(R.string.reporte_recibido))
            },
            text = {
                Text(stringResource(R.string.reporte_validado))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.reset()
                        tipoProblema = ""
                        compromiso = ""
                        descripcion = ""
                    }
                ) {
                    Text(stringResource(R.string.aceptar))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportarProblemaPreview() {
    PagoFlexTheme {
        ReportarProblemaScreen()
    }
}

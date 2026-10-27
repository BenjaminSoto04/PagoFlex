package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pagoflex.R
import com.example.pagoflex.ui.theme.PagoFlexTheme

@Composable
fun ReportarProblemaScreen(
    modifier: Modifier = Modifier,
    onEnviarReporte: (
        tipo: String,
        compromiso: String,
        descripcion: String
    ) -> Unit = { _, _, _ -> }
) {
    var tipoProblema by remember { mutableStateOf("") }
    var compromiso by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    var menuTipoAbierto by remember { mutableStateOf(false) }
    var menuCompromisoAbierto by remember { mutableStateOf(false) }

    var errorTipo by remember { mutableStateOf(false) }
    var errorCompromiso by remember { mutableStateOf(false) }
    var errorDescripcion by remember { mutableStateOf(false) }
    var reporteEnviado by remember { mutableStateOf(false) }

    val tiposProblema = listOf(
        "Problema con un pago",
        "Error en un comprobante",
        "Compromiso incorrecto",
        "Problema con la aplicación",
        "Otro"
    )

    val compromisos = listOf(
        "Cuenta de agua",
        "Electricidad",
        "Internet"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        Text(stringResource(R.string.tipo_problema))

        Box {
            OutlinedButton(
                onClick = { menuTipoAbierto = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tipoProblema.ifEmpty {
                        stringResource(R.string.selecciona_opcion)
                    }
                )
            }

            DropdownMenu(
                expanded = menuTipoAbierto,
                onDismissRequest = {
                    menuTipoAbierto = false
                }
            ) {
                tiposProblema.forEach { tipo ->
                    DropdownMenuItem(
                        text = { Text(tipo) },
                        onClick = {
                            tipoProblema = tipo
                            menuTipoAbierto = false
                            errorTipo = false
                        }
                    )
                }
            }
        }

        if (errorTipo) {
            Text(
                text = stringResource(R.string.error_tipo),
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(stringResource(R.string.compromiso_relacionado))

        Box {
            OutlinedButton(
                onClick = {
                    menuCompromisoAbierto = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = compromiso.ifEmpty {
                        stringResource(R.string.selecciona_compromiso)
                    }
                )
            }

            DropdownMenu(
                expanded = menuCompromisoAbierto,
                onDismissRequest = {
                    menuCompromisoAbierto = false
                }
            ) {
                compromisos.forEach { nombre ->
                    DropdownMenuItem(
                        text = { Text(nombre) },
                        onClick = {
                            compromiso = nombre
                            menuCompromisoAbierto = false
                            errorCompromiso = false
                        }
                    )
                }
            }
        }

        if (errorCompromiso) {
            Text(
                text = stringResource(R.string.error_compromiso),
                color = MaterialTheme.colorScheme.error
            )
        }

        OutlinedTextField(
            value = descripcion,
            onValueChange = {
                descripcion = it
                errorDescripcion = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(stringResource(R.string.descripcion_problema))
            },
            placeholder = {
                Text(stringResource(R.string.placeholder_descripcion))
            },
            minLines = 4,
            isError = errorDescripcion
        )

        if (errorDescripcion) {
            Text(
                text = stringResource(R.string.error_descripcion),
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                errorTipo = tipoProblema.isBlank()
                errorCompromiso = compromiso.isBlank()
                errorDescripcion = descripcion.trim().length < 10

                val formularioValido =
                    !errorTipo &&
                            !errorCompromiso &&
                            !errorDescripcion

                if (formularioValido) {
                    onEnviarReporte(
                        tipoProblema,
                        compromiso,
                        descripcion.trim()
                    )
                    reporteEnviado = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.enviar_reporte))
        }
    }

    if (reporteEnviado) {
        AlertDialog(
            onDismissRequest = {
                reporteEnviado = false
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
                        reporteEnviado = false
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

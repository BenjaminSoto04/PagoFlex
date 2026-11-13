package com.example.pagoflex.ui.styles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

object EstiloCampo {

    val alturaMinima = 56.dp

    @Composable
    fun CampoTexto(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        placeholder: (@Composable () -> Unit)? = null,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        isError: Boolean = false,
        singleLine: Boolean = true,
        minLines: Int = 1,
        maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        trailingIcon: (@Composable () -> Unit)? = null
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            label = { Text(label) },
            placeholder = placeholder,
            isError = isError,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon = trailingIcon,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }

    @Composable
    fun CampoContrasena(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        visible: Boolean,
        onToggleVisible: () -> Unit,
        modifier: Modifier = Modifier,
        isError: Boolean = false,
        keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
    ) {
        CampoTexto(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            isError = isError,
            singleLine = true,
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = keyboardOptions,
            trailingIcon = {
                TextButton(onClick = onToggleVisible) {
                    Text(if (visible) "Ocultar" else "Ver")
                }
            }
        )
    }

    @Composable
    fun CampoMultilinea(
        value: String,
        onValueChange: (String) -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        placeholder: (@Composable () -> Unit)? = null,
        isError: Boolean = false,
        minLines: Int = 3,
        maxLines: Int = 6
    ) {
        CampoTexto(
            value = value,
            onValueChange = onValueChange,
            label = label,
            modifier = modifier,
            placeholder = placeholder,
            isError = isError,
            singleLine = false,
            minLines = minLines,
            maxLines = maxLines
        )
    }

    @Composable
    fun <T> CampoSelector(
        label: String,
        opciones: List<T>,
        textoOpcion: (T) -> String,
        onSeleccionar: (T) -> Unit,
        modifier: Modifier = Modifier,
        seleccionado: String? = null,
        placeholder: String = "Selecciona una opción",
        isError: Boolean = false,
        enabled: Boolean = true
    ) {
        var expandido by remember { mutableStateOf(false) }
        Box(modifier.fillMaxWidth()) {
            CampoTexto(
                value = seleccionado.orEmpty(),
                onValueChange = {},
                label = label,
                placeholder = { Text(placeholder) },
                readOnly = true,
                enabled = enabled,
                isError = isError,
                trailingIcon = { Text(if (expandido) "▲" else "▼") }
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(enabled = enabled) { expandido = true }
            )
            DropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false }
            ) {
                opciones.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(textoOpcion(opcion)) },
                        onClick = {
                            onSeleccionar(opcion)
                            expandido = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun MensajeError(texto: String?, modifier: Modifier = Modifier) {
        if (!texto.isNullOrBlank()) {
            Text(
                text = texto,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}
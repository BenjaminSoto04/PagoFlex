package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.local.entity.PreguntasSeguridad
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.Validaciones
import com.example.pagoflex.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit = {},
    onVolver: () -> Unit = {}
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val loading by authViewModel.loading.collectAsStateWithLifecycle()
    val errorNombre by authViewModel.errorNombre.collectAsStateWithLifecycle()
    val errorEmail by authViewModel.errorEmail.collectAsStateWithLifecycle()
    val errorPassword by authViewModel.errorPassword.collectAsStateWithLifecycle()
    val errorTelefono by authViewModel.errorTelefono.collectAsStateWithLifecycle()
    val errorEmpresa by authViewModel.errorEmpresa.collectAsStateWithLifecycle()
    val errorPreguntaSeguridad by authViewModel.errorPreguntaSeguridad.collectAsStateWithLifecycle()
    val errorRespuestaSeguridad by authViewModel.errorRespuestaSeguridad.collectAsStateWithLifecycle()
    val errorGeneral by authViewModel.errorGeneral.collectAsStateWithLifecycle()

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("USUARIO") }
    var telefono by remember { mutableStateOf("") }
    var empresa by remember { mutableStateOf("") }
    var preguntaSeguridad by remember { mutableStateOf(PreguntasSeguridad.LISTA.first()) }
    var respuestaSeguridad by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var errorConfirm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.limpiarErrores()
    }

    LaunchedEffect(user) {
        if (user != null) onRegisterSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Crear cuenta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        EstiloCampo.CampoTexto(
            value = nombre,
            onValueChange = { nombre = it; authViewModel.limpiarErrorNombre() },
            label = "Nombre completo",
            isError = errorNombre != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )
        EstiloCampo.MensajeError(errorNombre)

        EstiloCampo.CampoTexto(
            value = email,
            onValueChange = { email = it; authViewModel.limpiarErrorEmail() },
            label = "Correo electrónico",
            isError = errorEmail != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        EstiloCampo.MensajeError(errorEmail)

        EstiloCampo.CampoContrasena(
            value = password,
            onValueChange = { password = it; authViewModel.limpiarErrorPassword(); errorConfirm = null },
            label = "Contraseña",
            visible = showPass,
            onToggleVisible = { showPass = !showPass },
            isError = errorPassword != null
        )
        EstiloCampo.MensajeError(errorPassword)

        EstiloCampo.CampoContrasena(
            value = confirm,
            onValueChange = { confirm = it; errorConfirm = null },
            label = "Confirmar contraseña",
            visible = showConfirm,
            onToggleVisible = { showConfirm = !showConfirm },
            isError = errorConfirm != null
        )
        EstiloCampo.MensajeError(errorConfirm)

        EstiloCampo.CampoSelector(
            label = "Pregunta de seguridad",
            opciones = PreguntasSeguridad.LISTA,
            textoOpcion = { it },
            seleccionado = preguntaSeguridad,
            onSeleccionar = {
                preguntaSeguridad = it
                authViewModel.limpiarErrorPreguntaSeguridad()
            },
            isError = errorPreguntaSeguridad != null
        )
        EstiloCampo.MensajeError(errorPreguntaSeguridad)

        EstiloCampo.CampoTexto(
            value = respuestaSeguridad,
            onValueChange = {
                respuestaSeguridad = it
                authViewModel.limpiarErrorRespuestaSeguridad()
            },
            label = "Respuesta de seguridad",
            placeholder = { Text("Ingresa tu respuesta...") },
            isError = errorRespuestaSeguridad != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )
        EstiloCampo.MensajeError(errorRespuestaSeguridad)

        EstiloCampo.CampoSelector(
            label = "Rol",
            opciones = listOf("USUARIO", "EJECUTIVO", "AGENTE"),
            textoOpcion = { it },
            seleccionado = rol,
            onSeleccionar = { rol = it }
        )

        EstiloCampo.CampoTexto(
            value = telefono,
            onValueChange = { telefono = it; authViewModel.limpiarErrorTelefono() },
            label = "Teléfono (opcional)",
            isError = errorTelefono != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            )
        )
        EstiloCampo.MensajeError(errorTelefono)

        EstiloCampo.CampoTexto(
            value = empresa,
            onValueChange = { empresa = it; authViewModel.limpiarErrorEmpresa() },
            label = "Empresa (opcional)",
            isError = errorEmpresa != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            )
        )
        EstiloCampo.MensajeError(errorEmpresa)

        EstiloCampo.MensajeError(errorGeneral)

        BotonPrimario(
            text = if (loading) "Registrando..." else "Registrar",
            onClick = {
                val errorConfirmacion = Validaciones.confirmacion(password, confirm)
                errorConfirm = errorConfirmacion
                if (errorConfirmacion != null) return@BotonPrimario
                authViewModel.register(
                    nombre = nombre,
                    email = email,
                    password = password,
                    rol = rol,
                    telefono = telefono.ifBlank { null },
                    empresa = empresa.ifBlank { null },
                    preguntaSeguridad = preguntaSeguridad,
                    respuestaSeguridad = respuestaSeguridad
                )
            },
            enabled = !loading
        )
    }
}
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
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.viewmodel.AccountViewModel
import com.example.pagoflex.viewmodel.AuthViewModel

@Composable
fun RecuperarPasswordScreen(
    accountViewModel: AccountViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onVolver: () -> Unit = {},
    onIrRestablecer: (String) -> Unit = {}
) {
    val preguntasOpciones by authViewModel.preguntasRecuperacion.collectAsStateWithLifecycle()
    val errorEmail by authViewModel.errorRecuperacionEmail.collectAsStateWithLifecycle()
    val errorPregunta by authViewModel.errorRecuperacionPregunta.collectAsStateWithLifecycle()
    val errorRespuesta by authViewModel.errorRecuperacionRespuesta.collectAsStateWithLifecycle()
    val errorGeneral by authViewModel.errorGeneral.collectAsStateWithLifecycle()
    val loading by authViewModel.loading.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var preguntaSeleccionada by remember { mutableStateOf("") }
    var respuesta by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        accountViewModel.clear()
        authViewModel.limpiarEstadoRecuperacion()
    }

    LaunchedEffect(email) {
        authViewModel.actualizarPreguntasParaUsuario(email)
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
        Text(
            text = "Recuperar contraseña",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ingresa tu correo o usuario, selecciona tu pregunta de seguridad registrada y escribe tu respuesta para continuar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EstiloCampo.CampoTexto(
            value = email,
            onValueChange = {
                email = it
                authViewModel.limpiarErrorRecuperacionEmail()
                authViewModel.limpiarErrorGeneral()
            },
            label = "Correo electrónico o usuario",
            isError = errorEmail != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        EstiloCampo.MensajeError(errorEmail)

        EstiloCampo.CampoSelector(
            label = "Pregunta de seguridad",
            opciones = preguntasOpciones,
            textoOpcion = { it },
            seleccionado = preguntaSeleccionada,
            placeholder = "Selecciona tu pregunta de seguridad",
            onSeleccionar = {
                preguntaSeleccionada = it
                authViewModel.limpiarErrorRecuperacionPregunta()
                authViewModel.limpiarErrorGeneral()
            },
            isError = errorPregunta != null
        )
        EstiloCampo.MensajeError(errorPregunta)

        EstiloCampo.CampoTexto(
            value = respuesta,
            onValueChange = {
                respuesta = it
                authViewModel.limpiarErrorRecuperacionRespuesta()
                authViewModel.limpiarErrorGeneral()
            },
            label = "Respuesta de seguridad",
            placeholder = { Text("Escribe tu respuesta...") },
            isError = errorRespuesta != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            )
        )
        EstiloCampo.MensajeError(errorRespuesta)

        EstiloCampo.MensajeError(errorGeneral)

        BotonPrimario(
            text = if (loading) "Verificando..." else "Verificar y continuar",
            onClick = {
                authViewModel.verificarPreguntaSeguridad(
                    identificador = email,
                    preguntaSeleccionada = preguntaSeleccionada,
                    respuestaIngresada = respuesta,
                    onExito = { correoVerificado ->
                        onIrRestablecer(correoVerificado)
                    }
                )
            },
            enabled = !loading
        )
    }
}

@Composable
fun RestablecerPasswordScreen(
    email: String,
    authViewModel: AuthViewModel = viewModel(),
    onVolver: () -> Unit = {},
    onPasswordActualizada: () -> Unit = {}
) {
    val errorNueva by authViewModel.errorNuevaPassword.collectAsStateWithLifecycle()
    val errorConfirmar by authViewModel.errorConfirmarPassword.collectAsStateWithLifecycle()
    val errorGeneral by authViewModel.errorGeneral.collectAsStateWithLifecycle()
    val mensaje by authViewModel.mensajeRecuperacion.collectAsStateWithLifecycle()
    val loading by authViewModel.loading.collectAsStateWithLifecycle()

    var nuevaPassword by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var mostrarNueva by remember { mutableStateOf(false) }
    var mostrarConfirmar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.limpiarErroresNuevaPassword()
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
        Text(
            text = "Nueva contraseña",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Cuenta verificada: $email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        EstiloCampo.CampoContrasena(
            value = nuevaPassword,
            onValueChange = {
                nuevaPassword = it
                authViewModel.limpiarErroresNuevaPassword()
            },
            label = "Nueva contraseña",
            visible = mostrarNueva,
            onToggleVisible = { mostrarNueva = !mostrarNueva },
            isError = errorNueva != null
        )
        EstiloCampo.MensajeError(errorNueva)

        EstiloCampo.CampoContrasena(
            value = confirmarPassword,
            onValueChange = {
                confirmarPassword = it
                authViewModel.limpiarErroresNuevaPassword()
            },
            label = "Confirmar nueva contraseña",
            visible = mostrarConfirmar,
            onToggleVisible = { mostrarConfirmar = !mostrarConfirmar },
            isError = errorConfirmar != null
        )
        EstiloCampo.MensajeError(errorConfirmar)

        EstiloCampo.MensajeError(errorGeneral)

        mensaje?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        BotonPrimario(
            text = if (loading) "Guardando..." else "Guardar nueva contraseña",
            onClick = {
                authViewModel.guardarNuevaPasswordRecuperada(
                    email = email,
                    nuevaPassword = nuevaPassword,
                    confirmarPassword = confirmarPassword,
                    onExito = { onPasswordActualizada() }
                )
            },
            enabled = !loading
        )
    }
}
package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.ui.theme.Primario
import com.example.pagoflex.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: (String) -> Unit = {},
    onIrRegistro: () -> Unit = {},
    onIrRecuperar: () -> Unit = {}
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val loading by authViewModel.loading.collectAsStateWithLifecycle()
    val errorEmail by authViewModel.errorEmail.collectAsStateWithLifecycle()
    val errorPassword by authViewModel.errorPassword.collectAsStateWithLifecycle()
    val errorGeneral by authViewModel.errorGeneral.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    LaunchedEffect(user) { user?.let { onLoginSuccess(it.rol) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "PagoFlex",
            style = MaterialTheme.typography.headlineLarge,
            color = Primario,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Tu plataforma de pagos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        EstilosTarjeta.Tarjeta(centrada = true) {
            Text(
                "Iniciar sesión",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                "Ingresa tus credenciales",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            EstiloCampo.CampoTexto(
                value = email,
                onValueChange = {
                    email = it
                    authViewModel.limpiarErrorEmail()
                    authViewModel.limpiarErrorGeneral()
                },
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
                onValueChange = {
                    password = it
                    authViewModel.limpiarErrorPassword()
                    authViewModel.limpiarErrorGeneral()
                },
                label = "Contraseña",
                visible = showPass,
                onToggleVisible = { showPass = !showPass },
                isError = errorPassword != null
            )
            EstiloCampo.MensajeError(errorPassword)
            EstiloCampo.MensajeError(errorGeneral)
            BotonPrimario(
                text = if (loading) "Ingresando..." else "Ingresar",
                onClick = { authViewModel.login(email, password) },
                enabled = !loading
            )
            TextButton(onClick = onIrRecuperar) { Text("¿Olvidaste tu contraseña?") }
            TextButton(onClick = onIrRegistro) { Text("Crear cuenta") }
        }
    }
}

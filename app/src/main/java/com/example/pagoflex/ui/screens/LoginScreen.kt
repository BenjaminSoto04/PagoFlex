package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.ui.components.PagoFlexCard
import com.example.pagoflex.ui.components.PagoFlexPrimaryButton
import com.example.pagoflex.ui.components.SectionHeader
import com.example.pagoflex.ui.theme.Primario
import com.example.pagoflex.ui.theme.TextoOscuro
import com.example.pagoflex.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: (String) -> Unit = {},
    onIrRegistro: () -> Unit = {},
    onIrRecuperar: () -> Unit = {}
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()
    val loading by authViewModel.loading.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    LaunchedEffect(user) { user?.let { onLoginSuccess(it.rol) } }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PagoFlex", style = MaterialTheme.typography.headlineLarge, color = Primario, fontWeight = FontWeight.Bold)
        Text("Tu plataforma de pagos", style = MaterialTheme.typography.bodyMedium, color = TextoOscuro)
        Spacer(Modifier.height(24.dp))
        PagoFlexCard {
            SectionHeader("Iniciar sesion", "Ingresa tus credenciales")
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contrasena") }, visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { showPass = !showPass }) { Text(if (showPass) "Ocultar" else "Ver") } }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(16.dp))
            PagoFlexPrimaryButton(if (loading) "Ingresando..." else "Ingresar", { authViewModel.login(email, password) }, enabled = !loading)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onIrRecuperar) { Text("¿Olvidaste tu contrasena?") }
            TextButton(onClick = onIrRegistro) { Text("Crear cuenta") }
        }
    }
}
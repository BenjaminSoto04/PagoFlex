package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit = {},
    onVolver: () -> Unit = {}
) {
    val user by authViewModel.currentUser.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()
    val loading by authViewModel.loading.collectAsStateWithLifecycle()

    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf("USUARIO") }
    var telefono by remember { mutableStateOf("") }
    var empresa by remember { mutableStateOf("") }
    var menuRol by remember { mutableStateOf(false) }

    LaunchedEffect(user) {
        if (user != null) onRegisterSuccess()
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Crear cuenta", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contrasena") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirmar contrasena") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        if (password != confirm && confirm.isNotEmpty()) {
            Text("Las contrasenas no coinciden", color = MaterialTheme.colorScheme.error)
        }
        Box {
            OutlinedButton(onClick = { menuRol = true }, modifier = Modifier.fillMaxWidth()) { Text("Rol: $rol") }
            DropdownMenu(expanded = menuRol, onDismissRequest = { menuRol = false }) {
                listOf("USUARIO", "EJECUTIVO", "AGENTE").forEach { r ->
                    DropdownMenuItem(text = { Text(r) }, onClick = { rol = r; menuRol = false })
                }
            }
        }
        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono (opcional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = empresa, onValueChange = { empresa = it }, label = { Text("Empresa (opcional)") }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            if (password != confirm) { authViewModel.clearError(); return@Button }
            authViewModel.register(nombre, email, password, rol, telefono.ifBlank { null }, empresa.ifBlank { null })
        }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) "Registrando..." else "Registrar")
        }
    }
}

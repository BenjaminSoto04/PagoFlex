package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.viewmodel.AccountViewModel

@Composable
fun RecuperarPasswordScreen(
    accountViewModel: AccountViewModel = viewModel(),
    onVolver: () -> Unit = {}
) {
    val message by accountViewModel.message.collectAsStateWithLifecycle()
    val error by accountViewModel.error.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Text("Recuperar contrasena", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = newPass, onValueChange = { newPass = it }, label = { Text("Nueva contrasena") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("Confirmar") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Button(onClick = { accountViewModel.recoverPassword(email, newPass, confirm) }, modifier = Modifier.fillMaxWidth()) {
            Text("Restablecer")
        }
    }
}

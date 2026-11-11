package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.viewmodel.ChatViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun BandejaAgenteScreen(onVolver: () -> Unit = {}, onCerrarSesion: () -> Unit = {}, onVerPerfil: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: ChatViewModel = viewModel()
    val agenteId = SessionManager.userId(context)
    val convs by vm.conversaciones.collectAsStateWithLifecycle()
    val mensajes by vm.mensajes.collectAsStateWithLifecycle()
    val convId by vm.conversacionId.collectAsStateWithLifecycle()
    var texto by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.initAgente() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Bandeja de atencion", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { onVerPerfil() }) { Text("Mi perfil") }
        }
        if (convId == null) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(convs) { c ->
                    Card(onClick = { vm.abrirConversacion(c.id) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Usuario ID: ${c.idUsuario}", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Estado: ${c.estado}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mensajes) { m ->
                    val esAgente = m.rolRemitente == "AGENTE"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (esAgente) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (esAgente) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Text(m.contenido, Modifier.padding(8.dp), color = if (esAgente) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = texto, onValueChange = { texto = it }, modifier = Modifier.weight(1f))
                Button(onClick = { vm.responderAgente(agenteId, texto); texto = "" }, enabled = texto.isNotBlank()) { Text("Responder") }
            }
            TextButton(onClick = { vm.cerrarConversacion() }) { Text("Volver a bandeja") }
        }
    }
}

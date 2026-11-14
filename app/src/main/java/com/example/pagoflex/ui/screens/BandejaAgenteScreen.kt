package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.styles.BotonCompacto
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.BotonTexto
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.viewmodel.ChatViewModel

@Composable
fun BandejaAgenteScreen(
    onVolver: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    onVerPerfil: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: ChatViewModel = viewModel()
    val agenteId = SessionManager.userId(context)
    val convs by vm.conversaciones.collectAsStateWithLifecycle()
    val usuariosAsignados by vm.usuariosAsignados.collectAsStateWithLifecycle()
    val convsFiltradas = convs.filter { it.idUsuario in usuariosAsignados }
    val mensajes by vm.mensajes.collectAsStateWithLifecycle()
    val convId by vm.conversacionId.collectAsStateWithLifecycle()
    val conversacionActual by vm.conversacionActual.collectAsStateWithLifecycle()
    val nombresUsuarios by vm.nombresUsuarios.collectAsStateWithLifecycle()

    var texto by remember { mutableStateOf("") }
    var errorRespuesta by remember { mutableStateOf<String?>(null) }

    val estaFinalizada = conversacionActual?.estado.equals("FINALIZADO", ignoreCase = true)

    LaunchedEffect(Unit) { vm.initAgente() }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bandeja de atención",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            BotonTexto(text = "Mi perfil", onClick = { onVerPerfil() })
        }

        if (convId == null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(convsFiltradas, key = { it.id }) { c ->
                    EstilosTarjeta.Tarjeta(
                        relleno = PaddingValues(12.dp),
                        onClick = { vm.abrirConversacion(c.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Usuario: ${nombresUsuarios[c.idUsuario] ?: "ID ${c.idUsuario}"}",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = c.estado,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (c.estado.equals("FINALIZADO", ignoreCase = true)) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        Text(
                            text = "Fecha: ${c.fechaCreacion}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val uid = conversacionActual?.idUsuario
                    Text(
                        text = "Solicitud de: ${if (uid != null) nombresUsuarios[uid] ?: "ID $uid" else "Usuario"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Estado actual: ${conversacionActual?.estado ?: "PENDIENTE"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (estaFinalizada) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                if (!estaFinalizada) {
                    BotonSecundario(
                        text = "Finalizar solicitud",
                        onClick = {
                            texto = ""
                            errorRespuesta = null
                            vm.finalizarConversacion(convId)
                        },
                        anchoCompleto = false
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mensajes, key = { it.id }) { m ->
                    val esAgente = m.rolRemitente == "AGENTE"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (esAgente) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (esAgente) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = m.contenido,
                                modifier = Modifier.padding(10.dp),
                                color = if (esAgente) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (estaFinalizada) {
                EstilosTarjeta.Tarjeta(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    relleno = PaddingValues(12.dp)
                ) {
                    Text(
                        text = "Esta solicitud se encuentra FINALIZADA. El historial de mensajes permanece visible en el registro, pero la conversación está bloqueada para nuevas respuestas.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    EstiloCampo.CampoTexto(
                        value = texto,
                        onValueChange = {
                            texto = it
                            errorRespuesta = null
                        },
                        label = "Respuesta",
                        placeholder = {
                            Text(
                                if (estaFinalizada) {
                                    "Solicitud finalizada"
                                } else {
                                    "Escribe tu respuesta..."
                                }
                            )
                        },
                        enabled = !estaFinalizada,
                        isError = errorRespuesta != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    EstiloCampo.MensajeError(errorRespuesta)
                }
                Spacer(Modifier.width(8.dp))
                BotonCompacto(
                    text = "Responder",
                    enabled = !estaFinalizada,
                    onClick = {
                        when {
                            estaFinalizada -> errorRespuesta = "La solicitud está finalizada."
                            texto.isBlank() -> errorRespuesta = "Escribe una respuesta antes de enviar"
                            else -> {
                                vm.responderAgente(agenteId, texto)
                                texto = ""
                                errorRespuesta = null
                            }
                        }
                    }
                )
            }
            BotonTexto(
                text = "Volver a bandeja",
                onClick = {
                    texto = ""
                    errorRespuesta = null
                    vm.cerrarConversacion()
                }
            )
        }
    }
}

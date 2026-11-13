package com.example.pagoflex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.styles.BotonCompacto
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.viewmodel.ChatViewModel

@Composable
fun AsistenteVirtualScreen(onVolver: () -> Unit = {}) {
    val context = LocalContext.current
    val vm: ChatViewModel = viewModel()
    val userId = SessionManager.userId(context)
    val mensajes by vm.mensajes.collectAsStateWithLifecycle()
    val conversacionActual by vm.conversacionActual.collectAsStateWithLifecycle()
    var texto by remember { mutableStateOf("") }
    var errorConsulta by remember { mutableStateOf<String?>(null) }

    val estaFinalizada = conversacionActual?.estado.equals("FINALIZADO", ignoreCase = true)

    LaunchedEffect(userId) {
        if (userId > 0) vm.initUsuario(userId)
    }

    val enviarConsulta: () -> Unit = {
        when {
            estaFinalizada -> errorConsulta = "Esta solicitud ya fue finalizada y no admite nuevos mensajes."
            texto.isBlank() -> errorConsulta = "Escribe tu consulta antes de enviar"
            userId <= 0 -> errorConsulta = "No pudimos identificar tu usuario. Vuelve a iniciar sesión."
            else -> {
                vm.enviarUsuario(userId, texto)
                texto = ""
                errorConsulta = null
            }
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingInterno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterno)
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "PF",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agente PagoFlex",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (estaFinalizada) {
                            "Solicitud finalizada"
                        } else {
                            "Asistente virtual - En línea"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                TextButton(onClick = onVolver) {
                    Text(text = "Volver", color = Color.White)
                }
            }

            EstilosTarjeta.Tarjeta(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                relleno = PaddingValues(12.dp)
            ) {
                Text(
                    text = "Consulta tus dudas sobre pagos, compromisos y vencimientos.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 13.sp
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mensajes, key = { it.id }) { m ->
                    val esUsuario = m.rolRemitente == "USUARIO"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            modifier = Modifier.width(280.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (esUsuario) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = m.contenido,
                                modifier = Modifier.padding(14.dp),
                                color = if (esUsuario) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            if (estaFinalizada) {
                EstilosTarjeta.Tarjeta(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    relleno = PaddingValues(12.dp)
                ) {
                    Text(
                        text = "Esta solicitud ha sido marcada como FINALIZADA por el agente. El historial permanece disponible para consulta, pero no es posible enviar nuevos mensajes.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EstiloCampo.CampoTexto(
                        value = texto,
                        onValueChange = {
                            texto = it
                            errorConsulta = null
                        },
                        label = "Mensaje",
                        placeholder = {
                            Text(
                                if (estaFinalizada) {
                                    "Solicitud finalizada"
                                } else {
                                    "Escribe tu consulta..."
                                }
                            )
                        },
                        enabled = !estaFinalizada,
                        singleLine = false,
                        minLines = 1,
                        maxLines = 3,
                        isError = errorConsulta != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { enviarConsulta() })
                    )
                    EstiloCampo.MensajeError(errorConsulta)
                }

                Spacer(modifier = Modifier.width(8.dp))

                BotonCompacto(
                    text = "Enviar",
                    onClick = { enviarConsulta() },
                    enabled = !estaFinalizada
                )
            }
        }
    }
}
package com.example.pagoflex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class MensajeChat(
    val texto: String,
    val esUsuario: Boolean
)

@Composable
fun PanelAgenteConversacionalScreen(
    onVolver: () -> Unit = {}
) {
    val mensajes = remember {
        mutableStateListOf(
            MensajeChat(
                texto = "¡Hola! Soy el asistente virtual de PagoFlex. ¿En qué puedo ayudarte?",
                esUsuario = false
            )
        )
    }

    var textoMensaje by remember { mutableStateOf("") }

    fun enviarMensaje() {
        val consulta = textoMensaje.trim()

        if (consulta.isNotEmpty()) {
            mensajes.add(
                MensajeChat(
                    texto = consulta,
                    esUsuario = true
                )
            )

            val consultaNormalizada = consulta.lowercase()

            val respuesta = when {
                "pago" in consultaNormalizada ->
                    "Puedes consultar tus compromisos y seleccionar uno disponible para pagar. Recuerda que esta versión utiliza respuestas simuladas."

                "vencimiento" in consultaNormalizada ||
                        "vencido" in consultaNormalizada ->
                    "Para revisar un vencimiento, consulta la fecha de tu compromiso. Si tienes dudas sobre un recargo, contacta al área de atención."

                "compromiso" in consultaNormalizada ||
                        "deuda" in consultaNormalizada ->
                    "Puedes revisar el estado, el monto y la fecha de tus compromisos en la sección correspondiente de PagoFlex."

                "problema" in consultaNormalizada ||
                        "ayuda" in consultaNormalizada ->
                    "Estoy aquí para ayudarte. Si necesitas informar un inconveniente, puedes utilizar la pantalla Reportar un problema."

                "hola" in consultaNormalizada ->
                    "¡Hola! Puedes preguntarme sobre pagos, compromisos o vencimientos."

                else ->
                    "Gracias por tu consulta. Puedo orientarte sobre pagos, compromisos y vencimientos. ¿Qué necesitas revisar?"
            }

            mensajes.add(
                MensajeChat(
                    texto = respuesta,
                    esUsuario = false
                )
            )

            textoMensaje = ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingInterno ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterno)
        ) {

            // Encabezado del agente
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
                        text = "Asistente virtual · En línea",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                TextButton(onClick = onVolver) {
                    Text(
                        text = "Volver",
                        color = Color.White
                    )
                }
            }

            // Mensaje informativo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Consulta tus dudas sobre pagos, compromisos y vencimientos.",
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 13.sp
                )
            }

            // Conversación
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mensajes) { mensaje ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mensaje.esUsuario) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        Surface(
                            modifier = Modifier.width(280.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = if (mensaje.esUsuario) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = mensaje.texto,
                                modifier = Modifier.padding(14.dp),
                                color = if (mensaje.esUsuario) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Campo para escribir y enviar mensajes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textoMensaje,
                    onValueChange = { textoMensaje = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("Escribe tu consulta...")
                    },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { enviarMensaje() }
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { enviarMensaje() },
                    enabled = textoMensaje.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Enviar")
                }
            }
        }
    }
}

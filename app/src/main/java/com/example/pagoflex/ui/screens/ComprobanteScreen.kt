package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.viewmodel.PagosViewModel
import androidx.compose.ui.platform.LocalContext

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ComprobanteScreen(
    idCompromiso: Long,
    pagosViewModel: PagosViewModel,
    onVolver: () -> Unit,
    onIrInicio: () -> Unit
) {

    // Observar los pagos registrados
    val pagos by pagosViewModel
        .pagos
        .collectAsStateWithLifecycle()

    // Observar los compromisos
    val compromisos by pagosViewModel
        .compromisos
        .collectAsStateWithLifecycle()

    // Buscar el compromiso del usuario
    val currentUserId = SessionManager.userId(LocalContext.current)
    val compromiso = compromisos.find {
        it.id == idCompromiso &&
                it.idUsuario == currentUserId
    }

    // Buscar el pago confirmado
    val pago = pagos.find {
        it.idCompromiso == idCompromiso
    }

    // Formato de pesos chilenos
    val formatoPesos = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("es-CL")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        TextButton(onClick = onVolver) {
            Text("← Volver")
        }

        Text(
            text = "Comprobante de pago",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Validar que existan los datos
        if (compromiso == null || pago == null) {

            Text(
                text = "No se encontro un comprobante para este compromiso."
            )

            Button(
                onClick = onIrInicio,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver al inicio")
            }

        } else {

            // Informacion de la cuota
            val cuota = if (
                compromiso.numeroCuota != null &&
                compromiso.totalCuotas != null
            ) {
                "${compromiso.numeroCuota} de ${compromiso.totalCuotas}"
            } else {
                "No aplica"
            }

            // Determinar si se pago a tiempo
            val formatoFecha =
                DateTimeFormatter.ofPattern("dd-MM-uuuu")

            val fechaPago = LocalDate.parse(
                pago.fechaHora.substringBefore(" "),
                formatoFecha
            )

            val fechaVencimiento = LocalDate.parse(
                compromiso.fechaVencimiento,
                formatoFecha
            )

            val pagoATiempo =
                !fechaPago.isAfter(fechaVencimiento)

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    // Confirmacion visual
                    Text(
                        text = "✓ Pago confirmado",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Total pagado",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "$ ${
                            formatoPesos.format(pago.montoPagado)
                        }",

                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    // Identificacion del comprobante
                    Text(
                        text = "N° comprobante: ${pago.numeroComprobante}"
                    )

                    Text(
                        text = "Fecha y hora: ${pago.fechaHora}"
                    )

                    HorizontalDivider()

                    // Informacion de la empresa
                    Text(
                        text = "Empresa",
                        fontWeight = FontWeight.Bold
                    )

                    Text(compromiso.empresa)

                    Text(
                        text = "Concepto: ${compromiso.concepto}"
                    )

                    Text(
                        text = "Folio: ${compromiso.folio}"
                    )

                    Text(
                        text = "Cuota: $cuota"
                    )

                    Text(
                        text = "Vencimiento: ${compromiso.fechaVencimiento}"
                    )

                    HorizontalDivider()

                    // Desglose del pago
                    Text(
                        text = "Detalle del pago",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Monto original: $ ${
                            formatoPesos.format(compromiso.monto)
                        }"
                    )

                    Text(
                        text = "Recargo: $ ${
                            formatoPesos.format(
                                compromiso.recargoAplicado
                            )
                        }"
                    )

                    Text(
                        text = "Total pagado: $ ${
                            formatoPesos.format(pago.montoPagado)
                        }"
                    )

                    HorizontalDivider()

                    // Informacion del pago
                    Text(
                        text = "Canal: ${pago.canal}"
                    )

                    Text(
                        text = "Medio de pago: ${pago.medioPago}"
                    )

                    Text(
                        text = if (pagoATiempo)
                            "Pago realizado a tiempo"
                        else
                            "Pago realizado despues del vencimiento"
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Text(
                    text = "Este comprobante corresponde a un pago simulado con fines academicos. No representa una transaccion bancaria real.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val context = LocalContext.current
            var photoUri by remember { mutableStateOf<Uri?>(null) }
            val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
                if (ok && photoUri != null) {
                    pago?.let { pagosViewModel.adjuntarFotoPago(it.id, photoUri.toString()) }
                }
            }
            Button(onClick = {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "comprobante_${System.currentTimeMillis()}.jpg")
                file.parentFile?.mkdirs()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                photoUri = uri
                cameraLauncher.launch(uri)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Tomar foto del comprobante")
            }
            pago?.fotoUri?.let { Text("Foto adjunta: $it", style = MaterialTheme.typography.bodySmall) }

            Button(
                onClick = onIrInicio,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Volver al inicio")
            }
        }
    }
}

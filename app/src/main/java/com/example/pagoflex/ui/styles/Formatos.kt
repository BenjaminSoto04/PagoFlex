package com.example.pagoflex.ui.styles

import com.example.pagoflex.model.EstadoCompromiso
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Formato de dinero usado en toda la app: $1.234.567 */
fun formatearPesos(monto: Int): String = formatearPesos(monto.toLong())

fun formatearPesos(monto: Long): String =
    "$" + monto.toString().reversed().chunked(3).joinToString(".").reversed()

/**
 * Formato de indicadores economicos en pesos chilenos: $ 39.843 CLP.
 * Con [decimales] > 0 se usa coma decimal (por ejemplo $ 0,24 CLP).
 */
fun formatearCLP(valor: Double?, decimales: Int = 0): String {
    if (valor == null || valor.isNaN() || valor.isInfinite()) return "Sin dato"
    val texto = if (decimales <= 0) {
        formatearPesos(valor.roundToInt())
    } else {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CL")).apply {
            minimumFractionDigits = decimales
            maximumFractionDigits = decimales
        }.format(valor)
    }
    return "$ $texto CLP"
}

/** Las fechas se guardan como dd-MM-uuuu; para mostrar se usa dd/MM/uuuu. */
fun formatearFecha(fecha: String): String = fecha.replace('-', '/')

/** Etiqueta en español del estado de un compromiso. */
fun etiquetaEstado(estado: EstadoCompromiso): String = when (estado) {
    EstadoCompromiso.PENDIENTE -> "Pendiente"
    EstadoCompromiso.VENCIDO -> "Vencido"
    EstadoCompromiso.PAGADO -> "Pagado"
    EstadoCompromiso.EN_REVISION -> "En revisión"
    EstadoCompromiso.ANULADO -> "Anulado"
}

val formatoFechaCorta: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu")
val formatoFechaHoraCorta: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

/** Devuelve la fecha de un texto dd-MM-uuuu (o dd-MM-uuuu HH:mm) sin lanzar excepciones. */
fun parsearFecha(texto: String): LocalDate? = runCatching {
    if (texto.length > 10) LocalDateTime.parse(texto, formatoFechaHoraCorta).toLocalDate()
    else LocalDate.parse(texto, formatoFechaCorta)
}.getOrNull()

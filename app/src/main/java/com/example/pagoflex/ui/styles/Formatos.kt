package com.example.pagoflex.ui.styles

import com.example.pagoflex.model.EstadoCompromiso
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Formato de dinero usado en toda la app: $1.234.567 */
fun formatearPesos(monto: Int): String = formatearPesos(monto.toLong())

fun formatearPesos(monto: Long): String =
    "$" + monto.toString().reversed().chunked(3).joinToString(".").reversed()

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

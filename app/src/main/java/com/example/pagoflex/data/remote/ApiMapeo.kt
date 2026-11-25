package com.example.pagoflex.data.remote

import com.example.pagoflex.model.EstadoCompromiso
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Utilidades compartidas por los repositorios que hablan con PagoFlexMS.
 *
 * Los microservicios serializan `LocalDate`/`LocalDateTime` en formato ISO
 * (`2026-12-05` / `2026-12-05T10:15:30`), mientras que la app guarda en Room
 * `dd-MM-uuuu` y `dd-MM-uuuu HH:mm`. Tambien traducen los estados del
 * catalogo (`"Pendiente"`, `"En revision"`, ...) al enum local.
 */

private val FORMATO_APP_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu")

/**
 * Ejecuta una llamada Retrofit y devuelve el body solo cuando la respuesta fue
 * exitosa. Cualquier error de red, timeout o body nulo devuelve `null` para que
 * el repositorio caiga al fallback de Room.
 */
suspend fun <T> ejecutarRemoto(llamada: suspend () -> Response<T>): T? = try {
    val response = llamada()
    if (response.isSuccessful && response.body() != null) response.body() else null
} catch (e: Exception) {
    e.printStackTrace()
    null
}

/**
 * Variante acotada de [ejecutarRemoto] para lecturas puntuales que ocurren en
 * medio de una accion del usuario: si el microservicio no responde en [ms]
 * milisegundos se cancela la llamada y el repositorio cae a Room.
 */
suspend fun <T> ejecutarRemotoConEspera(
    ms: Long = 3_000,
    llamada: suspend () -> Response<T>
): T? = withTimeoutOrNull(ms) { ejecutarRemoto(llamada) }

/** `"2026-12-05"` (API) -> `"05-12-2026"` (Room). */
fun fechaDesdeApi(valor: String?): String? {
    val texto = valor?.trim().orEmpty()
    if (texto.isEmpty()) return null
    if (texto.length >= 10 && texto[2] == '-') return texto.take(10) // ya viene en formato app
    return try {
        LocalDate.parse(texto.take(10)).format(FORMATO_APP_FECHA)
    } catch (e: Exception) {
        null
    }
}

/** `"2026-12-05T10:15:30"` (API) -> `"05-12-2026 10:15"` (Room). */
fun fechaHoraDesdeApi(valor: String?): String? {
    val texto = valor?.trim().orEmpty()
    if (texto.isEmpty()) return null
    if (texto.length >= 10 && texto[2] == '-') return texto.take(16) // ya viene en formato app
    return try {
        val fecha = LocalDate.parse(texto.take(10)).format(FORMATO_APP_FECHA)
        val hora = texto.substringAfter('T', "").take(5).ifBlank { "00:00" }
        "$fecha $hora"
    } catch (e: Exception) {
        null
    }
}

/** `"05-12-2026"` (Room) -> `"2026-12-05"` (API). */
fun fechaParaApi(valor: String): String = try {
    LocalDate.parse(valor.trim(), FORMATO_APP_FECHA).toString()
} catch (e: Exception) {
    LocalDate.now().toString()
}

/** `"05-12-2026 10:15"` (Room) -> `"2026-12-05T10:15:00"` (API). */
fun fechaHoraParaApi(valor: String): String = try {
    val fecha = LocalDate.parse(valor.trim().take(10), FORMATO_APP_FECHA)
    val partesHora = valor.trim().substringAfter(' ', "00:00").split(":")
    val hora = partesHora.getOrNull(0)?.trim()?.padStart(2, '0') ?: "00"
    val minuto = partesHora.getOrNull(1)?.trim()?.take(2)?.padStart(2, '0') ?: "00"
    "${fecha}T$hora:$minuto:00"
} catch (e: Exception) {
    "${LocalDate.now()}T00:00:00"
}

private fun normalizar(texto: String?): String =
    Normalizer.normalize(texto?.trim().orEmpty(), Normalizer.Form.NFD)
        .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
        .uppercase(Locale.ROOT)

/** Nombre del catalogo del microservicio -> enum local. */
fun estadoCompromisoDesdeApi(valor: String?): EstadoCompromiso {
    val estado = normalizar(valor).replace(' ', '_')
    return when {
        estado.contains("PAG") -> EstadoCompromiso.PAGADO
        estado.contains("VENC") -> EstadoCompromiso.VENCIDO
        estado.contains("REVIS") -> EstadoCompromiso.EN_REVISION
        estado.contains("ANUL") -> EstadoCompromiso.ANULADO
        else -> EstadoCompromiso.PENDIENTE
    }
}

/** Enum local -> nombre del catalogo del microservicio. */
fun estadoCompromisoParaApi(estado: EstadoCompromiso): String = when (estado) {
    EstadoCompromiso.PENDIENTE -> "Pendiente"
    EstadoCompromiso.VENCIDO -> "Vencido"
    EstadoCompromiso.PAGADO -> "Pagado"
    EstadoCompromiso.EN_REVISION -> "En revisión"
    EstadoCompromiso.ANULADO -> "Anulado"
}

package com.example.pagoflex.util

import java.util.Locale
import java.util.UUID

/**
 * Genera identificadores cortos y unicos para compromisos y pagos.
 *
 * El folio combina la marca de tiempo actual con un fragmento aleatorio para
 * evitar colisiones incluso si se generan varios folios en el mismo segundo.
 */
object FolioGenerator {

    fun generar(prefijo: String = "PF"): String {
        val marcaTiempo = System.currentTimeMillis().toString(36).uppercase(Locale.ROOT)
        val aleatorio = UUID.randomUUID().toString().take(4).uppercase(Locale.ROOT)
        return "$prefijo-$marcaTiempo-$aleatorio"
    }
}

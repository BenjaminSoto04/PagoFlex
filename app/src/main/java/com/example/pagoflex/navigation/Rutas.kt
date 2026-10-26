package com.example.pagoflex.navigation

sealed class Ruta(val ruta: String) {

    data object Inicio : Ruta("inicio")

    data object Compromisos : Ruta("compromisos")

    data object Historial : Ruta("historial")

    data object DetalleCompromiso : Ruta("detalle/{id}") {

        fun crear(id: Long): String {
            return "detalle/$id"
        }
    }

    data object Pago : Ruta("pago/{id}") {

        fun crear(id: Long): String {
            return "pago/$id"
        }
    }

    data object Comprobante : Ruta("comprobante/{id}") {

        fun crear(id: Long): String {
            return "comprobante/$id"
        }
    }
}
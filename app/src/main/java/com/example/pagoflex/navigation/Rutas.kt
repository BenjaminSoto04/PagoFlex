package com.example.pagoflex.navigation

sealed class Ruta(val ruta: String) {

    data object Inicio : Ruta("inicio")

    data object Compromisos : Ruta("compromisos")

    data object Historial : Ruta("historial")
}
package com.example.pagoflex.navigation

sealed class Ruta(val ruta: String) {
    data object Compromiso : Ruta("compromiso")
}
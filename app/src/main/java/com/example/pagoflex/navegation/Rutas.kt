package com.example.pagoflex.navegation

sealed class Ruta(val ruta: String) {
    data object ReportarProblema : Ruta("reportar_problema")
}

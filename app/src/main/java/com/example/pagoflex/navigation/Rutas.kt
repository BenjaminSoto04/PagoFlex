package com.example.pagoflex.navigation

sealed class Ruta(val ruta: String) {

    data object Login : Ruta("login")

    data object Register : Ruta("register")

    data object Recuperar : Ruta("recuperar")

    data object Perfil : Ruta("perfil")

    data object Inicio : Ruta("inicio")

    data object Compromisos : Ruta("compromisos")

    data object Historial : Ruta("historial")

    data object DetalleCompromiso : Ruta("detalle/{id}") {
        fun crear(id: Long): String = "detalle/$id"
    }

    data object Pago : Ruta("pago/{id}") {
        fun crear(id: Long): String = "pago/$id"
    }

    data object Comprobante : Ruta("comprobante/{id}") {
        fun crear(id: Long): String = "comprobante/$id"
    }

    data object ReportarProblema : Ruta("reportar_problema")

    data object PanelAgente : Ruta("panel_agente")
    data object AsistenteVirtual : Ruta("asistente_virtual")
    data object BandejaAgente : Ruta("bandeja_agente")

    data object PanelEjecutivo : Ruta("panel_ejecutivo")

    data object ResumenRecaudacion : Ruta("resumen_recaudacion")

    data object GestionCompromisos : Ruta("gestion_compromisos")
}

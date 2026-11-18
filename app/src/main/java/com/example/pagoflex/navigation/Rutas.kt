package com.example.pagoflex.navigation

import android.net.Uri

sealed class Ruta(val ruta: String) {

    data object Login : Ruta("login")

    data object Register : Ruta("register")

    data object Recuperar : Ruta("recuperar")

    data object RestablecerPassword : Ruta("restablecer_password/{email}") {
        fun crear(email: String): String = "restablecer_password/${Uri.encode(email)}"
    }

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
    data object MisReportes : Ruta("mis_reportes")
    data object DetalleReporte : Ruta("detalle_reporte/{id}") {
        fun crear(id: Long): String = "detalle_reporte/$id"
    }

    data object NuevoCompromiso : Ruta("nuevo_compromiso")

    data object DashboardAgente : Ruta("dashboard_agente")
    data object PanelAgente : Ruta("panel_agente")
    data object AsistenteVirtual : Ruta("asistente_virtual")
    data object BandejaAgente : Ruta("bandeja_agente")
    data object BandejaReportesAgente : Ruta("bandeja_reportes_agente")
    data object DetalleReporteAgente : Ruta("detalle_reporte_agente/{id}") {
        fun crear(id: Long): String = "detalle_reporte_agente/$id"
    }
    data object GestionRoles : Ruta("gestion_roles")

    data object PanelEjecutivo : Ruta("panel_ejecutivo")

    data object ResumenRecaudacion : Ruta("resumen_recaudacion")

    data object GestionCompromisos : Ruta("gestion_compromisos")
}
package com.example.pagoflex.navegation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pagoflex.ui.screens.GestionAnulacionCompromisosScreen
import com.example.pagoflex.ui.screens.PanelAgenteConversacionalScreen
import com.example.pagoflex.ui.screens.ReportarProblemaScreen
import com.example.pagoflex.ui.screens.PanelEjecutivoEmpresaScreen
import com.example.pagoflex.ui.screens.ResumenRecaudacionReportesScreen

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "gestion_compromisos"
    ) {
        composable("reportar_problema") {
            ReportarProblemaScreen()
        }

        composable("panel_agente") {
            PanelAgenteConversacionalScreen()
        }

        composable("panel_ejecutivo") {
            PanelEjecutivoEmpresaScreen()
        }

        composable("resumen_recaudacion") {
            ResumenRecaudacionReportesScreen()
        }

        composable("gestion_compromisos") {
            GestionAnulacionCompromisosScreen()
        }
    }
}
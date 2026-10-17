package com.example.pagoflex.navegation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pagoflex.ui.screens.ReportarProblemaScreen

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Ruta.ReportarProblema.ruta) {
        composable(Ruta.ReportarProblema.ruta) {
            ReportarProblemaScreen()
        }
    }
}
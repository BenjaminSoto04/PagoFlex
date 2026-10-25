package com.example.pagoflex.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.pagoflex.ui.screens.InicioScreen
import com.example.pagoflex.viewmodel.PagosViewModel

@Composable
fun AppNavegacion() {

    val navController = rememberNavController()

    // ViewModel de PagoFlex
    val pagosViewModel: PagosViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Ruta.Inicio.ruta
    ) {

        // Pantalla de inicio
        composable(Ruta.Inicio.ruta) {

            InicioScreen(
                pagosViewModel = pagosViewModel,

                onVerCompromisos = {
                    navController.navigate(
                        Ruta.Compromisos.ruta
                    )
                },

                onVerHistorial = {
                    navController.navigate(
                        Ruta.Historial.ruta
                    )
                }
            )
        }

        // Pantalla temporal de compromisos
        composable(Ruta.Compromisos.ruta) {

            PantallaTemporal(
                titulo = "Mis compromisos",
                onVolver = {
                    navController.popBackStack()
                }
            )
        }

        // Pantalla temporal del historial
        composable(Ruta.Historial.ruta) {

            PantallaTemporal(
                titulo = "Historial de pagos",
                onVolver = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun PantallaTemporal(
    titulo: String,
    onVolver: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(text = titulo)

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(text = "Pantalla en construcción")

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(onClick = onVolver) {
            Text("Volver al inicio")
        }
    }
}
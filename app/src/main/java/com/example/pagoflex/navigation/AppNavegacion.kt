package com.example.pagoflex.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument

import com.example.pagoflex.ui.screens.InicioScreen
import com.example.pagoflex.ui.screens.CompromisosScreen
import com.example.pagoflex.ui.screens.DetalleCompromisoScreen
import com.example.pagoflex.ui.screens.PagoScreen
import com.example.pagoflex.ui.screens.ComprobanteScreen
import com.example.pagoflex.ui.screens.HistorialScreen
import com.example.pagoflex.viewmodel.PagosViewModel

@Composable
fun AppNavegacion() {

    val navController = rememberNavController()

    // ViewModel de PagoFlex
    val pagosViewModel: PagosViewModel = viewModel()

    // Obtener el ciclo de vida de la pantalla
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observar cuándo el usuario vuelve a la aplicación
    DisposableEffect(lifecycleOwner, pagosViewModel) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                pagosViewModel.refrescarVencimientos()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {

            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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


        composable(Ruta.Compromisos.ruta) {

            CompromisosScreen(
                pagosViewModel = pagosViewModel,

                onVolver = {
                    navController.popBackStack()
                },

                onVerDetalle = { id ->

                    navController.navigate(
                        Ruta.DetalleCompromiso.crear(id)
                    )
                }
            )
        }

        composable(
            route = Ruta.DetalleCompromiso.ruta,

            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            )

        ) { entrada ->

            val id = entrada.arguments?.getLong("id") ?: 0L

            DetalleCompromisoScreen(
                idCompromiso = id,
                pagosViewModel = pagosViewModel,

                onVolver = {
                    navController.popBackStack()
                },

                onPagar = { idCompromiso ->

                    navController.navigate(
                        Ruta.Pago.crear(idCompromiso)
                    )
                }
            )
        }

        composable(
            route = Ruta.Pago.ruta,

            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            )

        ) { entrada ->

            val idCompromiso =
                entrada.arguments?.getLong("id") ?: 0L


            PagoScreen(
                idCompromiso = idCompromiso,

                pagosViewModel = pagosViewModel,

                onVolver = {
                    navController.popBackStack()
                },

                onVerComprobante = { id ->

                    navController.navigate(
                        Ruta.Comprobante.crear(id)
                    )
                }
            )
        }

        composable(
            route = Ruta.Comprobante.ruta,

            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            )

        ) { entrada ->

            val idCompromiso =
                entrada.arguments?.getLong("id") ?: 0L

            ComprobanteScreen(
                idCompromiso = idCompromiso,

                pagosViewModel = pagosViewModel,

                onVolver = {
                    navController.popBackStack()
                },

                onIrInicio = {
                    navController.popBackStack(
                        route = Ruta.Inicio.ruta,
                        inclusive = false
                    )
                }
            )
        }

        composable(Ruta.Historial.ruta) {

            HistorialScreen(
                pagosViewModel = pagosViewModel,

                onVolver = {
                    navController.popBackStack()
                },

                onVerComprobante = { idCompromiso ->

                    navController.navigate(
                        Ruta.Comprobante.crear(idCompromiso)
                    )
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
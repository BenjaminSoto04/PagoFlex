package com.example.pagoflex.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.screens.ComprobanteScreen
import com.example.pagoflex.ui.screens.CompromisosScreen
import com.example.pagoflex.ui.screens.DetalleCompromisoScreen
import com.example.pagoflex.ui.screens.GestionAnulacionCompromisosScreen
import com.example.pagoflex.ui.screens.HistorialScreen
import com.example.pagoflex.ui.screens.InicioScreen
import com.example.pagoflex.ui.screens.LoginScreen
import com.example.pagoflex.ui.screens.PagoScreen
import com.example.pagoflex.ui.screens.AsistenteVirtualScreen
import com.example.pagoflex.ui.screens.BandejaAgenteScreen
import com.example.pagoflex.ui.screens.PanelEjecutivoEmpresaScreen
import com.example.pagoflex.ui.screens.PerfilScreen
import com.example.pagoflex.ui.screens.RecuperarPasswordScreen
import com.example.pagoflex.ui.screens.RegisterScreen
import com.example.pagoflex.ui.screens.ReportarProblemaScreen
import com.example.pagoflex.ui.screens.ResumenRecaudacionReportesScreen
import com.example.pagoflex.viewmodel.AuthViewModel
import com.example.pagoflex.viewmodel.AccountViewModel
import com.example.pagoflex.viewmodel.PagosViewModel
import android.content.Context
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val pagosViewModel: PagosViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()

    val startDestination = Ruta.Login.ruta

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, pagosViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) pagosViewModel.refrescarVencimientos()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
    NavHost(navController = navController, startDestination = startDestination, enterTransition = { slideInHorizontally(animationSpec = tween(300)) + fadeIn() }, exitTransition = { slideOutHorizontally(animationSpec = tween(300)) + fadeOut() }) {
        composable(Ruta.Login.ruta) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = { role ->
                    val dest = when (role) {
                        "EJECUTIVO" -> Ruta.PanelEjecutivo.ruta
                        "AGENTE" -> Ruta.PanelAgente.ruta
                        else -> Ruta.Inicio.ruta
                    }
                    navController.navigate(dest) { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                },
                onIrRegistro = { navController.navigate(Ruta.Register.ruta) },
                onIrRecuperar = { navController.navigate(Ruta.Recuperar.ruta) }
            )
        }
        composable(Ruta.Register.ruta) {
            RegisterScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    val role = authViewModel.currentUser.value?.rol ?: "USUARIO"
                    val dest = when (role) {
                        "EJECUTIVO" -> Ruta.PanelEjecutivo.ruta
                        "AGENTE" -> Ruta.PanelAgente.ruta
                        else -> Ruta.Inicio.ruta
                    }
                    navController.navigate(dest) { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                },
                onVolver = { navController.popBackStack() }
            )
        }
        composable(Ruta.Recuperar.ruta) {
            RecuperarPasswordScreen(accountViewModel = accountViewModel, onVolver = { navController.popBackStack() })
        }
        composable(Ruta.Perfil.ruta) {
            val u = currentUser
                        if (u != null) {
                                PerfilScreen(
                    user = u,
                    accountViewModel = accountViewModel,
                    authViewModel = authViewModel,
                    onVolver = { navController.popBackStack() },
                    onCerrarSesion = {
                                                navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
                        authViewModel.logout()
                        accountViewModel.clear()
                    }
                )
            } else {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            }
        }

        composable(Ruta.Inicio.ruta) {
            val role = SessionManager.role(context)
            if (role != "USUARIO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            } else {
                InicioScreen(
                    pagosViewModel = pagosViewModel,
                    nombreUsuario = currentUser?.nombre,
                    onVerCompromisos = { navController.navigate(Ruta.Compromisos.ruta) },
                    onVerHistorial = { navController.navigate(Ruta.Historial.ruta) },
                    onVerPerfil = { android.util.Log.d("PagoFlexNav", "BandejaAgente route Mi perfil clicked"); navController.navigate(Ruta.Perfil.ruta) },
                    onVerAgente = { navController.navigate(Ruta.PanelAgente.ruta) },
                    onVerReportarProblema = { navController.navigate(Ruta.ReportarProblema.ruta) },
                    onVerAsistente = { navController.navigate(Ruta.AsistenteVirtual.ruta) },
                    onVerPanelEjecutivo = { navController.navigate(Ruta.PanelEjecutivo.ruta) },
                    onVerGestionCompromisos = { navController.navigate(Ruta.GestionCompromisos.ruta) },
                    onVerReportes = { navController.navigate(Ruta.ResumenRecaudacion.ruta) }
                )
            }
        }

        // Guard: only show user screens if role USUARIO; but allow navigation; we won't enforce hard here except via UI.
        composable(Ruta.Compromisos.ruta) {
            val role = SessionManager.role(context)
            if (role != "USUARIO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            } else {
                CompromisosScreen(pagosViewModel = pagosViewModel, onVolver = { navController.popBackStack() }, onVerDetalle = { id -> navController.navigate(Ruta.DetalleCompromiso.crear(id)) })
            }
        }
        composable(route = Ruta.DetalleCompromiso.ruta, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            val role = SessionManager.role(context)
            if (role != "USUARIO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            } else {
                val id = entrada.arguments?.getLong("id") ?: 0L
                DetalleCompromisoScreen(idCompromiso = id, pagosViewModel = pagosViewModel, onVolver = { navController.popBackStack() }, onPagar = { idC -> navController.navigate(Ruta.Pago.crear(idC)) })
            }
        }
        composable(route = Ruta.Pago.ruta, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            val role = SessionManager.role(context)
            if (role != "USUARIO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            } else {
                val idCompromiso = entrada.arguments?.getLong("id") ?: 0L
                PagoScreen(idCompromiso = idCompromiso, pagosViewModel = pagosViewModel, onVolver = { navController.popBackStack() }, onVerComprobante = { id -> navController.navigate(Ruta.Comprobante.crear(id)) })
            }
        }
        composable(route = Ruta.Comprobante.ruta, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entrada ->
            val role = SessionManager.role(context)
            if (role != "USUARIO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            } else {
                val idCompromiso = entrada.arguments?.getLong("id") ?: 0L
                ComprobanteScreen(idCompromiso = idCompromiso, pagosViewModel = pagosViewModel, onVolver = { navController.popBackStack() }, onIrInicio = { navController.popBackStack(route = Ruta.Inicio.ruta, inclusive = false) })
            }
        }
        composable(Ruta.Historial.ruta) {
            val role = SessionManager.role(context)
            if (role != "USUARIO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            } else {
                HistorialScreen(pagosViewModel = pagosViewModel, onVolver = { navController.popBackStack() }, onVerComprobante = { id -> navController.navigate(Ruta.Comprobante.crear(id)) })
            }
        }
        composable(Ruta.ReportarProblema.ruta) {
            val role = SessionManager.role(context)
            if (role != "USUARIO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
            } else {
                ReportarProblemaScreen(onVolver = { navController.popBackStack() })
            }
        }
        composable(Ruta.AsistenteVirtual.ruta) {
            AsistenteVirtualScreen(onVolver = { navController.popBackStack() })
        }
        composable(Ruta.BandejaAgente.ruta) {
            android.util.Log.d("PagoFlexNav", "BandejaAgente route currentUser=${currentUser?.id}")
            BandejaAgenteScreen(
                onVolver = { navController.popBackStack() },
                onCerrarSesion = {
                    navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
                    authViewModel.logout()
                    accountViewModel.clear()
                },
                    onVerPerfil = { android.util.Log.d("PagoFlexNav", "Agent Mi perfil clicked"); navController.navigate(Ruta.Perfil.ruta) }
                )
            }
        composable(Ruta.PanelAgente.ruta) {
            val role = SessionManager.role(context)
            if (role != "AGENTE") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) } }
            } else {
                BandejaAgenteScreen(
                    onVolver = { navController.popBackStack() },
                    onCerrarSesion = {
                                                navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
                        authViewModel.logout()
                        accountViewModel.clear()
                    },
                    onVerPerfil = { try { navController.navigate(Ruta.Perfil.ruta);  } catch (e: Exception) { android.util.Log.e("PagoFlexNav", "BandejaAgente route navigate failed", e) } }
                )
            }
        }
        composable(Ruta.PanelEjecutivo.ruta) {
            val role = SessionManager.role(context)
            if (role != "EJECUTIVO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) } }
            } else {
                PanelEjecutivoEmpresaScreen(
                    user = currentUser,
                    onVolver = { navController.popBackStack() },
                    onCerrarSesion = {
                                                navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
                        authViewModel.logout()
                        accountViewModel.clear()
                    },
                    onVerPerfil = { try { navController.navigate(Ruta.Perfil.ruta);  } catch (e: Exception) { android.util.Log.e("PagoFlexNav", "BandejaAgente route navigate failed", e) } }
                )
            }
        }
        composable(Ruta.ResumenRecaudacion.ruta) {
            val role = SessionManager.role(context)
            if (role != "EJECUTIVO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) } }
            } else {
                ResumenRecaudacionReportesScreen(onVolver = { navController.popBackStack() })
            }
        }
        composable(Ruta.GestionCompromisos.ruta) {
            val role = SessionManager.role(context)
            if (role != "EJECUTIVO") {
                LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) } }
            } else {
                GestionAnulacionCompromisosScreen(onVolver = { navController.popBackStack() })
            }
        }
    }
    }

}

@Composable
fun PantallaTemporal(titulo: String, onVolver: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(text = titulo)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Pantalla en construcción")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onVolver) { Text("Volver al inicio") }
    }
}

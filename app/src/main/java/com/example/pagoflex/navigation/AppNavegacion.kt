package com.example.pagoflex.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.screens.AsistenteVirtualScreen
import com.example.pagoflex.ui.screens.BandejaAgenteScreen
import com.example.pagoflex.ui.screens.BandejaReportesAgenteScreen
import com.example.pagoflex.ui.screens.CompromisosScreen
import com.example.pagoflex.ui.screens.ComprobanteScreen
import com.example.pagoflex.ui.screens.DashboardAgenteScreen
import com.example.pagoflex.ui.screens.DetalleCompromisoScreen
import com.example.pagoflex.ui.screens.DetalleReporteAgenteScreen
import com.example.pagoflex.ui.screens.DetalleReporteUsuarioScreen
import com.example.pagoflex.ui.screens.GestionAnulacionCompromisosScreen
import com.example.pagoflex.ui.screens.GestionRolesScreen
import com.example.pagoflex.ui.screens.HistorialScreen
import com.example.pagoflex.ui.screens.InicioScreen
import com.example.pagoflex.ui.screens.LoginScreen
import com.example.pagoflex.ui.screens.MisReportesScreen
import com.example.pagoflex.ui.screens.NuevoCompromisoScreen
import com.example.pagoflex.ui.screens.PagoScreen
import com.example.pagoflex.ui.screens.PanelEjecutivoEmpresaScreen
import com.example.pagoflex.ui.screens.PerfilScreen
import com.example.pagoflex.ui.screens.RecuperarPasswordScreen
import com.example.pagoflex.ui.screens.RegisterScreen
import com.example.pagoflex.ui.screens.ReportarProblemaScreen
import com.example.pagoflex.ui.screens.RestablecerPasswordScreen
import com.example.pagoflex.ui.screens.ResumenRecaudacionReportesScreen
import com.example.pagoflex.viewmodel.AccountViewModel
import com.example.pagoflex.viewmodel.AuthViewModel
import com.example.pagoflex.viewmodel.PagosViewModel

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
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { slideInHorizontally(animationSpec = tween(300)) + fadeIn() },
            exitTransition = { slideOutHorizontally(animationSpec = tween(300)) + fadeOut() }
        ) {
            composable(Ruta.Login.ruta) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = { role ->
                        val dest = when (role) {
                            "EJECUTIVO" -> Ruta.PanelEjecutivo.ruta
                            "AGENTE" -> Ruta.DashboardAgente.ruta
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
                            "AGENTE" -> Ruta.DashboardAgente.ruta
                            else -> Ruta.Inicio.ruta
                        }
                        navController.navigate(dest) { popUpTo(Ruta.Login.ruta) { inclusive = true } }
                    },
                    onVolver = { navController.popBackStack() }
                )
            }
            composable(Ruta.Recuperar.ruta) {
                RecuperarPasswordScreen(
                    accountViewModel = accountViewModel,
                    authViewModel = authViewModel,
                    onVolver = { navController.popBackStack() },
                    onIrRestablecer = { emailVerificado ->
                        navController.navigate(Ruta.RestablecerPassword.crear(emailVerificado))
                    }
                )
            }
            composable(
                route = Ruta.RestablecerPassword.ruta,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { entrada ->
                val emailArg = Uri.decode(entrada.arguments?.getString("email").orEmpty())
                RestablecerPasswordScreen(
                    email = emailArg,
                    authViewModel = authViewModel,
                    onVolver = { navController.popBackStack() },
                    onPasswordActualizada = {
                        navController.navigate(Ruta.Login.ruta) {
                            popUpTo(Ruta.Login.ruta) { inclusive = true }
                        }
                    }
                )
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
                        onVerPerfil = {
                            android.util.Log.d("PagoFlexNav", "BandejaAgente route Mi perfil clicked")
                            navController.navigate(Ruta.Perfil.ruta)
                        },
                        onVerAgente = { navController.navigate(Ruta.PanelAgente.ruta) },
                        onVerReportarProblema = { navController.navigate(Ruta.ReportarProblema.ruta) },
                        onVerMisReportes = { navController.navigate(Ruta.MisReportes.ruta) },
                        onVerAsistente = { navController.navigate(Ruta.AsistenteVirtual.ruta) },
                        onVerPanelEjecutivo = { navController.navigate(Ruta.PanelEjecutivo.ruta) },
                        onVerGestionCompromisos = { navController.navigate(Ruta.GestionCompromisos.ruta) },
                        onVerReportes = { navController.navigate(Ruta.ResumenRecaudacion.ruta) }
                    )
                }
            }

            composable(Ruta.Compromisos.ruta) {
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    CompromisosScreen(
                        pagosViewModel = pagosViewModel,
                        onVolver = { navController.popBackStack() },
                        onVerDetalle = { id -> navController.navigate(Ruta.DetalleCompromiso.crear(id)) },
                        onNuevoCompromiso = { navController.navigate(Ruta.NuevoCompromiso.ruta) }
                    )
                }
            }
            composable(
                route = Ruta.DetalleCompromiso.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entrada ->
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    val id = entrada.arguments?.getLong("id") ?: 0L
                    DetalleCompromisoScreen(
                        idCompromiso = id,
                        pagosViewModel = pagosViewModel,
                        onVolver = { navController.popBackStack() },
                        onPagar = { idC -> navController.navigate(Ruta.Pago.crear(idC)) }
                    )
                }
            }
            composable(
                route = Ruta.Pago.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entrada ->
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    val idCompromiso = entrada.arguments?.getLong("id") ?: 0L
                    PagoScreen(
                        idCompromiso = idCompromiso,
                        pagosViewModel = pagosViewModel,
                        onVolver = { navController.popBackStack() },
                        onVerComprobante = { id -> navController.navigate(Ruta.Comprobante.crear(id)) }
                    )
                }
            }
            composable(
                route = Ruta.Comprobante.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entrada ->
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    val idCompromiso = entrada.arguments?.getLong("id") ?: 0L
                    ComprobanteScreen(
                        idCompromiso = idCompromiso,
                        pagosViewModel = pagosViewModel,
                        onVolver = { navController.popBackStack() },
                        onIrInicio = { navController.popBackStack(route = Ruta.Inicio.ruta, inclusive = false) }
                    )
                }
            }
            composable(Ruta.Historial.ruta) {
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    HistorialScreen(
                        pagosViewModel = pagosViewModel,
                        onVolver = { navController.popBackStack() },
                        onVerComprobante = { id -> navController.navigate(Ruta.Comprobante.crear(id)) }
                    )
                }
            }
            composable(Ruta.ReportarProblema.ruta) {
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    ReportarProblemaScreen(
                        onVolver = { navController.popBackStack() },
                        onVerMisReportes = { navController.navigate(Ruta.MisReportes.ruta) }
                    )
                }
            }
            composable(Ruta.MisReportes.ruta) {
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    MisReportesScreen(
                        onVolver = { navController.popBackStack() },
                        onVerDetalle = { id -> navController.navigate(Ruta.DetalleReporte.crear(id)) },
                        onCrearNuevo = { navController.navigate(Ruta.ReportarProblema.ruta) }
                    )
                }
            }
            composable(
                route = Ruta.DetalleReporte.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entrada ->
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    val id = entrada.arguments?.getLong("id") ?: 0L
                    DetalleReporteUsuarioScreen(
                        reporteId = id,
                        onVolver = { navController.popBackStack() },
                        onCrearNuevo = { navController.navigate(Ruta.ReportarProblema.ruta) }
                    )
                }
            }
            composable(Ruta.NuevoCompromiso.ruta) {
                val role = SessionManager.role(context)
                if (role != "USUARIO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    NuevoCompromisoScreen(
                        onVolver = { navController.popBackStack() },
                        onGuardado = { navController.popBackStack() }
                    )
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
                    onVerPerfil = {
                        android.util.Log.d("PagoFlexNav", "Agent Mi perfil clicked")
                        navController.navigate(Ruta.Perfil.ruta)
                    }
                )
            }
            composable(Ruta.DashboardAgente.ruta) {
                val role = SessionManager.role(context)
                if (role != "AGENTE") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    DashboardAgenteScreen(
                        onVerBandejaAtencion = { navController.navigate(Ruta.PanelAgente.ruta) },
                        onVerBandejaReportes = { navController.navigate(Ruta.BandejaReportesAgente.ruta) },
                        onVerGestionRoles = { navController.navigate(Ruta.GestionRoles.ruta) },
                        onVerPerfil = { navController.navigate(Ruta.Perfil.ruta) },
                        onCerrarSesion = {
                            navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } }
                            authViewModel.logout()
                            accountViewModel.clear()
                        }
                    )
                }
            }
            composable(Ruta.BandejaReportesAgente.ruta) {
                val role = SessionManager.role(context)
                if (role != "AGENTE") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    BandejaReportesAgenteScreen(
                        onVolver = { navController.popBackStack() },
                        onVerBandejaAtencion = { navController.navigate(Ruta.PanelAgente.ruta) },
                        onVerDetalle = { id -> navController.navigate(Ruta.DetalleReporteAgente.crear(id)) },
                        onVerPerfil = { navController.navigate(Ruta.Perfil.ruta) }
                    )
                }
            }
            composable(
                route = Ruta.DetalleReporteAgente.ruta,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { entrada ->
                val role = SessionManager.role(context)
                if (role != "AGENTE") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    val id = entrada.arguments?.getLong("id") ?: 0L
                    DetalleReporteAgenteScreen(
                        reporteId = id,
                        onVolver = { navController.popBackStack() }
                    )
                }
            }
            composable(Ruta.GestionRoles.ruta) {
                val role = SessionManager.role(context)
                if (role != "AGENTE") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) { inclusive = true } } }
                } else {
                    GestionRolesScreen(onVolver = { navController.popBackStack() })
                }
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
                        onVerPerfil = {
                            try {
                                navController.navigate(Ruta.Perfil.ruta)
                            } catch (e: Exception) {
                                android.util.Log.e("PagoFlexNav", "BandejaAgente route navigate failed", e)
                            }
                        }
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
                        onVerPerfil = {
                            try {
                                navController.navigate(Ruta.Perfil.ruta)
                            } catch (e: Exception) {
                                android.util.Log.e("PagoFlexNav", "BandejaAgente route navigate failed", e)
                            }
                        }
                    )
                }
            }
            composable(Ruta.ResumenRecaudacion.ruta) {
                val role = SessionManager.role(context)
                if (role != "EJECUTIVO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) } }
                } else {
                    ResumenRecaudacionReportesScreen(
                        user = currentUser,
                        onVolver = { navController.popBackStack() }
                    )
                }
            }
            composable(Ruta.GestionCompromisos.ruta) {
                val role = SessionManager.role(context)
                if (role != "EJECUTIVO") {
                    LaunchedEffect(Unit) { navController.navigate(Ruta.Login.ruta) { popUpTo(0) } }
                } else {
                    GestionAnulacionCompromisosScreen(
                        user = currentUser,
                        onVolver = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
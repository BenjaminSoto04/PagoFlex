package com.example.pagoflex.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.viewmodel.GestionUsuariosViewModel

private val rolesDisponibles = listOf("USUARIO", "EJECUTIVO", "AGENTE")

@Composable
fun GestionRolesScreen(
    onVolver: () -> Unit = {},
    vm: GestionUsuariosViewModel = viewModel()
) {
    val actorId = SessionManager.userId(LocalContext.current)
    val usuarios by vm.usuarios.collectAsStateWithLifecycle()
    val mensaje by vm.mensaje.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val procesandoId by vm.procesandoId.collectAsStateWithLifecycle()
    val procesandoEstadoId by vm.procesandoEstadoId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(mensaje) {
        mensaje?.let {
            snackbarHostState.showSnackbar(it)
            vm.limpiarMensaje()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = onVolver) { Text("← Volver al panel") }

            Text(
                text = "Administración de usuarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Habilita o deshabilita cuentas y cambia el rol de las cuentas registradas. Una cuenta deshabilitada no podrá iniciar sesión.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(usuarios, key = { it.id }) { usuario ->
                    TarjetaUsuarioGestion(
                        usuario = usuario,
                        protegido = vm.esProtegido(usuario, actorId),
                        esCuentaPropia = vm.esCuentaPropia(usuario, actorId),
                        procesando = procesandoId == usuario.id,
                        procesandoEstado = procesandoEstadoId == usuario.id,
                        onCambiarRol = { nuevoRol ->
                            vm.cambiarRol(usuario.id, nuevoRol, actorId)
                        },
                        onCambiarEstado = { activo ->
                            vm.cambiarEstadoCuenta(usuario.id, activo, actorId)
                        }
                    )
                }
            }

            BotonSecundario(text = "Volver al panel", onClick = onVolver)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun TarjetaUsuarioGestion(
    usuario: UserEntity,
    protegido: Boolean,
    esCuentaPropia: Boolean,
    procesando: Boolean,
    procesandoEstado: Boolean,
    onCambiarRol: (String) -> Unit,
    onCambiarEstado: (Boolean) -> Unit
) {
    EstilosTarjeta.Tarjeta(
        centrada = false,
        relleno = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = usuario.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = usuario.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                usuario.codigo?.takeIf { it.isNotBlank() }?.let { codigo ->
                    Text(
                        text = "Código: $codigo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = usuario.rol,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (usuario.activo) "Cuenta habilitada" else "Cuenta deshabilitada",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (usuario.activo) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                Text(
                    text = when {
                        esCuentaPropia -> "Es tu cuenta activa: no puedes deshabilitarla."
                        procesandoEstado -> "Actualizando estado..."
                        usuario.activo -> "El usuario puede iniciar sesión."
                        else -> "El usuario no podrá iniciar sesión."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = usuario.activo,
                onCheckedChange = onCambiarEstado,
                enabled = !esCuentaPropia && !procesandoEstado
            )
        }

        HorizontalDivider()

        when {
            protegido -> Text(
                text = "Cuenta protegida. Su rol no puede modificarse desde aquí.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            procesando -> Text(
                text = "Actualizando rol...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            else -> {
                EstiloCampo.CampoSelector(
                    label = "Rol",
                    opciones = rolesDisponibles,
                    textoOpcion = { it },
                    seleccionado = usuario.rol,
                    onSeleccionar = onCambiarRol
                )
                Text(
                    text = "Si pasa a EJECUTIVO se asigna una empresa por defecto; si vuelve a USUARIO se asigna un agente de atención.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

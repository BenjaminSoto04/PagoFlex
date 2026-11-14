package com.example.pagoflex.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.FileProvider
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.io.File
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.ui.styles.BotonPrimario
import com.example.pagoflex.ui.styles.BotonSecundario
import com.example.pagoflex.ui.styles.EstiloCampo
import com.example.pagoflex.ui.styles.EstilosTarjeta
import com.example.pagoflex.viewmodel.AccountViewModel

@Composable
fun PerfilScreen(
    user: UserEntity,
    accountViewModel: AccountViewModel = viewModel(),
    authViewModel: com.example.pagoflex.viewmodel.AuthViewModel = viewModel(),
    onVolver: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    android.util.Log.d("PagoFlexNav", "PerfilScreen composing user=${user.id} route=...")
    val message by accountViewModel.message.collectAsStateWithLifecycle()
    val errorGeneral by accountViewModel.error.collectAsStateWithLifecycle()
    val errorNombre by accountViewModel.errorNombre.collectAsStateWithLifecycle()
    val errorTelefono by accountViewModel.errorTelefono.collectAsStateWithLifecycle()
    val errorEmail by accountViewModel.errorEmail.collectAsStateWithLifecycle()
    val errorOld by accountViewModel.errorOld.collectAsStateWithLifecycle()
    val errorNew by accountViewModel.errorNew.collectAsStateWithLifecycle()
    val errorConfirm by accountViewModel.errorConfirm.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var nombre by remember(user) { mutableStateOf(user.nombre) }
    var telefono by remember(user) { mutableStateOf(user.telefono ?: "") }
    var email by remember(user) { mutableStateOf(user.email) }
    val esUsuario = user.rol == "USUARIO"
    var nombreEmpresa by remember(user) { mutableStateOf(user.empresa) }
    val perfilContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(user.empresaId) {
        if (!esUsuario && nombreEmpresa.isNullOrBlank() && user.empresaId != null) {
            nombreEmpresa = com.example.pagoflex.data.repository.RepositoryProvider.database(perfilContext).empresaDao().getById(user.empresaId)?.nombreFantasia
        }
    }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showOld by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!)
            authViewModel.refreshUser()
            accountViewModel.clear()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    var showPhotoDialog by remember { mutableStateOf(false) }
    val photoFile = remember(user) { File(context.filesDir, "profile_${user.id}.jpg") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            accountViewModel.updateFoto(user.id, photoFile.absolutePath)
            authViewModel.refreshUser()
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { input ->
                photoFile.outputStream().use { output -> input.copyTo(output) }
            }
            accountViewModel.updateFoto(user.id, photoFile.absolutePath)
            authViewModel.refreshUser()
        }
    }

    var photoError by remember { mutableStateOf<String?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            showPhotoDialog = false
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            cameraLauncher.launch(uri)
        } else {
            photoError = "Permiso de cámara denegado"
            showPhotoDialog = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(onClick = onVolver) { Text("← Volver") }
        Box(Modifier.size(120.dp).clip(androidx.compose.foundation.shape.CircleShape).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
            if (photoFile.exists()) {
                val bmp = remember(photoFile) { BitmapFactory.decodeFile(photoFile.absolutePath) }
                if (bmp != null) Image(bitmap = bmp.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text(user.nombre.take(1).uppercase())
            } else {
                Text(user.nombre.take(1).uppercase(), style = MaterialTheme.typography.headlineLarge)
            }
        }
        EstilosTarjeta.Tarjeta(centrada = true) {
            Text("Mi perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Nombre: ${user.nombre}", style = MaterialTheme.typography.bodyLarge)
            Text("Correo: ${user.email}", style = MaterialTheme.typography.bodyLarge)
            Text("Rol: ${user.rol}", style = MaterialTheme.typography.bodyLarge)
            user.telefono?.let { Text("Teléfono: $it", style = MaterialTheme.typography.bodyLarge) }
            if (!esUsuario) nombreEmpresa?.let { Text("Empresa: $it", style = MaterialTheme.typography.bodyLarge) }
        }

        BotonPrimario(text = "Cambiar foto de perfil", onClick = { showPhotoDialog = true })
        photoError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider()
        Text("Editar perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        EstiloCampo.CampoTexto(
            value = nombre,
            onValueChange = { nombre = it; accountViewModel.limpiarErrorNombre(); accountViewModel.limpiarErrorGeneral() },
            label = "Nombre completo",
            isError = errorNombre != null
        )
        EstiloCampo.MensajeError(errorNombre)
        EstiloCampo.CampoTexto(
            value = telefono,
            onValueChange = { telefono = it; accountViewModel.limpiarErrorTelefono(); accountViewModel.limpiarErrorGeneral() },
            label = "Teléfono (opcional)",
            isError = errorTelefono != null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
            )
        )
        EstiloCampo.MensajeError(errorTelefono)
        EstiloCampo.CampoTexto(
            value = email,
            onValueChange = { email = it; accountViewModel.limpiarErrorEmail(); accountViewModel.limpiarErrorGeneral() },
            label = "Correo electrónico",
            isError = errorEmail != null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            )
        )
        EstiloCampo.MensajeError(errorEmail)
        EstiloCampo.MensajeError(errorGeneral)
        BotonPrimario(
            text = "Guardar perfil",
            onClick = {
                accountViewModel.updateProfile(
                    user,
                    nombre,
                    telefono.ifBlank { null },
                    email,
                    user.empresa
                )
            }
        )

        HorizontalDivider()
        BotonSecundario(text = "Cambiar contraseña", onClick = { showPasswordDialog = true })

        HorizontalDivider()
        BotonSecundario(text = "Cerrar sesión", onClick = { showLogoutDialog = true })
    }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            modifier = Modifier.imePadding(),
            title = { Text("Cambiar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EstiloCampo.CampoContrasena(
                        value = oldPass,
                        onValueChange = { oldPass = it; accountViewModel.limpiarErroresContrasena() },
                        label = "Contraseña actual",
                        visible = showOld,
                        onToggleVisible = { showOld = !showOld },
                        isError = errorOld != null
                    )
                    EstiloCampo.MensajeError(errorOld)

                    EstiloCampo.CampoContrasena(
                        value = newPass,
                        onValueChange = { newPass = it; accountViewModel.limpiarErroresContrasena() },
                        label = "Nueva contraseña",
                        visible = showNew,
                        onToggleVisible = { showNew = !showNew },
                        isError = errorNew != null
                    )
                    EstiloCampo.MensajeError(errorNew)

                    EstiloCampo.CampoContrasena(
                        value = confirm,
                        onValueChange = { confirm = it; accountViewModel.limpiarErroresContrasena() },
                        label = "Confirmar nueva contraseña",
                        visible = showConfirm,
                        onToggleVisible = { showConfirm = !showConfirm },
                        isError = errorConfirm != null
                    )
                    EstiloCampo.MensajeError(errorConfirm)

                    EstiloCampo.MensajeError(errorGeneral)
                }
            },
            confirmButton = {
                BotonPrimario(
                    text = "Guardar cambios",
                    onClick = { accountViewModel.changePassword(user.id, oldPass, newPass, confirm) },
                    anchoCompleto = false
                )
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Close dialog on success
    LaunchedEffect(message) {
        if (message != null && showPasswordDialog) {
            showPasswordDialog = false
            oldPass = ""; newPass = ""; confirm = ""
        }
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Cambiar foto de perfil") },
            text = { Text("Selecciona una opción") },
            confirmButton = {
                BotonPrimario(
                    text = "Tomar foto",
                    onClick = {
                        showPhotoDialog = false
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    anchoCompleto = false
                )
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false; galleryLauncher.launch("image/*") }) { Text("Elegir de galeria") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("¿Deseas cerrar la sesión actual?") },
            confirmButton = {
                BotonPrimario(
                    text = "Cerrar sesión",
                    onClick = {
                        android.util.Log.d("PagoFlexLogout", "logout confirm")
                        showLogoutDialog = false
                        onCerrarSesion()
                    },
                    anchoCompleto = false
                )
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

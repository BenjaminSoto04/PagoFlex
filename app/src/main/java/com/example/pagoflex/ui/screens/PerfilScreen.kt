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
    val errorOld by accountViewModel.errorOld.collectAsStateWithLifecycle()
    val errorNew by accountViewModel.errorNew.collectAsStateWithLifecycle()
    val errorConfirm by accountViewModel.errorConfirm.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var nombre by remember(user) { mutableStateOf(user.nombre) }
    var telefono by remember(user) { mutableStateOf(user.telefono ?: "") }
    var empresa by remember(user) { mutableStateOf(user.empresa ?: "") }

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
            photoError = "Permiso de camara denegado"
            showPhotoDialog = false
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Text("Mi perfil", style = MaterialTheme.typography.titleLarge)
            Text("Nombre: ${user.nombre}", style = MaterialTheme.typography.bodyLarge)
            Text("Correo: ${user.email}", style = MaterialTheme.typography.bodyLarge)
            Text("Rol: ${user.rol}", style = MaterialTheme.typography.bodyLarge)
            user.telefono?.let { Text("Telefono: $it", style = MaterialTheme.typography.bodyLarge) }
            user.empresa?.let { Text("Empresa: $it", style = MaterialTheme.typography.bodyLarge) }

        Button(onClick = { showPhotoDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Cambiar foto de perfil") }
        photoError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        HorizontalDivider()
        Text("Editar perfil", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Telefono") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = empresa, onValueChange = { empresa = it }, label = { Text("Empresa") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { accountViewModel.updateProfile(user, nombre, telefono.ifBlank { null }, empresa.ifBlank { null }) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar perfil") }

            HorizontalDivider()
            Button(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Cambiar contrasena") }

            HorizontalDivider()
            Button(onClick = { showLogoutDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesion") }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Cambiar contrasena") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text("Contrasena actual") },
                        visualTransformation = if (showOld) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showOld = !showOld }) {
                                Text(if (showOld) "Ocultar" else "Ver")
                            }
                        },
                        isError = errorOld != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorOld?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Nueva contrasena") },
                        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showNew = !showNew }) {
                                Text(if (showNew) "Ocultar" else "Ver")
                            }
                        },
                        isError = errorNew != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorNew?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirmar nueva contrasena") },
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showConfirm = !showConfirm }) {
                                Text(if (showConfirm) "Ocultar" else "Ver")
                            }
                        },
                        isError = errorConfirm != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorConfirm?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                Button(onClick = { accountViewModel.changePassword(user.id, oldPass, newPass, confirm) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Guardar cambios")
                }
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
            text = { Text("Selecciona una opcion") },
            confirmButton = {
                Button(onClick = {
                    showPhotoDialog = false
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                }) { Text("Tomar foto") }
            },
            dismissButton = {
                TextButton(onClick = { showPhotoDialog = false; galleryLauncher.launch("image/*") }) { Text("Elegir de galeria") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesion") },
            text = { Text("¿Desea cerrar la sesion actual?") },
            confirmButton = {
                Button(onClick = { android.util.Log.d("PagoFlexLogout", "logout confirm"); showLogoutDialog = false; onCerrarSesion() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Cerrar sesion")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

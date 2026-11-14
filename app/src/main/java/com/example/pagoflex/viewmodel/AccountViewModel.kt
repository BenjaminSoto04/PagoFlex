package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import com.example.pagoflex.ui.styles.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: UserRepository = RepositoryProvider.users(application)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Error general para fallas que no corresponden a un campo puntual. */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _errorNombre = MutableStateFlow<String?>(null)
    val errorNombre: StateFlow<String?> = _errorNombre.asStateFlow()

    private val _errorTelefono = MutableStateFlow<String?>(null)
    val errorTelefono: StateFlow<String?> = _errorTelefono.asStateFlow()

    private val _errorEmail = MutableStateFlow<String?>(null)
    val errorEmail: StateFlow<String?> = _errorEmail.asStateFlow()

    private val _errorOld = MutableStateFlow<String?>(null)
    val errorOld: StateFlow<String?> = _errorOld.asStateFlow()

    private val _errorNew = MutableStateFlow<String?>(null)
    val errorNew: StateFlow<String?> = _errorNew.asStateFlow()

    private val _errorConfirm = MutableStateFlow<String?>(null)
    val errorConfirm: StateFlow<String?> = _errorConfirm.asStateFlow()

    fun updateProfile(user: UserEntity, nombre: String, telefono: String?, email: String, empresa: String?) {
        _errorNombre.value = Validaciones.nombre(nombre)
        _errorTelefono.value = Validaciones.telefono(telefono.orEmpty())
        _errorEmail.value = Validaciones.correo(email)
        _error.value = null
        if (_errorNombre.value != null || _errorTelefono.value != null || _errorEmail.value != null) return

        viewModelScope.launch {
            val correoNormalizado = email.trim().lowercase()
            val existente = repo.getByEmail(correoNormalizado)
            if (existente != null && existente.id != user.id) {
                _errorEmail.value = "El correo ya está registrado"
                return@launch
            }
            runCatching {
                repo.updateProfile(user.copy(nombre = nombre.trim(), telefono = telefono, email = correoNormalizado, empresa = empresa))
            }.onSuccess {
                _message.value = "Perfil actualizado"
            }.onFailure {
                _error.value = "No pudimos guardar los cambios. Inténtalo nuevamente."
            }
        }
    }

    fun changePassword(userId: Long, old: String, newPass: String, confirm: String) {
        limpiarErroresContrasena()
        _error.value = null

        _errorOld.value = if (old.isEmpty()) "Ingresa tu contraseña actual" else null
        _errorNew.value = Validaciones.contrasena(newPass)
        if (_errorNew.value == null && newPass == old) {
            _errorNew.value = "La nueva contraseña debe ser distinta de la actual"
        }
        _errorConfirm.value = Validaciones.confirmacion(newPass, confirm)

        val hayErrores = listOf(_errorOld.value, _errorNew.value, _errorConfirm.value).any { it != null }
        if (hayErrores) return

        viewModelScope.launch {
            repo.changePassword(userId, old, newPass).onSuccess {
                _message.value = "Contraseña actualizada correctamente"
            }.onFailure { e ->
                val mensaje = e.message.orEmpty()
                if (mensaje.contains("contraseña", ignoreCase = true) ||
                    mensaje.contains("usuario", ignoreCase = true)
                ) {
                    _errorOld.value = "La contraseña actual es incorrecta"
                } else {
                    _error.value = mensaje.ifBlank { "No pudimos cambiar la contraseña. Inténtalo nuevamente." }
                }
            }
        }
    }

    fun recoverPassword(email: String, newPass: String, confirm: String) {
        _errorEmail.value = Validaciones.correo(email)
        _errorNew.value = Validaciones.contrasena(newPass)
        _errorConfirm.value = Validaciones.confirmacion(newPass, confirm)
        _error.value = null
        _message.value = null

        val hayErrores = listOf(_errorEmail.value, _errorNew.value, _errorConfirm.value).any { it != null }
        if (hayErrores) return

        viewModelScope.launch {
            repo.resetPassword(email.trim(), newPass).onSuccess {
                _message.value = "Contraseña restablecida correctamente"
            }.onFailure { e ->
                val mensaje = e.message.orEmpty()
                if (mensaje.contains("correo", ignoreCase = true)) {
                    _errorEmail.value = "No existe una cuenta registrada con este correo"
                } else {
                    _error.value = mensaje.ifBlank { "No pudimos restablecer la contraseña. Inténtalo nuevamente." }
                }
            }
        }
    }

    fun updateFoto(userId: Long, fotoUri: String?) {
        viewModelScope.launch {
            repo.updateFoto(userId, fotoUri)
            _message.value = "Foto actualizada"
        }
    }

    fun limpiarErrorNombre() { _errorNombre.value = null }

    fun limpiarErrorTelefono() { _errorTelefono.value = null }

    fun limpiarErrorEmail() { _errorEmail.value = null }

    fun limpiarErrorGeneral() { _error.value = null }

    fun limpiarErroresContrasena() {
        _errorOld.value = null
        _errorNew.value = null
        _errorConfirm.value = null
    }

    fun clear() {
        _message.value = null
        _error.value = null
        _errorNombre.value = null
        _errorTelefono.value = null
        _errorEmail.value = null
        limpiarErroresContrasena()
    }
}

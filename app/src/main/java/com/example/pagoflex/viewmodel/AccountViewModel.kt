package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import com.example.pagoflex.security.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: UserRepository = RepositoryProvider.users(application)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _errorOld = MutableStateFlow<String?>(null)
    val errorOld: StateFlow<String?> = _errorOld.asStateFlow()

    private val _errorNew = MutableStateFlow<String?>(null)
    val errorNew: StateFlow<String?> = _errorNew.asStateFlow()

    private val _errorConfirm = MutableStateFlow<String?>(null)
    val errorConfirm: StateFlow<String?> = _errorConfirm.asStateFlow()

    fun updateProfile(user: UserEntity, nombre: String, telefono: String?, empresa: String?) {
        if (nombre.isBlank()) { _error.value = "Nombre obligatorio"; return }
        viewModelScope.launch {
            repo.updateProfile(user.copy(nombre = nombre.trim(), telefono = telefono, empresa = empresa))
            _message.value = "Perfil actualizado"
        }
    }

    fun changePassword(userId: Long, old: String, newPass: String, confirm: String) {
        _errorOld.value = null
        _errorNew.value = null
        _errorConfirm.value = null
        _error.value = null

        var hasError = false
        if (old.isBlank()) {
            _errorOld.value = "La contrasena actual es obligatoria"
            hasError = true
        }
        if (newPass.isBlank()) {
            _errorNew.value = "La nueva contrasena es obligatoria"
            hasError = true
        } else {
            val errs = PasswordHasher.validate(newPass)
            if (errs.isNotEmpty()) {
                _errorNew.value = errs.first()
                hasError = true
            } else if (newPass == old) {
                _errorNew.value = "La nueva contrasena debe ser diferente de la actual"
                hasError = true
            }
        }
        if (confirm.isBlank()) {
            _errorConfirm.value = "La confirmacion es obligatoria"
            hasError = true
        } else if (newPass != confirm) {
            _errorConfirm.value = "Las contrasenas no coinciden"
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            repo.changePassword(userId, old, newPass).onSuccess {
                _message.value = "Contrasena actualizada correctamente"
            }.onFailure { e ->
                _errorOld.value = e.message ?: "Contrasena actual incorrecta"
            }
        }
    }

    fun recoverPassword(email: String, newPass: String, confirm: String) {
        val errs = PasswordHasher.validate(newPass)
        if (errs.isNotEmpty()) { _error.value = errs.first(); return }
        if (newPass != confirm) { _error.value = "Las contrasenas no coinciden"; return }
        viewModelScope.launch {
            repo.resetPassword(email.trim(), newPass).onSuccess { _message.value = "Contrasena restablecida" }
                .onFailure { _error.value = it.message }
        }
    }

    fun updateFoto(userId: Long, fotoUri: String?) {
        viewModelScope.launch {
            repo.updateFoto(userId, fotoUri)
            _message.value = "Foto actualizada"
        }
    }

    fun clear() { _message.value = null; _error.value = null; _errorOld.value = null; _errorNew.value = null; _errorConfirm.value = null }
}

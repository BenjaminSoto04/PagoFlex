package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.security.PasswordHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: UserRepository = RepositoryProvider.users(application)

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Do not auto-restore session. Login must be explicit.

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Completa todos los campos"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            repo.login(email.trim().lowercase(), password).onSuccess { user ->
                _currentUser.value = user
                _error.value = null
                SessionManager.save(getApplication(), user.id, user.rol)
            }.onFailure { e ->
                _error.value = e.message
            }
            _loading.value = false
        }
    }

    fun register(nombre: String, email: String, password: String, rol: String, telefono: String? = null, empresa: String? = null) {
        val errs = PasswordHasher.validate(password)
        if (errs.isNotEmpty()) {
            _error.value = errs.first()
            return
        }
        if (nombre.isBlank() || email.isBlank()) {
            _error.value = "Completa todos los campos"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            repo.register(nombre.trim(), email.trim(), password, rol, telefono, empresa).onSuccess { id ->
                val user = repo.getById(id)
                _currentUser.value = user
                if (user != null) SessionManager.save(getApplication(), user.id, user.rol)
                _error.value = null
            }.onFailure { e ->
                _error.value = e.message
            }
            _loading.value = false
        }
    }

    fun logout() {
        _currentUser.value = null
        SessionManager.clear(getApplication())
    }

    fun seleccionarSesion(user: UserEntity) {
        _currentUser.value = user
        SessionManager.save(getApplication(), user.id, user.rol)
        _error.value = null
    }

    fun refreshUser() {
        val id = SessionManager.userId(getApplication())
        if (id > 0) viewModelScope.launch { _currentUser.value = repo.getById(id) }
    }

    fun clearError() { _error.value = null }
}

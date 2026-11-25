package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.EmpresaRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Administracion de usuarios del agente. Los tres usuarios demo principales y
 * la propia cuenta del agente quedan protegidos para cambiar de rol, pero el
 * estado habilitado/deshabilitado solo se bloquea para la cuenta con la sesion
 * activa (para no dejar al administrador sin acceso).
 */
class GestionUsuariosViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepo: UserRepository = RepositoryProvider.users(application)
    private val empresaRepo: EmpresaRepository = RepositoryProvider.empresas(application)

    private val _usuarios = MutableStateFlow<List<UserEntity>>(emptyList())
    val usuarios: StateFlow<List<UserEntity>> = _usuarios.asStateFlow()

    private val _empresas = MutableStateFlow<List<EmpresaEntity>>(emptyList())
    val empresas: StateFlow<List<EmpresaEntity>> = _empresas.asStateFlow()

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _procesandoId = MutableStateFlow<Long?>(null)
    val procesandoId: StateFlow<Long?> = _procesandoId.asStateFlow()

    private val _procesandoEstadoId = MutableStateFlow<Long?>(null)
    val procesandoEstadoId: StateFlow<Long?> = _procesandoEstadoId.asStateFlow()

    private val correosProtegidos = setOf(
        "usuario.prueba@pagoflex.cl",
        "ejecutivo.prueba@pagoflex.cl",
        "agente.prueba@pagoflex.cl"
    )

    init {
        viewModelScope.launch {
            userRepo.getAll().collect { usuarios ->
                _usuarios.value = usuarios.sortedByDescending { it.id }
            }
        }
        viewModelScope.launch {
            empresaRepo.getAll().collect { empresas ->
                _empresas.value = empresas.sortedBy { it.nombreFantasia }
            }
        }
    }

    fun esProtegido(user: UserEntity, actorId: Long): Boolean =
        user.id == actorId || user.email.lowercase() in correosProtegidos

    /** La cuenta con la sesion activa no puede deshabilitarse a si misma. */
    fun esCuentaPropia(user: UserEntity, actorId: Long): Boolean = user.id == actorId

    fun cambiarRol(userId: Long, nuevoRol: String, actorId: Long) {
        _mensaje.value = null
        _error.value = null
        val usuario = _usuarios.value.find { it.id == userId }
        if (usuario == null) {
            _error.value = "No encontramos el usuario seleccionado."
            return
        }
        if (esProtegido(usuario, actorId)) {
            _error.value = "Esta cuenta está protegida y no puede cambiar de rol."
            return
        }
        if (usuario.rol == nuevoRol) return

        viewModelScope.launch {
            _procesandoId.value = userId
            userRepo.updateRol(userId, nuevoRol)
                .onSuccess {
                    _mensaje.value = "Rol de ${usuario.nombre} actualizado a $nuevoRol."
                }
                .onFailure {
                    _error.value = it.message ?: "No pudimos actualizar el rol."
                }
            _procesandoId.value = null
        }
    }

    /**
     * Asigna EJECUTIVO y la empresa elegida en un solo UPDATE de Room. Se usa
     * desde el desplegable "Empresa asignada" de GestionRolesScreen.
     */
    fun cambiarRolConEmpresa(userId: Long, empresaId: Long, actorId: Long) {
        _mensaje.value = null
        _error.value = null
        val usuario = _usuarios.value.find { it.id == userId }
        if (usuario == null) {
            _error.value = "No encontramos el usuario seleccionado."
            return
        }
        if (esProtegido(usuario, actorId)) {
            _error.value = "Esta cuenta está protegida y no puede cambiar de rol."
            return
        }
        val empresa = _empresas.value.find { it.id == empresaId }
        if (empresa == null) {
            _error.value = "Selecciona una empresa válida para el ejecutivo."
            return
        }
        if (usuario.rol == "EJECUTIVO" &&
            usuario.empresaId == empresa.id &&
            usuario.empresa == empresa.nombreFantasia
        ) {
            return
        }

        viewModelScope.launch {
            _procesandoId.value = userId
            userRepo.updateRolConEmpresa(userId, empresa.id)
                .onSuccess {
                    _mensaje.value = "Rol de ${usuario.nombre} actualizado a EJECUTIVO. " +
                            "Empresa asignada: ${empresa.nombreFantasia}."
                }
                .onFailure {
                    _error.value = it.message ?: "No pudimos asignar la empresa al ejecutivo."
                }
            _procesandoId.value = null
        }
    }

    /**
     * Habilita o deshabilita una cuenta en Room. Cualquier cuenta puede
     * cambiarse salvo la del agente que tiene la sesion iniciada.
     */
    fun cambiarEstadoCuenta(userId: Long, activo: Boolean, actorId: Long) {
        _mensaje.value = null
        _error.value = null
        val usuario = _usuarios.value.find { it.id == userId }
        if (usuario == null) {
            _error.value = "No encontramos el usuario seleccionado."
            return
        }
        if (usuario.id == actorId) {
            _error.value = "No puedes deshabilitar la cuenta con la que tienes la sesión iniciada."
            return
        }
        if (usuario.activo == activo) return

        viewModelScope.launch {
            _procesandoEstadoId.value = userId
            userRepo.updateActivo(userId, activo)
                .onSuccess {
                    _mensaje.value = if (activo) {
                        "Cuenta de ${usuario.nombre} habilitada."
                    } else {
                        "Cuenta de ${usuario.nombre} deshabilitada. No podrá iniciar sesión."
                    }
                }
                .onFailure {
                    _error.value = it.message ?: "No pudimos actualizar el estado de la cuenta."
                }
            _procesandoEstadoId.value = null
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    fun limpiarMensajes() {
        _mensaje.value = null
        _error.value = null
    }
}

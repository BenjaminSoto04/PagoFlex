package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import com.example.pagoflex.data.repository.ChatRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: ChatRepository = RepositoryProvider.chat(application)
    private val userRepo: UserRepository = RepositoryProvider.users(application)

    private val mapaVacio = listOf(0L to "").drop(1).toMap()
    private val _nombresUsuarios = MutableStateFlow(mapaVacio)
    val nombresUsuarios = _nombresUsuarios.asStateFlow()

    private val _conversacionId = MutableStateFlow<Long?>(null)
    val conversacionId: StateFlow<Long?> = _conversacionId.asStateFlow()

    private val _conversacionActual = MutableStateFlow<ConversacionEntity?>(null)
    val conversacionActual: StateFlow<ConversacionEntity?> = _conversacionActual.asStateFlow()

    private val _mensajes = MutableStateFlow<List<MensajeEntity>>(emptyList())
    val mensajes: StateFlow<List<MensajeEntity>> = _mensajes.asStateFlow()

    private val _conversaciones = MutableStateFlow<List<ConversacionEntity>>(emptyList())
    val conversaciones: StateFlow<List<ConversacionEntity>> = _conversaciones.asStateFlow()

    private val fmt = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

    private var usuarioConvJob: Job? = null
    private var conversacionDetalleJob: Job? = null
    private var mensajesJob: Job? = null
    private var bandejaConvsJob: Job? = null
    private var usuariosMapJob: Job? = null

    fun initUsuario(userId: Long) {
        usuarioConvJob?.cancel()
        usuarioConvJob = viewModelScope.launch {
            repo.observeConversacionByUsuario(userId).collect { conv ->
                _conversacionActual.value = conv
                if (conv != null) {
                    if (_conversacionId.value != conv.id || mensajesJob == null) {
                        _conversacionId.value = conv.id
                        observarMensajes(conv.id)
                    }
                } else {
                    _conversacionId.value = null
                    mensajesJob?.cancel()
                    _mensajes.value = emptyList()
                }
            }
        }
    }

    private fun observarMensajes(convId: Long) {
        mensajesJob?.cancel()
        mensajesJob = viewModelScope.launch {
            repo.getMensajes(convId).collect { lista ->
                _mensajes.value = lista
            }
        }
    }

    fun enviarUsuario(userId: Long, texto: String) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            val actual = repo.getConversacionByUsuario(userId)
            if (actual != null && actual.estado.equals("FINALIZADO", ignoreCase = true)) {
                return@launch
            }

            var id = actual?.id ?: _conversacionId.value
            if (id == null) {
                id = repo.getOrCreateConversacion(userId, LocalDateTime.now().format(fmt))
                _conversacionId.value = id
                observarMensajes(id)
            }

            repo.enviarMensaje(
                convId = id,
                remitenteId = userId,
                rol = "USUARIO",
                contenido = texto.trim(),
                fecha = LocalDateTime.now().format(fmt)
            )
            if (repo.countSistema(id) == 0) {
                repo.enviarMensaje(
                    convId = id,
                    remitenteId = 0L,
                    rol = "SISTEMA",
                    contenido = "Los agentes están ocupados en este momento. Por favor, espera en línea y un agente se comunicará contigo.",
                    fecha = LocalDateTime.now().format(fmt)
                )
            }
        }
    }

    fun initAgente() {
        bandejaConvsJob?.cancel()
        bandejaConvsJob = viewModelScope.launch {
            repo.getAllConversaciones().collect { _conversaciones.value = it }
        }
        usuariosMapJob?.cancel()
        usuariosMapJob = viewModelScope.launch {
            userRepo.getAll().collect { usuarios ->
                _nombresUsuarios.value = usuarios.associate { it.id to it.nombre }
            }
        }
    }

    fun abrirConversacion(convId: Long) {
        _conversacionId.value = convId
        conversacionDetalleJob?.cancel()
        conversacionDetalleJob = viewModelScope.launch {
            repo.observeConversacionById(convId).collect { conv ->
                _conversacionActual.value = conv
            }
        }
        observarMensajes(convId)
    }

    fun cerrarConversacion() {
        conversacionDetalleJob?.cancel()
        mensajesJob?.cancel()
        _conversacionId.value = null
        _conversacionActual.value = null
        _mensajes.value = emptyList()
    }

    fun responderAgente(agenteId: Long, texto: String) {
        val id = _conversacionId.value ?: return
        if (texto.isBlank()) return
        viewModelScope.launch {
            val conv = repo.getConversacionById(id)
            if (conv == null || conv.estado.equals("FINALIZADO", ignoreCase = true)) {
                return@launch
            }
            repo.enviarMensaje(
                convId = id,
                remitenteId = agenteId,
                rol = "AGENTE",
                contenido = texto.trim(),
                fecha = LocalDateTime.now().format(fmt)
            )
        }
    }

    fun finalizarConversacion(convId: Long? = _conversacionId.value) {
        val id = convId ?: return
        viewModelScope.launch {
            repo.cambiarEstadoConversacion(id, "FINALIZADO")
        }
    }
}
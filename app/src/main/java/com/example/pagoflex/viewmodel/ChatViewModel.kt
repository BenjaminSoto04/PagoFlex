package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.repository.ChatRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import com.example.pagoflex.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: ChatRepository = RepositoryProvider.chat(application)

    private val _conversacionId = MutableStateFlow<Long?>(null)
    val conversacionId: StateFlow<Long?> = _conversacionId.asStateFlow()

    private val _mensajes = MutableStateFlow<List<MensajeEntity>>(emptyList())
    val mensajes: StateFlow<List<MensajeEntity>> = _mensajes.asStateFlow()

    private val _conversaciones = MutableStateFlow<List<ConversacionEntity>>(emptyList())
    val conversaciones: StateFlow<List<ConversacionEntity>> = _conversaciones.asStateFlow()

    private val fmt = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

    fun initUsuario(userId: Long) {
        viewModelScope.launch {
            val existing = repo.getConversacionByUsuario(userId)
            if (existing != null) {
                _conversacionId.value = existing.id
                repo.getMensajes(existing.id).collect { _mensajes.value = it }
            } else {
                _conversacionId.value = null
                _mensajes.value = emptyList()
            }
        }
    }

    fun enviarUsuario(userId: Long, texto: String) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            var id = _conversacionId.value
            if (id == null) {
                id = repo.getOrCreateConversacion(userId, LocalDateTime.now().format(fmt))
                _conversacionId.value = id
                viewModelScope.launch { repo.getMensajes(id).collect { _mensajes.value = it } }
            }
            repo.enviarMensaje(id!!, userId, "USUARIO", texto.trim(), LocalDateTime.now().format(fmt))
            if (repo.countSistema(id!!) == 0) {
                repo.enviarMensaje(id!!, 0L, "SISTEMA", "Los agentes estan ocupados en este momento. Por favor, espera en linea y un agente se comunicara contigo.", LocalDateTime.now().format(fmt))
            }
        }
    }

    fun initAgente() {
        viewModelScope.launch {
            repo.getAllConversaciones().collect { _conversaciones.value = it }
        }
    }

    fun abrirConversacion(convId: Long) {
        viewModelScope.launch {
            _conversacionId.value = convId
            repo.getMensajes(convId).collect { _mensajes.value = it }
        }
    }

    fun cerrarConversacion() { _conversacionId.value = null }

    fun responderAgente(agenteId: Long, texto: String) {
        val id = _conversacionId.value ?: return
        if (texto.isBlank()) return
        viewModelScope.launch {
            repo.enviarMensaje(id, agenteId, "AGENTE", texto.trim(), LocalDateTime.now().format(fmt))
        }
    }
}
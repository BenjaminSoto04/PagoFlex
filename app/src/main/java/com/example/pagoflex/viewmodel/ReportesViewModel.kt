package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.data.repository.ReporteMensaje
import com.example.pagoflex.data.repository.ReporteRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Datos compartidos por las pantallas de reportes del usuario y del agente:
 * lista de reportes, detalle, hilo de mensajes y cambio de estado.
 */
class ReportesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: ReporteRepository = RepositoryProvider.reportes(application)
    private val userRepo: UserRepository = RepositoryProvider.users(application)

    private val formatoFechaHora = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

    private val _reportes = MutableStateFlow<List<Reporte>>(emptyList())
    val reportes: StateFlow<List<Reporte>> = _reportes.asStateFlow()

    private val _nombresUsuarios = MutableStateFlow<Map<Long, String>>(emptyMap())
    val nombresUsuarios: StateFlow<Map<Long, String>> = _nombresUsuarios.asStateFlow()

    private val _reporteActual = MutableStateFlow<Reporte?>(null)
    val reporteActual: StateFlow<Reporte?> = _reporteActual.asStateFlow()

    private val _mensajes = MutableStateFlow<List<ReporteMensaje>>(emptyList())
    val mensajes: StateFlow<List<ReporteMensaje>> = _mensajes.asStateFlow()

    private var detalleJob: Job? = null
    private var mensajesJob: Job? = null

    init {
        viewModelScope.launch {
            repo.getAll().collect { _reportes.value = it }
        }
        viewModelScope.launch {
            userRepo.getAll().collect { usuarios ->
                _nombresUsuarios.value = usuarios.associate { it.id to it.nombre }
            }
        }
    }

    fun abrirReporte(id: Long) {
        detalleJob?.cancel()
        mensajesJob?.cancel()
        detalleJob = viewModelScope.launch {
            repo.observeById(id).collect { _reporteActual.value = it }
        }
        mensajesJob = viewModelScope.launch {
            repo.getMensajes(id).collect { _mensajes.value = it }
        }
    }

    fun cerrarReporte() {
        detalleJob?.cancel()
        mensajesJob?.cancel()
        _reporteActual.value = null
        _mensajes.value = emptyList()
    }

    fun enviarMensaje(remitenteId: Long, rol: String, texto: String) {
        val id = _reporteActual.value?.id ?: return
        if (texto.isBlank()) return
        viewModelScope.launch {
            val actual = repo.getById(id) ?: return@launch
            if (actual.estado.equals("FINALIZADO", ignoreCase = true)) return@launch
            repo.enviarMensaje(
                idReporte = id,
                idRemitente = remitenteId,
                rolRemitente = rol,
                contenido = texto.trim(),
                fechaHora = LocalDateTime.now().format(formatoFechaHora)
            )
        }
    }

    fun cambiarEstado(id: Long, estado: String) {
        viewModelScope.launch {
            repo.actualizarEstado(id, estado)
        }
    }
}

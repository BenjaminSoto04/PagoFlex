package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.repository.CompromisoRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EjecutivoViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: CompromisoRepository = RepositoryProvider.compromisos(application)

    private val _compromisos = MutableStateFlow<List<Compromiso>>(emptyList())
    val compromisos: StateFlow<List<Compromiso>> = _compromisos.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAll().collect { _compromisos.value = it }
        }
    }

    fun validarCompromiso(c: Compromiso): String? {
        if (c.empresa.isBlank()) return "Empresa obligatoria"
        if (c.concepto.isBlank()) return "Concepto obligatorio"
        if (c.monto <= 0) return "Monto invalido"
        if (c.fechaVencimiento.isBlank()) return "Fecha de vencimiento obligatoria"
        return try {
            LocalDate.parse(c.fechaVencimiento, DateTimeFormatter.ofPattern("dd-MM-uuuu"))
            null
        } catch (e: Exception) {
            "Formato de fecha invalido (dd-MM-aaaa)"
        }
    }

    fun crearCompromiso(c: Compromiso) {
        val err = validarCompromiso(c)
        if (err != null) { _error.value = err; return }
        viewModelScope.launch {
            repo.insert(c)
            _success.value = "Compromiso creado"
        }
    }

    fun modificarCompromiso(c: Compromiso) {
        val err = validarCompromiso(c)
        if (err != null) { _error.value = err; return }
        viewModelScope.launch {
            repo.update(c)
            _success.value = "Compromiso modificado"
        }
    }

    fun anularCompromiso(id: Long, motivo: String) {
        if (motivo.isBlank()) { _error.value = "Indica el motivo de anulacion"; return }
        viewModelScope.launch {
            repo.anular(id)
            _success.value = "Compromiso anulado"
        }
    }

    fun clear() { _error.value = null; _success.value = null }
}

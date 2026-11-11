package com.example.pagoflex.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.data.repository.ReporteRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportarProblemaViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = RepositoryProvider.reportes(application)

    private val _errorTipo = MutableStateFlow(false)
    val errorTipo = _errorTipo
    private val _errorCompromiso = MutableStateFlow(false)
    val errorCompromiso = _errorCompromiso
    private val _errorDescripcion = MutableStateFlow(false)
    val errorDescripcion = _errorDescripcion

    private val _enviado = MutableStateFlow(false)
    val enviado: StateFlow<Boolean> = _enviado

    // expose mutablestateflow as stateflow? keep simple
    val _tipo = MutableStateFlow("")
    val _compromiso = MutableStateFlow("")
    val _descripcion = MutableStateFlow("")

    fun validarYEnviar(tipo: String, compromiso: String, descripcion: String, userId: Long) {
        val eTipo = tipo.isBlank()
        val eComp = compromiso.isBlank()
        val eDesc = descripcion.trim().length < 10
        _errorTipo.value = eTipo
        _errorCompromiso.value = eComp
        _errorDescripcion.value = eDesc
        if (eTipo || eComp || eDesc) return
        viewModelScope.launch {
            repo.insert(Reporte(
                tipo = tipo,
                compromiso = compromiso,
                descripcion = descripcion.trim(),
                fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")),
                idUsuario = userId
            ))
            _enviado.value = true
        }
    }

    fun reset() { _enviado.value = false }
}


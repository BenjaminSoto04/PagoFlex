package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.ui.styles.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportarProblemaViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = RepositoryProvider.reportes(application)

    private val _errorTipo = MutableStateFlow<String?>(null)
    val errorTipo: StateFlow<String?> = _errorTipo.asStateFlow()

    private val _errorCompromiso = MutableStateFlow<String?>(null)
    val errorCompromiso: StateFlow<String?> = _errorCompromiso.asStateFlow()

    private val _errorDescripcion = MutableStateFlow<String?>(null)
    val errorDescripcion: StateFlow<String?> = _errorDescripcion.asStateFlow()

    private val _errorGeneral = MutableStateFlow<String?>(null)
    val errorGeneral: StateFlow<String?> = _errorGeneral.asStateFlow()

    private val _enviado = MutableStateFlow(false)
    val enviado: StateFlow<Boolean> = _enviado.asStateFlow()

    fun validarYEnviar(tipo: String, compromiso: String, descripcion: String, userId: Long) {
        _errorTipo.value = if (tipo.isBlank()) "Selecciona el tipo de problema" else null
        _errorCompromiso.value = if (compromiso.isBlank()) "Selecciona el compromiso relacionado" else null
        _errorDescripcion.value = Validaciones.descripcion(descripcion)
        _errorGeneral.value = null

        val hayErrores = listOf(_errorTipo.value, _errorCompromiso.value, _errorDescripcion.value).any { it != null }
        if (hayErrores) return

        if (userId <= 0) {
            _errorGeneral.value = "No pudimos identificar tu usuario. Vuelve a iniciar sesión e inténtalo otra vez."
            return
        }

        viewModelScope.launch {
            try {
                repo.insert(
                    Reporte(
                        tipo = tipo,
                        compromiso = compromiso,
                        descripcion = descripcion.trim(),
                        fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")),
                        idUsuario = userId
                    )
                )
                _enviado.value = true
            } catch (e: Exception) {
                _errorGeneral.value = "No pudimos enviar el reporte. Inténtalo nuevamente."
            }
        }
    }

    fun limpiarErrorTipo() { _errorTipo.value = null }

    fun limpiarErrorCompromiso() { _errorCompromiso.value = null }

    fun limpiarErrorDescripcion() { _errorDescripcion.value = null }

    fun reset() {
        _enviado.value = false
        _errorTipo.value = null
        _errorCompromiso.value = null
        _errorDescripcion.value = null
        _errorGeneral.value = null
    }
}

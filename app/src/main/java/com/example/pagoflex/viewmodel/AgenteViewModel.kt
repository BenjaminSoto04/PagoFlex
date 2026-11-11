package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.repository.CompromisoRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.model.Compromiso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgenteViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: CompromisoRepository = RepositoryProvider.compromisos(application)

    private val _mensaje = MutableStateFlow("Hola, soy el agente PagoFlex. ¿En que puedo ayudarte?")
    val mensaje: StateFlow<String> = _mensaje.asStateFlow()

    private val _compromisos = MutableStateFlow<List<Compromiso>>(emptyList())
    val compromisos: StateFlow<List<Compromiso>> = _compromisos.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAll().collect { _compromisos.value = it }
        }
    }

    fun consultar(texto: String) {
        val t = texto.lowercase()
        _mensaje.value = when {
            "pago" in t -> "Puedes pagar tus compromisos desde la seccion Mis compromisos."
            "venc" in t -> "Revisa tus compromisos vencidos en el inicio."
            "comprobante" in t -> "Los comprobantes estan en el historial de pagos."
            else -> "No entendi tu consulta. Intenta con pago, vencimiento o comprobante."
        }
    }
}

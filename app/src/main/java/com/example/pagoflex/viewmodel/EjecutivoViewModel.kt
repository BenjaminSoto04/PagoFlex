package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.CompromisoRepository
import com.example.pagoflex.data.repository.PagoRepository
import com.example.pagoflex.data.repository.Reporte
import com.example.pagoflex.data.repository.ReporteRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.repository.UserRepository
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.Pago
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Datos compartidos por las pantallas del rol ejecutivo: panel de la empresa,
 * gestion/anulacion de compromisos y reportes de recaudacion.
 */
class EjecutivoViewModel(application: Application) : AndroidViewModel(application) {
    private val repo: CompromisoRepository = RepositoryProvider.compromisos(application)
    private val pagoRepo: PagoRepository = RepositoryProvider.pagos(application)
    private val userRepo: UserRepository = RepositoryProvider.users(application)
    private val reporteRepo: ReporteRepository = RepositoryProvider.reportes(application)

    private val _compromisos = MutableStateFlow<List<Compromiso>>(emptyList())
    val compromisos: StateFlow<List<Compromiso>> = _compromisos.asStateFlow()

    private val _pagos = MutableStateFlow<List<Pago>>(emptyList())
    val pagos: StateFlow<List<Pago>> = _pagos.asStateFlow()

    private val _usuarios = MutableStateFlow<List<UserEntity>>(emptyList())
    val usuarios: StateFlow<List<UserEntity>> = _usuarios.asStateFlow()

    private val _reportes = MutableStateFlow<List<Reporte>>(emptyList())
    val reportes: StateFlow<List<Reporte>> = _reportes.asStateFlow()

    private val _usuarioActual = MutableStateFlow<UserEntity?>(null)
    val usuarioActual: StateFlow<UserEntity?> = _usuarioActual.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _success = MutableStateFlow<String?>(null)
    val success: StateFlow<String?> = _success.asStateFlow()

    init {
        val userId = SessionManager.userId(application)
        viewModelScope.launch { _usuarioActual.value = userRepo.getById(userId) }
        viewModelScope.launch { repo.getAll().collect { _compromisos.value = it } }
        viewModelScope.launch { pagoRepo.getAll().collect { _pagos.value = it } }
        viewModelScope.launch { userRepo.getAll().collect { _usuarios.value = it } }
        viewModelScope.launch { reporteRepo.getAll().collect { _reportes.value = it } }
    }

    fun validarCompromiso(c: Compromiso): String? {
        if (c.empresa.isBlank()) return "Empresa obligatoria"
        if (c.concepto.isBlank()) return "Concepto obligatorio"
        if (c.monto <= 0) return "Monto inválido"
        if (c.fechaVencimiento.isBlank()) return "Fecha de vencimiento obligatoria"
        return try {
            LocalDate.parse(c.fechaVencimiento, DateTimeFormatter.ofPattern("dd-MM-uuuu"))
            null
        } catch (e: Exception) {
            "Formato de fecha inválido (dd-MM-aaaa)"
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
        if (motivo.isBlank()) { _error.value = "Indica el motivo de la anulación"; return }
        _error.value = null
        viewModelScope.launch {
            runCatching { repo.anular(id, motivo) }
                .onSuccess { _success.value = "Compromiso anulado" }
                .onFailure { _error.value = "No pudimos anular el compromiso. Inténtalo nuevamente." }
        }
    }

    /** Vuelve a leer los datos locales (los flujos de Room ya están activos). */
    fun refrescar() {
        _error.value = null
        _success.value = "Datos actualizados desde la base local."
    }

    fun limpiarMensajes() {
        _error.value = null
        _success.value = null
    }

    fun clear() = limpiarMensajes()
}

package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.CompromisoEntity
import com.example.pagoflex.data.repository.CompromisoRepository
import com.example.pagoflex.data.repository.PagoRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.data.session.SessionManager
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.model.Pago
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PagosViewModel(application: Application) : AndroidViewModel(application) {

    private val compromisoRepo: CompromisoRepository = RepositoryProvider.compromisos(application)
    private val pagoRepo: PagoRepository = RepositoryProvider.pagos(application)

    private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-uuuu")
    private val formatoFechaHora = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

    private val recargosEmpresas = mapOf(
        3L to 12000,
        5L to 0,
        6L to 2000,
        8L to 7500
    )

    private val _compromisos = MutableStateFlow<List<Compromiso>>(emptyList())
    val compromisos: StateFlow<List<Compromiso>> = _compromisos.asStateFlow()

    private val _pagos = MutableStateFlow<List<Pago>>(emptyList())
    val pagos: StateFlow<List<Pago>> = _pagos.asStateFlow()

    init {
        viewModelScope.launch {
            compromisoRepo.getAll().collect { list ->
                _compromisos.value = list
            }
        }
        viewModelScope.launch {
            pagoRepo.getAll().collect { list ->
                _pagos.value = list
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            refrescarVencimientosInternal()
        }
    }

    private fun currentUserId(): Long {
        return SessionManager.userId(getApplication())
    }

    private fun actualizarVencimiento(compromiso: Compromiso): Compromiso {
        if (compromiso.estado != EstadoCompromiso.PENDIENTE) return compromiso
        val vencimiento = LocalDate.parse(compromiso.fechaVencimiento, formatoFecha)
        if (vencimiento.isBefore(LocalDate.now())) {
            val recargo = recargosEmpresas[compromiso.idEmpresa] ?: 0
            return compromiso.copy(estado = EstadoCompromiso.VENCIDO, recargoAplicado = recargo)
        }
        return compromiso
    }

    fun refrescarVencimientos() {
        viewModelScope.launch(Dispatchers.IO) { refrescarVencimientosInternal() }
    }

    private suspend fun refrescarVencimientosInternal() {
        val list = _compromisos.value
        list.forEach { c ->
            if (c.estado == EstadoCompromiso.PENDIENTE) {
                val updated = actualizarVencimiento(c)
                if (updated.estado != c.estado || updated.recargoAplicado != c.recargoAplicado) {
                    compromisoRepo.update(updated)
                }
            }
        }
    }

    fun obtenerCompromisos(): List<Compromiso> =
        _compromisos.value.filter { it.idUsuario == currentUserId() }

    fun filtrarCompromisos(estado: EstadoCompromiso): List<Compromiso> =
        obtenerCompromisos().filter { it.estado == estado }

    fun obtenerCompromiso(id: Long): Compromiso? =
        obtenerCompromisos().find { it.id == id }

    fun obtenerTotalCompromiso(compromiso: Compromiso): Int =
        compromiso.monto + compromiso.recargoAplicado

    fun obtenerTotalPendiente(): Int =
        obtenerCompromisos().filter { it.estado == EstadoCompromiso.PENDIENTE || it.estado == EstadoCompromiso.VENCIDO }
            .sumOf { obtenerTotalCompromiso(it) }

    fun obtenerCantidadVencidos(): Int =
        obtenerCompromisos().count { it.estado == EstadoCompromiso.VENCIDO }

    fun obtenerProximoVencimiento(): Compromiso? =
        obtenerCompromisos().filter { it.estado == EstadoCompromiso.PENDIENTE }
            .minByOrNull { LocalDate.parse(it.fechaVencimiento, formatoFecha) }

    fun validarPago(compromiso: Compromiso): String? {
        val actual = obtenerCompromiso(compromiso.id) ?: return "Compromiso no encontrado"
        when (actual.estado) {
            EstadoCompromiso.PAGADO -> return "Este compromiso ya esta pagado"
            EstadoCompromiso.ANULADO -> return "Este compromiso esta anulado"
            EstadoCompromiso.EN_REVISION -> return "Este compromiso esta en revision"
            EstadoCompromiso.PENDIENTE, EstadoCompromiso.VENCIDO -> Unit
        }
        val pagoExistente = _pagos.value.any { it.idCompromiso == actual.id }
        if (pagoExistente) return "Este compromiso ya tiene un pago registrado"

        val fechaActual = LocalDate.parse(actual.fechaVencimiento, formatoFecha)
        val tieneAnterior = obtenerCompromisos().any { otro ->
            val fechaOtro = LocalDate.parse(otro.fechaVencimiento, formatoFecha)
            val estaActivo = otro.estado == EstadoCompromiso.PENDIENTE || otro.estado == EstadoCompromiso.VENCIDO
            val esAnterior = fechaOtro.isBefore(fechaActual) || (
                    fechaOtro.isEqual(fechaActual) &&
                            otro.numeroCuota != null && actual.numeroCuota != null &&
                            otro.numeroCuota < actual.numeroCuota
                    )
            otro.id != actual.id && otro.idEmpresa == actual.idEmpresa && estaActivo && esAnterior
        }
        if (tieneAnterior) return "Primero debes pagar el compromiso anterior de esta empresa"
        return null
    }

    fun pagarCompromiso(id: Long): String? = runBlocking(Dispatchers.IO) {
        refrescarVencimientosInternal()
        val compromiso = obtenerCompromiso(id) ?: return@runBlocking "Compromiso no encontrado"
        val error = validarPago(compromiso)
        if (error != null) return@runBlocking error
        if (_pagos.value.any { it.idCompromiso == id }) return@runBlocking "Este compromiso ya tiene un pago registrado"

        val siguienteId = (_pagos.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val siguienteFolio = (_pagos.value.maxOfOrNull {
            it.numeroComprobante.removePrefix("CPR-").toLongOrNull() ?: 0L
        } ?: 0L) + 1L
        val numeroComprobante = "CPR-${siguienteFolio.toString().padStart(6, '0')}"

        val nuevoPago = Pago(
            id = siguienteId,
            numeroComprobante = numeroComprobante,
            idCompromiso = compromiso.id,
            fechaHora = LocalDateTime.now().format(formatoFechaHora),
            montoPagado = obtenerTotalCompromiso(compromiso),
            canal = "App",
            medioPago = "Pago simulado"
        )
        pagoRepo.insert(nuevoPago)
        compromisoRepo.update(compromiso.copy(estado = EstadoCompromiso.PAGADO))
        null
    }

    fun obtenerHistorial(): List<Pago> {
        val idsCompromisos = obtenerCompromisos().map { it.id }.toSet()
        return _pagos.value.filter { it.idCompromiso in idsCompromisos }
    }

    fun obtenerComprobante(idCompromiso: Long): Pago? =
        obtenerHistorial().find { it.idCompromiso == idCompromiso }

    // Executive helpers
    fun crearCompromiso(compromiso: Compromiso) {
        viewModelScope.launch(Dispatchers.IO) { compromisoRepo.insert(compromiso) }
    }

    fun modificarCompromiso(compromiso: Compromiso) {
        viewModelScope.launch(Dispatchers.IO) { compromisoRepo.update(compromiso) }
    }

    fun anularCompromiso(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { compromisoRepo.anular(id) }
    }

    // attach photo to pago
    fun adjuntarFotoPago(idPago: Long, uri: String) {
        viewModelScope.launch(Dispatchers.IO) { pagoRepo.updateFoto(idPago, uri) }
    }
}

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
import com.example.pagoflex.util.FolioGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

class PagosViewModel(application: Application) : AndroidViewModel(application) {

    private val compromisoRepo: CompromisoRepository = RepositoryProvider.compromisos(application)
    private val pagoRepo: PagoRepository = RepositoryProvider.pagos(application)

    private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-uuuu")
    private val formatoFechaHora = DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

    private val recargosEmpresas = mapOf(
        3L to 12000,
        5L to 0,
        6L to 2000,
        8L to 7500,
        1009L to 9000,
        1010L to 6000,
        1011L to 4000
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
        val userId = currentUserId()

        //Consultar directamente los compromisos del usuario actual.
        val list = compromisoRepo.getByUsuario(userId).first()

        list.forEach { compromiso ->
            if (compromiso.estado == EstadoCompromiso.PENDIENTE){
                val updated = actualizarVencimiento(compromiso)

                if (
                    updated.estado != compromiso.estado ||
                    updated.recargoAplicado != compromiso.recargoAplicado
                ) {
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
        return validarPagoInterno(
            actual = actual,
            compromisosUsuario = obtenerCompromisos(),
            pagos = _pagos.value
        )
    }

    /**
     * Reglas de pago evaluadas contra una foto consistente de Room: un pago por
     * cuota y primero el compromiso activo mas antiguo de la misma empresa.
     */
    private fun validarPagoInterno(
        actual: Compromiso,
        compromisosUsuario: List<Compromiso>,
        pagos: List<Pago>
    ): String? {
        when (actual.estado) {
            EstadoCompromiso.PAGADO -> return "Este compromiso ya está pagado"
            EstadoCompromiso.ANULADO -> return "Este compromiso está anulado"
            EstadoCompromiso.EN_REVISION -> return "Este compromiso está en revisión"
            EstadoCompromiso.PENDIENTE, EstadoCompromiso.VENCIDO -> Unit
        }
        // Una fila representa una cuota: mientras queden cuotas por delante se
        // permite un pago, aunque ya existan pagos de cuotas anteriores.
        val tieneCuotasPendientes = actual.numeroCuota != null &&
                actual.totalCuotas != null &&
                actual.numeroCuota < actual.totalCuotas
        val pagoExistente = pagos.any { it.idCompromiso == actual.id }
        if (pagoExistente && !tieneCuotasPendientes) {
            return "Este compromiso ya tiene un pago registrado"
        }

        val fechaActual = LocalDate.parse(actual.fechaVencimiento, formatoFecha)
        val tieneAnterior = compromisosUsuario.any { otro ->
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

    /**
     * Fecha de la siguiente cuota. Mantiene el dia de vencimiento original y,
     * si quedo en el pasado, avanza hasta el primer mes futuro.
     */
    private fun calcularSiguienteVencimiento(fechaActual: LocalDate): LocalDate {
        var siguiente = fechaActual.plusMonths(1)
        val hoy = LocalDate.now()
        while (siguiente.isBefore(hoy)) {
            siguiente = siguiente.plusMonths(1)
        }
        return siguiente
    }

    /**
     * Genera y persiste el folio unico del compromiso cuando el usuario abre su
     * detalle. Si ya existe, no se modifica.
     */
    fun asegurarFolioCompromiso(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val compromiso = compromisoRepo.getById(id) ?: return@launch
            if (compromiso.idUsuario != currentUserId()) return@launch
            val folioCompromiso = compromiso.folioUnico
            if (folioCompromiso.isNullOrBlank()) {
                compromisoRepo.update(compromiso.copy(folioUnico = FolioGenerator.generar()))
            }
            _pagos.value
                .filter { it.idCompromiso == id && it.folioUnico.isNullOrBlank() }
                .forEach { pago ->
                    pagoRepo.update(pago.copy(folioUnico = FolioGenerator.generar()))
                }
        }
    }

    fun pagarCompromiso(id: Long): String? = runBlocking(Dispatchers.IO) {
        refrescarVencimientosInternal()

        val userId = currentUserId()
        val compromisosUsuario = compromisoRepo.getByUsuario(userId).first()
        val compromiso = compromisosUsuario.find { it.id == id }
            ?: return@runBlocking "Compromiso no encontrado"

        val pagosActuales = pagoRepo.getAllSync()
        val error = validarPagoInterno(compromiso, compromisosUsuario, pagosActuales)
        if (error != null) return@runBlocking error

        val siguienteId = (pagosActuales.maxOfOrNull { it.id } ?: 0L) + 1L
        val siguienteFolio = (pagosActuales.maxOfOrNull {
            it.numeroComprobante.removePrefix("CPR-").toLongOrNull() ?: 0L
        } ?: 0L) + 1L
        val numeroComprobante = "CPR-${siguienteFolio.toString().padStart(6, '0')}"

        val folioUnicoPago = FolioGenerator.generar()
        val folioUnicoCompromiso = compromiso.folioUnico
            ?.takeIf { it.isNotBlank() }
            ?: FolioGenerator.generar()

        val cuotaActual = compromiso.numeroCuota
        val totalCuotas = compromiso.totalCuotas
        val tieneSiguienteCuota = cuotaActual != null &&
                totalCuotas != null &&
                cuotaActual < totalCuotas

        val nuevoPago = Pago(
            id = siguienteId,
            numeroComprobante = numeroComprobante,
            idCompromiso = compromiso.id,
            fechaHora = LocalDateTime.now().format(formatoFechaHora),
            montoPagado = obtenerTotalCompromiso(compromiso),
            canal = "App",
            medioPago = "Pago simulado",
            folioUnico = folioUnicoPago,
            cuotaPagada = cuotaActual,
            fechaVencimientoCuota = compromiso.fechaVencimiento
        )
        pagoRepo.insert(nuevoPago)

        // La cuota pagada queda siempre en PAGADO para que aparezca en el
        // filtro "Pagados". Si el compromiso tiene mas cuotas, se registra la
        // siguiente como una fila PENDIENTE independiente; asi el panel
        // ejecutivo sigue contando el saldo real por cobrar.
        compromisoRepo.update(
            compromiso.copy(
                estado = EstadoCompromiso.PAGADO,
                folioUnico = folioUnicoCompromiso
            )
        )

        if (tieneSiguienteCuota) {
            val fechaSiguiente = calcularSiguienteVencimiento(
                LocalDate.parse(compromiso.fechaVencimiento, formatoFecha)
            )
            compromisoRepo.insert(
                compromiso.copy(
                    id = compromisoRepo.nextId(),
                    numeroCuota = cuotaActual + 1,
                    fechaVencimiento = fechaSiguiente.format(formatoFecha),
                    estado = EstadoCompromiso.PENDIENTE,
                    recargoAplicado = 0,
                    motivoAnulacion = null,
                    folioUnico = FolioGenerator.generar()
                )
            )
        }
        null
    }

    fun obtenerHistorial(): List<Pago> {
        val idsCompromisos = obtenerCompromisos().map { it.id }.toSet()
        return _pagos.value.filter { it.idCompromiso in idsCompromisos }
    }

    fun obtenerComprobante(idCompromiso: Long): Pago? =
        obtenerHistorial()
            .filter { it.idCompromiso == idCompromiso }
            .maxByOrNull { it.id }

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
}

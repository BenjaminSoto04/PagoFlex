package com.example.pagoflex.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pagoflex.data.DatosSimulados
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.model.Pago
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PagosViewModel : ViewModel() {

    // Formatos utilizados por PagoFlex
    private val formatoFecha =
        DateTimeFormatter.ofPattern("dd-MM-uuuu")

    private val formatoFechaHora =
        DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm")

    // Recargos fijos de las empresas de prueba
    private val recargosEmpresas = mapOf(
        3L to 12000,
        5L to 0,
        6L to 2000,
        8L to 7500
    )

    // Lista observable de compromisos
    private val _compromisos = MutableStateFlow(
        DatosSimulados.compromisos.map {
            actualizarVencimiento(it)
        }
    )

    val compromisos: StateFlow<List<Compromiso>> =
        _compromisos.asStateFlow()

    // Lista observable de pagos
    private val _pagos = MutableStateFlow(
        DatosSimulados.pagos
    )

    val pagos: StateFlow<List<Pago>> =
        _pagos.asStateFlow()

    // Actualizar el estado según la fecha
    private fun actualizarVencimiento(
        compromiso: Compromiso
    ): Compromiso {

        if (compromiso.estado != EstadoCompromiso.PENDIENTE) {
            return compromiso
        }

        val vencimiento = LocalDate.parse(
            compromiso.fechaVencimiento,
            formatoFecha
        )

        if (vencimiento.isBefore(LocalDate.now())) {

            val recargo = recargosEmpresas[
                compromiso.idEmpresa
            ] ?: 0

            return compromiso.copy(
                estado = EstadoCompromiso.VENCIDO,
                recargoAplicado = recargo
            )
        }

        return compromiso
    }

    // Obtener compromisos del usuario actual
    fun obtenerCompromisos(): List<Compromiso> {

        return _compromisos.value.filter {
            it.idUsuario == DatosSimulados.ID_USUARIO_ACTUAL
        }
    }

    // Filtrar compromisos por estado
    fun filtrarCompromisos(
        estado: EstadoCompromiso
    ): List<Compromiso> {

        return obtenerCompromisos().filter {
            it.estado == estado
        }
    }

    // Buscar compromiso por identificador
    fun obtenerCompromiso(
        id: Long
    ): Compromiso? {

        return obtenerCompromisos().find {
            it.id == id
        }
    }

    // Calcular el total de un compromiso
    fun obtenerTotalCompromiso(
        compromiso: Compromiso
    ): Int {

        return compromiso.monto +
                compromiso.recargoAplicado
    }

    // Calcular el total pendiente
    fun obtenerTotalPendiente(): Int {

        return obtenerCompromisos()
            .filter {
                it.estado == EstadoCompromiso.PENDIENTE ||
                        it.estado == EstadoCompromiso.VENCIDO
            }
            .sumOf {
                obtenerTotalCompromiso(it)
            }
    }

    // Cantidad de compromisos vencidos
    fun obtenerCantidadVencidos(): Int {

        return obtenerCompromisos().count {
            it.estado == EstadoCompromiso.VENCIDO
        }
    }

    // Obtener próximo vencimiento
    fun obtenerProximoVencimiento(): Compromiso? {

        return obtenerCompromisos()
            .filter {
                it.estado == EstadoCompromiso.PENDIENTE
            }
            .minByOrNull {
                LocalDate.parse(
                    it.fechaVencimiento,
                    formatoFecha
                )
            }
    }

    // Validar si se puede pagar
    fun validarPago(
        compromiso: Compromiso
    ): String? {

        val actual = obtenerCompromiso(compromiso.id)
            ?: return "Compromiso no encontrado"

        when (actual.estado) {

            EstadoCompromiso.PAGADO ->
                return "Este compromiso ya está pagado"

            EstadoCompromiso.ANULADO ->
                return "Este compromiso está anulado"

            EstadoCompromiso.EN_REVISION ->
                return "Este compromiso está en revisión"

            else -> Unit
        }

        // Verificar cuotas anteriores sin pagar
        val fechaActual = LocalDate.parse(
            actual.fechaVencimiento,
            formatoFecha
        )

        val tieneAnterior = obtenerCompromisos().any {

            val fechaOtra = LocalDate.parse(
                it.fechaVencimiento,
                formatoFecha
            )

            it.id != actual.id &&
                    it.idEmpresa == actual.idEmpresa &&
                    it.concepto == actual.concepto &&
                    (it.estado == EstadoCompromiso.PENDIENTE ||
                            it.estado == EstadoCompromiso.VENCIDO) &&
                    (
                            fechaOtra.isBefore(fechaActual) ||
                                    (
                                            fechaOtra.isEqual(fechaActual) &&
                                                    it.id < actual.id
                                            )
                            )
        }

        if (tieneAnterior) {
            return "Primero debes pagar el compromiso anterior"
        }

        return null
    }

    // Confirmar un pago simulado
    fun pagarCompromiso(id: Long): String? {

        val compromiso = obtenerCompromiso(id)
            ?: return "Compromiso no encontrado"

        val error = validarPago(compromiso)

        if (error != null) {
            return error
        }

        // Evitar pagos duplicados
        if (_pagos.value.any {
                it.idCompromiso == id
            }) {
            return "Este compromiso ya tiene un pago registrado"
        }

        val siguienteId = (
                _pagos.value.maxOfOrNull { it.id } ?: 0L
                ) + 1L

        val siguienteFolio = (
                _pagos.value.maxOfOrNull {
                    it.numeroComprobante
                        .removePrefix("CPR-")
                        .toLongOrNull() ?: 0L
                } ?: 0L
                ) + 1L

        val numeroComprobante =
            "CPR-${siguienteFolio.toString().padStart(6, '0')}"

        val nuevoPago = Pago(
            id = siguienteId,
            numeroComprobante = numeroComprobante,
            idCompromiso = compromiso.id,
            fechaHora = LocalDateTime.now()
                .format(formatoFechaHora),
            montoPagado = obtenerTotalCompromiso(compromiso),
            canal = "App",
            medioPago = "Pago simulado"
        )

        // Agregar pago al historial
        _pagos.value = _pagos.value + nuevoPago

        // Actualizar compromiso
        _compromisos.value = _compromisos.value.map {

            if (it.id == compromiso.id) {
                it.copy(
                    estado = EstadoCompromiso.PAGADO
                )
            } else {
                it
            }
        }

        // null significa que el pago fue exitoso
        return null
    }

    // Consultar historial del usuario
    fun obtenerHistorial(): List<Pago> {

        val idsCompromisos = obtenerCompromisos()
            .map { it.id }
            .toSet()

        return _pagos.value.filter {
            it.idCompromiso in idsCompromisos
        }
    }

    // Obtener comprobante de un compromiso
    fun obtenerComprobante(
        idCompromiso: Long
    ): Pago? {

        return obtenerHistorial().find {
            it.idCompromiso == idCompromiso
        }
    }
}
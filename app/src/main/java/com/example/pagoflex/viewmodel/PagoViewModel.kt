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

    // Recargos fijos de las empresas ficticias
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

    fun refrescarVencimientos() {

        _compromisos.value = _compromisos.value.map {
            actualizarVencimiento(it)
        }
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

        // Buscar el compromiso actualizado
        val actual = obtenerCompromiso(compromiso.id)
            ?: return "Compromiso no encontrado"

        // Validar el estado
        when (actual.estado) {

            EstadoCompromiso.PAGADO ->
                return "Este compromiso ya está pagado"

            EstadoCompromiso.ANULADO ->
                return "Este compromiso está anulado"

            EstadoCompromiso.EN_REVISION ->
                return "Este compromiso está en revisión"

            EstadoCompromiso.PENDIENTE,
            EstadoCompromiso.VENCIDO -> Unit
        }

        // Evitar pagos duplicados
        val pagoExistente = _pagos.value.any {
            it.idCompromiso == actual.id
        }

        if (pagoExistente) {
            return "Este compromiso ya tiene un pago registrado"
        }

        // Fecha del compromiso seleccionado
        val fechaActual = LocalDate.parse(
            actual.fechaVencimiento,
            formatoFecha
        )

        // Buscar compromisos anteriores
        // que pertenezcan a la misma empresa
        val tieneAnterior = obtenerCompromisos().any { otro ->

            val fechaOtro = LocalDate.parse(
                otro.fechaVencimiento,
                formatoFecha
            )

            val estaActivo =
                otro.estado == EstadoCompromiso.PENDIENTE ||
                        otro.estado == EstadoCompromiso.VENCIDO

            val esAnterior =
                fechaOtro.isBefore(fechaActual) ||
                        (
                                fechaOtro.isEqual(fechaActual) &&
                                        otro.numeroCuota != null &&
                                        actual.numeroCuota != null &&
                                        otro.numeroCuota < actual.numeroCuota
                                )

            otro.id != actual.id &&
                    otro.idEmpresa == actual.idEmpresa &&
                    estaActivo &&
                    esAnterior
        }

        if (tieneAnterior) {
            return "Primero debes pagar el compromiso anterior de esta empresa"
        }

        return null
    }

    // Confirmar un pago simulado
    fun pagarCompromiso(id: Long): String? {

        refrescarVencimientos()

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
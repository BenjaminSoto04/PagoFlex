package com.example.pagoflex.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.local.entity.EmpresaEntity
import com.example.pagoflex.data.repository.CompromisoRepository
import com.example.pagoflex.data.repository.EmpresaRepository
import com.example.pagoflex.data.repository.RepositoryProvider
import com.example.pagoflex.model.Compromiso
import com.example.pagoflex.model.EstadoCompromiso
import com.example.pagoflex.util.FolioGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Calculo unico de vencimientos usado por el formulario y por el guardado.
 *
 * - Monto total: un unico pago que vence en un mes desde hoy.
 * - Monto por cuota: la primera cuota vence el dia 5 del mes siguiente y las
 *   siguientes mantienen el dia 5 mes a mes.
 */
object CalendarioCompromisos {
    private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu")

    fun primerVencimiento(modoMonto: String, hoy: LocalDate = LocalDate.now()): LocalDate =
        if (modoMonto == "TOTAL") {
            hoy.plusMonths(1)
        } else {
            hoy.plusMonths(1).withDayOfMonth(5)
        }

    fun vencimientos(
        modoMonto: String,
        cuotas: Int,
        hoy: LocalDate = LocalDate.now()
    ): List<LocalDate> {
        val cantidad = if (modoMonto == "TOTAL") 1 else cuotas.coerceAtLeast(1)
        val primera = primerVencimiento(modoMonto, hoy)
        return (0 until cantidad).map { primera.plusMonths(it.toLong()) }
    }

    fun formatear(fecha: LocalDate): String = fecha.format(formatoFecha)
}

/**
 * Formulario de alta de compromisos del usuario. Genera el folio unico y deja
 * el compromiso en estado PENDIENTE, en la cuota 1, listo para el flujo de pago.
 */
class NuevoCompromisoViewModel(application: Application) : AndroidViewModel(application) {
    private val compromisoRepo: CompromisoRepository = RepositoryProvider.compromisos(application)
    private val empresaRepo: EmpresaRepository = RepositoryProvider.empresas(application)

    private val _empresas = MutableStateFlow<List<EmpresaEntity>>(emptyList())
    val empresas: StateFlow<List<EmpresaEntity>> = _empresas.asStateFlow()

    private val _errorEmpresa = MutableStateFlow<String?>(null)
    val errorEmpresa: StateFlow<String?> = _errorEmpresa.asStateFlow()

    private val _errorConcepto = MutableStateFlow<String?>(null)
    val errorConcepto: StateFlow<String?> = _errorConcepto.asStateFlow()

    private val _errorMonto = MutableStateFlow<String?>(null)
    val errorMonto: StateFlow<String?> = _errorMonto.asStateFlow()

    private val _errorCuotas = MutableStateFlow<String?>(null)
    val errorCuotas: StateFlow<String?> = _errorCuotas.asStateFlow()

    private val _errorGeneral = MutableStateFlow<String?>(null)
    val errorGeneral: StateFlow<String?> = _errorGeneral.asStateFlow()

    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    private val _guardado = MutableStateFlow(false)
    val guardado: StateFlow<Boolean> = _guardado.asStateFlow()

    init {
        viewModelScope.launch {
            empresaRepo.getAll().collect { _empresas.value = it.sortedBy { empresa -> empresa.nombreFantasia } }
        }
    }

    fun guardar(
        userId: Long,
        empresaId: Long?,
        tipo: String,
        detalle: String,
        modoMonto: String,
        montoTexto: String,
        cuotasTexto: String
    ) {
        _errorEmpresa.value = null
        _errorConcepto.value = null
        _errorMonto.value = null
        _errorCuotas.value = null
        _errorGeneral.value = null

        if (userId <= 0) {
            _errorGeneral.value = "No pudimos identificar tu usuario. Vuelve a iniciar sesión."
            return
        }

        val empresa = _empresas.value.find { it.id == empresaId }
        if (empresa == null) _errorEmpresa.value = "Selecciona una empresa"

        if (tipo.isBlank()) _errorConcepto.value = "Selecciona el tipo de compromiso"

        val montoIngresado = montoTexto.filter { it.isDigit() }.toIntOrNull()
        if (montoIngresado == null || montoIngresado <= 0) {
            _errorMonto.value = "Ingresa un monto válido mayor a cero"
        }

        // "Monto total" siempre representa un pago unico: las cuotas quedan
        // fijas en 1 sin importar lo que llegue desde el formulario.
        val cuotas = if (modoMonto == "TOTAL") {
            1
        } else {
            cuotasTexto.filter { it.isDigit() }.toIntOrNull()
        }
        if (cuotas == null || cuotas < 1) {
            _errorCuotas.value = "Ingresa al menos 1 cuota"
        }

        val montoPorCuota = if (montoIngresado == null || cuotas == null || cuotas < 1) {
            0
        } else {
            montoIngresado
        }

        val hayErrores = listOf(
            _errorEmpresa.value,
            _errorConcepto.value,
            _errorMonto.value,
            _errorCuotas.value
        ).any { it != null }
        if (hayErrores || empresa == null || cuotas == null || montoPorCuota <= 0) return

        val concepto = if (detalle.isBlank()) tipo else "$tipo · ${detalle.trim()}"
        val fechaVencimiento = CalendarioCompromisos.formatear(
            CalendarioCompromisos.primerVencimiento(modoMonto)
        )

        viewModelScope.launch {
            _guardando.value = true
            try {
                val id = compromisoRepo.nextId()
                compromisoRepo.insert(
                    Compromiso(
                        id = id,
                        folio = FolioGenerator.generar("CP"),
                        idUsuario = userId,
                        idEmpresa = empresa.id,
                        empresa = empresa.nombreFantasia,
                        concepto = concepto,
                        numeroCuota = 1,
                        totalCuotas = cuotas,
                        monto = montoPorCuota,
                        fechaVencimiento = fechaVencimiento,
                        estado = EstadoCompromiso.PENDIENTE,
                        recargoAplicado = 0,
                        folioUnico = FolioGenerator.generar()
                    )
                )
                _guardado.value = true
            } catch (e: Exception) {
                _errorGeneral.value = "No pudimos guardar el compromiso. Inténtalo nuevamente."
            } finally {
                _guardando.value = false
            }
        }
    }

    fun limpiarErrorEmpresa() { _errorEmpresa.value = null }
    fun limpiarErrorConcepto() { _errorConcepto.value = null }
    fun limpiarErrorMonto() { _errorMonto.value = null }
    fun limpiarErrorCuotas() { _errorCuotas.value = null }

    fun reset() {
        _errorEmpresa.value = null
        _errorConcepto.value = null
        _errorMonto.value = null
        _errorCuotas.value = null
        _errorGeneral.value = null
        _guardado.value = false
    }
}

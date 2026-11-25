package com.example.pagoflex.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pagoflex.data.remote.IndicadoresApiService
import com.example.pagoflex.data.remote.RetrofitClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class IndicadoresData(
    val fecha: String?,
    val uf: Double?,
    val uf12: Double?,
    val dolar_america: Double?,
    val dolar_canada: Double?,
    val moneda_australia: Double?,
    val moneda_brasil: Double?,
    val moneda_colombia: Double?,
    val moneda_nueva_zelandia: Double?,
    val moneda_peru: Double?,
    val moneda_china: Double?
)

data class IndicadorState(
    val loading: Boolean = false,
    val error: String? = null,
    val indicador: IndicadoresData? = null,
    val mostrarIndicadores: Boolean = true, // Para manejar modo offline
    /** true cuando los valores mostrados no vienen de mindicador.cl (respaldo en CLP). */
    val usandoRespaldo: Boolean = false
)

class IndicadorViewModel @JvmOverloads constructor(
    private val indicadoresApiService: IndicadoresApiService = RetrofitClient.indicadores
) : ViewModel() {

    private val _state = MutableStateFlow(IndicadorState(loading = true))
    val state: StateFlow<IndicadorState> = _state.asStateFlow()

    /** Ultima respuesta valida de mindicador.cl, para reutilizarla si luego falla. */
    private var ultimoValorValido: IndicadoresData? = null

    init {
        cargarIndicadores()
    }

    fun cargarIndicadores() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val respuesta = consultarIndicadores()
            val valido = respuesta?.takeIf { it.uf != null }

            if (valido != null) {
                ultimoValorValido = valido
                _state.value = IndicadorState(
                    loading = false,
                    indicador = valido
                )
            } else {
                // mindicador.cl tardo demasiado, respondio error (por ejemplo 500)
                // o no hubo conexion: se mantienen indicadores en CLP.
                _state.value = IndicadorState(
                    loading = false,
                    indicador = ultimoValorValido ?: INDICADORES_RESPALDO,
                    usandoRespaldo = true,
                    error = "No se pudo actualizar desde mindicador.cl; se muestran valores de respaldo en CLP."
                )
            }
        }
    }

    fun refrescar() {
        cargarIndicadores()
    }

    /**
     * Consulta mindicador.cl con un limite de tiempo propio para no dejar la
     * tarjeta en "cargando" cuando el servicio tarda. Devuelve null ante
     * timeout, error HTTP (500 incluido) o fallo de red.
     */
    private suspend fun consultarIndicadores(): IndicadoresData? =
        try {
            withTimeout(TIEMPO_MAXIMO_MS) {
                val response = indicadoresApiService.obtenerIndicadores()
                if (response.isSuccessful) response.body() else null
            }
        } catch (e: TimeoutCancellationException) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    companion object {
        /** Si mindicador.cl no responde en este plazo se usa el respaldo local. */
        private const val TIEMPO_MAXIMO_MS = 6_000L

        /**
         * Valores referenciales en CLP usados cuando mindicador.cl no responde.
         * Se muestran igual que los reales, con el aviso de respaldo en la UI.
         */
        val INDICADORES_RESPALDO = IndicadoresData(
            fecha = null,
            uf = 39_842.53,
            uf12 = 37_912.44,
            dolar_america = 946.20,
            dolar_canada = 689.15,
            moneda_australia = 618.40,
            moneda_brasil = 174.20,
            moneda_colombia = 0.24,
            moneda_nueva_zelandia = 566.30,
            moneda_peru = 251.70,
            moneda_china = 132.10
        )
    }
}

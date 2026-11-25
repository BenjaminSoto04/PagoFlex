package com.example.pagoflex.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Retrofit de la app.
 *
 * Todos los microservicios de PagoFlexMS se exponen en la misma maquina que
 * ejecuta el emulador, por lo que se usa `10.0.2.2` (loopback del host visto
 * desde el AVD):
 *
 * - usuarios      8083
 * - compromisos   8081
 * - agente        8082
 * - pagos         8084
 * - empresas      8085
 * - reportes      8086
 */
object RetrofitClient {

    /** Host del backend. Para un dispositivo fisico, reemplazar por la IP del PC. */
    private const val HOST = "http://10.0.2.2"

    private const val PUERTO_USUARIOS = 8083
    private const val PUERTO_COMPROMISOS = 8081
    private const val PUERTO_AGENTE = 8082
    private const val PUERTO_PAGOS = 8084
    private const val PUERTO_EMPRESAS = 8085
    private const val PUERTO_REPORTES = 8086

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Timeouts cortos para que el fallback a Room ocurra rapido cuando el
     * microservicio no esta levantado.
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    // API externa (Mindicador.cl)
    val indicadores: IndicadoresApiService by lazy {
        crear("https://mindicador.cl/", IndicadoresApiService::class.java)
    }

    val usuarios: UsuariosApiService by lazy {
        crear(url(PUERTO_USUARIOS), UsuariosApiService::class.java)
    }

    val empresas: EmpresasApiService by lazy {
        crear(url(PUERTO_EMPRESAS), EmpresasApiService::class.java)
    }

    val compromisos: CompromisosApiService by lazy {
        crear(url(PUERTO_COMPROMISOS), CompromisosApiService::class.java)
    }

    val agente: AgenteApiService by lazy {
        crear(url(PUERTO_AGENTE), AgenteApiService::class.java)
    }

    val pagos: PagosApiService by lazy {
        crear(url(PUERTO_PAGOS), PagosApiService::class.java)
    }

    val reportes: ReportesApiService by lazy {
        crear(url(PUERTO_REPORTES), ReportesApiService::class.java)
    }

    fun obtenerIndicadores(): IndicadoresApiService = indicadores
    fun obtenerUsuarios(): UsuariosApiService = usuarios
    fun obtenerEmpresas(): EmpresasApiService = empresas
    fun obtenerCompromisos(): CompromisosApiService = compromisos
    fun obtenerAgente(): AgenteApiService = agente
    fun obtenerPagos(): PagosApiService = pagos
    fun obtenerReportes(): ReportesApiService = reportes

    private fun url(puerto: Int): String = "$HOST:$puerto/"

    private fun <T> crear(baseUrl: String, serviceInterface: Class<T>): T =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(serviceInterface)
}

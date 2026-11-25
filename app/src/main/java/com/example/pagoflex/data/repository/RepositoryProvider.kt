package com.example.pagoflex.data.repository

import android.content.Context
import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.remote.RetrofitClient

/**
 * Service locator de repositorios.
 *
 * Todos los repositorios combinan el microservicio correspondiente de
 * PagoFlexMS con Room como cache/fallback local:
 *
 * - usuarios      8083
 * - compromisos   8081
 * - agente        8082
 * - pagos         8084
 * - empresas      8085
 * - reportes      8086
 */
object RepositoryProvider {

    fun database(context: Context) = AppDatabase.getDatabase(context)

    fun users(context: Context) =
        UserRepositoryImpl(database(context), RetrofitClient.obtenerUsuarios())

    fun empresas(context: Context) =
        EmpresaRepositoryImpl(database(context), RetrofitClient.obtenerEmpresas())

    fun compromisos(context: Context) =
        CompromisoRepositoryImpl(database(context), RetrofitClient.obtenerCompromisos())

    fun pagos(context: Context) =
        PagoRepositoryImpl(database(context), RetrofitClient.obtenerPagos())

    fun reportes(context: Context) =
        ReporteRepositoryImpl(database(context), RetrofitClient.obtenerReportes())

    fun chat(context: Context) =
        ChatRepositoryImpl(database(context), RetrofitClient.obtenerAgente())
}

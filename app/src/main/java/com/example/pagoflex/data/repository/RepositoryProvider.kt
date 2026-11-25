package com.example.pagoflex.data.repository

import android.content.Context
import com.example.pagoflex.data.remote.RetrofitClient

object RepositoryProvider {

    fun users(context: Context) =
        UserRepositoryImpl(RetrofitClient.obtenerUsuarios())

    fun empresas(context: Context) =
        EmpresaRepositoryImpl(RetrofitClient.obtenerEmpresas())

    fun compromisos(context: Context) =
        CompromisoRepositoryImpl(RetrofitClient.obtenerCompromisos())

    fun pagos(context: Context) =
        PagoRepositoryImpl(RetrofitClient.obtenerPagos())

    fun reportes(context: Context) =
        ReporteRepositoryImpl(RetrofitClient.obtenerReportes())

    fun chat(context: Context) =
        ChatRepositoryImpl(RetrofitClient.obtenerAgente())
}

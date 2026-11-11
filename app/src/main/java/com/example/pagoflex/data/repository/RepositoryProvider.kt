package com.example.pagoflex.data.repository

import android.content.Context
import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.repository.ChatRepositoryImpl
import com.example.pagoflex.data.local.entity.EmpresaEntity

object RepositoryProvider {
    fun database(context: Context) = AppDatabase.getDatabase(context)
    fun users(context: Context) = UserRepositoryImpl(database(context))
    fun compromisos(context: Context) = CompromisoRepositoryImpl(database(context))
    fun pagos(context: Context) = PagoRepositoryImpl(database(context))
    fun reportes(context: Context) = ReporteRepositoryImpl(database(context))
    fun chat(context: Context) = ChatRepositoryImpl(database(context))
}

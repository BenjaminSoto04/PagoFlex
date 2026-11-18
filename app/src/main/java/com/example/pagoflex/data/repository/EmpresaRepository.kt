package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow

interface EmpresaRepository {
    fun getAll(): Flow<List<EmpresaEntity>>
    suspend fun getFirst(): EmpresaEntity?
}

class EmpresaRepositoryImpl(private val db: AppDatabase) : EmpresaRepository {
    override fun getAll(): Flow<List<EmpresaEntity>> = db.empresaDao().getAll()
    override suspend fun getFirst(): EmpresaEntity? = db.empresaDao().getFirst()
}

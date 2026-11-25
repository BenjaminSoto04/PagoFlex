package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del repositorio de empresas. Room es la fuente local y el
 * microservicio de empresas (puerto 8085) se sincroniza cuando esta disponible.
 */
interface EmpresaRepository {
    fun getAll(): Flow<List<EmpresaEntity>>
    suspend fun getFirst(): EmpresaEntity?
    suspend fun getCodigo(codigo: String): EmpresaEntity?
    suspend fun getByRut(rut: String): EmpresaEntity?
    suspend fun getById(id: Long): EmpresaEntity?
    suspend fun insert(empresa: EmpresaEntity)
    suspend fun update(empresa: EmpresaEntity)
    suspend fun delete(id: Long)
}

package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pagoflex.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {
    @Query("SELECT * FROM empresas")
    fun getAll(): Flow<List<EmpresaEntity>>

    @Query("SELECT * FROM empresas WHERE id = :id")
    suspend fun getById(id: Long): EmpresaEntity?

    @Query("SELECT * FROM empresas WHERE codigo = :codigo")
    suspend fun getByCodigo(codigo: String): EmpresaEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: EmpresaEntity)
}
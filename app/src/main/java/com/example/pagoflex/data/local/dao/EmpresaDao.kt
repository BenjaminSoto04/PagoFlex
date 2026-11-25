package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pagoflex.data.local.entity.EmpresaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {
    @Query("SELECT * FROM empresas")
    fun getAll(): Flow<List<EmpresaEntity>>

    @Query("SELECT * FROM empresas")
    suspend fun getAllSync(): List<EmpresaEntity>

    @Query("SELECT * FROM empresas WHERE id = :id")
    suspend fun getById(id: Long): EmpresaEntity?

    @Query("SELECT * FROM empresas ORDER BY id ASC LIMIT 1")
    suspend fun getFirst(): EmpresaEntity?

    @Query("SELECT * FROM empresas WHERE codigo = :codigo")
    suspend fun getByCodigo(codigo: String): EmpresaEntity?

    @Query("SELECT * FROM empresas WHERE rut = :rut")
    suspend fun getByRut(rut: String): EmpresaEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(e: EmpresaEntity)

    /** Inserta o reemplaza la fila (usado al sincronizar con el microservicio). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: EmpresaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(empresas: List<EmpresaEntity>)

    @Update
    suspend fun update(e: EmpresaEntity)

    @Query("DELETE FROM empresas WHERE id = :id")
    suspend fun deleteById(id: Long)
}

package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pagoflex.data.local.entity.CompromisoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompromisoDao {
    @Query("SELECT * FROM compromisos")
    fun getAll(): Flow<List<CompromisoEntity>>

    @Query("SELECT * FROM compromisos WHERE idUsuario = :userId")
    fun getByUsuario(userId: Long): Flow<List<CompromisoEntity>>

    @Query("SELECT * FROM compromisos WHERE id = :id")
    suspend fun getById(id: Long): CompromisoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(c: CompromisoEntity)

    @Update
    suspend fun update(c: CompromisoEntity)

    @Query("UPDATE compromisos SET estado = :estado WHERE id = :id")
    suspend fun updateEstado(id: Long, estado: String)

    @Query("DELETE FROM compromisos WHERE id = :id")
    suspend fun deleteById(id: Long)
}

package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pagoflex.data.local.entity.ConversacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversacionDao {
    @Query("SELECT * FROM conversaciones WHERE idUsuario = :userId")
    fun getByUsuario(userId: Long): Flow<List<ConversacionEntity>>

    @Query("SELECT * FROM conversaciones WHERE idUsuario = :userId LIMIT 1")
    suspend fun getByUsuarioOne(userId: Long): ConversacionEntity?

    @Query("SELECT * FROM conversaciones")
    fun getAll(): Flow<List<ConversacionEntity>>

    @Insert
    suspend fun insert(c: ConversacionEntity): Long

    @Query("DELETE FROM conversaciones WHERE idUsuario = :userId")
    suspend fun deleteByUsuario(userId: Long)
}
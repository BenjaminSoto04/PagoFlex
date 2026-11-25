package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pagoflex.data.local.entity.ConversacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversacionDao {
    @Query("SELECT * FROM conversaciones WHERE idUsuario = :userId ORDER BY id DESC")
    fun getByUsuario(userId: Long): Flow<List<ConversacionEntity>>

    @Query("SELECT * FROM conversaciones WHERE idUsuario = :userId ORDER BY id DESC LIMIT 1")
    suspend fun getByUsuarioOne(userId: Long): ConversacionEntity?

    @Query("SELECT * FROM conversaciones WHERE idUsuario = :userId ORDER BY id DESC LIMIT 1")
    fun observeByUsuario(userId: Long): Flow<ConversacionEntity?>

    @Query("SELECT * FROM conversaciones WHERE id = :convId LIMIT 1")
    fun observeById(convId: Long): Flow<ConversacionEntity?>

    @Query("SELECT * FROM conversaciones WHERE id = :convId LIMIT 1")
    suspend fun getById(convId: Long): ConversacionEntity?

    @Query("SELECT * FROM conversaciones ORDER BY id DESC")
    fun getAll(): Flow<List<ConversacionEntity>>

    @Insert
    suspend fun insert(c: ConversacionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(c: ConversacionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(c: ConversacionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<ConversacionEntity>)

    @Update
    suspend fun update(c: ConversacionEntity)

    @Query("UPDATE conversaciones SET estado = :estado WHERE id = :convId")
    suspend fun actualizarEstado(convId: Long, estado: String)

    @Query("DELETE FROM conversaciones WHERE idUsuario = :userId")
    suspend fun deleteByUsuario(userId: Long)
}
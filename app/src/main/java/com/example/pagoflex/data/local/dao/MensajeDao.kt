package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pagoflex.data.local.entity.MensajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MensajeDao {
    @Query("SELECT * FROM mensajes WHERE idConversacion = :convId ORDER BY id ASC")
    fun getByConversacion(convId: Long): Flow<List<MensajeEntity>>

    @Insert
    suspend fun insert(m: MensajeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(m: MensajeEntity): Long

    @Update
    suspend fun update(m: MensajeEntity)

    @Query("SELECT COUNT(*) FROM mensajes WHERE idConversacion = :convId AND rolRemitente = 'SISTEMA'")
    suspend fun countSistema(convId: Long): Int

    @Query("DELETE FROM mensajes WHERE idConversacion IN (SELECT id FROM conversaciones WHERE idUsuario = :userId)")
    suspend fun deleteByUsuario(userId: Long)
}
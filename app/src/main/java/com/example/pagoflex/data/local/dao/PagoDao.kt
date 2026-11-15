package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pagoflex.data.local.entity.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoDao {
    @Query("SELECT * FROM pagos")
    fun getAll(): Flow<List<PagoEntity>>

    @Query("SELECT * FROM pagos")
    suspend fun getAllSync(): List<PagoEntity>

    @Query("SELECT * FROM pagos WHERE idCompromiso = :idCompromiso ORDER BY id DESC LIMIT 1")
    suspend fun getByCompromiso(idCompromiso: Long): PagoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: PagoEntity)

    @Update
    suspend fun update(p: PagoEntity)
}

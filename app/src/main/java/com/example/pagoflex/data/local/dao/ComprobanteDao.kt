package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pagoflex.data.local.entity.ComprobanteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComprobanteDao {
    @Insert
    suspend fun insert(c: ComprobanteEntity): Long

    @Query("SELECT * FROM comprobantes WHERE idPago = :idPago")
    suspend fun getByPago(idPago: Long): ComprobanteEntity?

    @Query("SELECT * FROM comprobantes")
    fun getAll(): Flow<List<ComprobanteEntity>>
}

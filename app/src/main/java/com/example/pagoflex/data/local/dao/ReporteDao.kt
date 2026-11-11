package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pagoflex.data.local.entity.ReporteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReporteDao {
    @Insert
    suspend fun insert(r: ReporteEntity): Long

    @Query("SELECT * FROM reportes ORDER BY id DESC")
    fun getAll(): Flow<List<ReporteEntity>>
}

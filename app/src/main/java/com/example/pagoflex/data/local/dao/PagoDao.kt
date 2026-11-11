package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pagoflex.data.local.entity.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoDao {
    @Query("SELECT * FROM pagos")
    fun getAll(): Flow<List<PagoEntity>>

    @Query("SELECT * FROM pagos WHERE idCompromiso = :idCompromiso")
    suspend fun getByCompromiso(idCompromiso: Long): PagoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(p: PagoEntity)

    @Query("UPDATE pagos SET fotoUri = :uri WHERE id = :id")
    suspend fun updateFoto(id: Long, uri: String)
}

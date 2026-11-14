package com.example.pagoflex.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pagoflex.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM usuarios")
    fun getAll(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun count(): Int

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query(
        "SELECT * FROM usuarios WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) " +
                "OR LOWER(TRIM(codigo)) = LOWER(TRIM(:email)) LIMIT 1"
    )
    suspend fun getByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("UPDATE usuarios SET passwordHash = :hash, salt = :salt WHERE id = :id")
    suspend fun updatePassword(id: Long, hash: String, salt: String)

    @Query("UPDATE usuarios SET fotoUri = :fotoUri WHERE id = :id")
    suspend fun updateFoto(id: Long, fotoUri: String?)

    /** Ids de los usuarios mock creados por versiones anteriores de la app. */
    @Query("SELECT id FROM usuarios WHERE codigo LIKE 'DEMO-%'")
    suspend fun getIdsUsuariosMock(): List<Long>

    /** Elimina los usuarios mock creados por versiones anteriores de la app. */
    @Query("DELETE FROM usuarios WHERE codigo LIKE 'DEMO-%'")
    suspend fun deleteUsuariosMock(): Int

    @Query("DELETE FROM usuarios WHERE id = :id")
    suspend fun deleteById(id: Long)
}
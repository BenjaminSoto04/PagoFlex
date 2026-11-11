
package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun register(nombre: String, email: String, password: String, rol: String, telefono: String? = null, empresa: String? = null): Result<Long>
    suspend fun login(email: String, password: String): Result<UserEntity>
    suspend fun getById(id: Long): UserEntity?
    suspend fun getByEmail(email: String): UserEntity?
    fun getAll(): kotlinx.coroutines.flow.Flow<List<UserEntity>>
    suspend fun updateProfile(user: UserEntity)
    suspend fun updateFoto(id: Long, fotoUri: String?)
    suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit>
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit>
}

class UserRepositoryImpl(private val db: AppDatabase) : UserRepository {
    override suspend fun register(nombre: String, email: String, password: String, rol: String, telefono: String?, empresa: String?): Result<Long> {
        val existing = db.userDao().getByEmail(email)
        if (existing != null) return Result.failure(IllegalArgumentException("El correo ya está registrado"))
        val salt = com.example.pagoflex.security.PasswordHasher.generateSalt()
        val hash = com.example.pagoflex.security.PasswordHasher.hash(password, salt)
        val id = db.userDao().insert(UserEntity(nombre = nombre, email = email, passwordHash = hash, salt = salt, rol = rol, telefono = telefono, empresa = empresa))
        return Result.success(id)
    }

    override suspend fun login(email: String, password: String): Result<UserEntity> {
        val user = db.userDao().getByEmail(email.trim().lowercase()) ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        if (!com.example.pagoflex.security.PasswordHasher.verify(password, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Contraseña incorrecta"))
        }
        return Result.success(user)
    }

    override suspend fun getById(id: Long): UserEntity? = db.userDao().getById(id)
    override suspend fun getByEmail(email: String): UserEntity? = db.userDao().getByEmail(email)
    override fun getAll(): kotlinx.coroutines.flow.Flow<List<UserEntity>> = db.userDao().getAll()
    override suspend fun updateProfile(user: UserEntity) = db.userDao().update(user)
    override suspend fun updateFoto(id: Long, fotoUri: String?) = db.userDao().updateFoto(id, fotoUri)
    override suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit> {
        val user = db.userDao().getById(id) ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        if (!com.example.pagoflex.security.PasswordHasher.verify(oldPassword, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Contraseña actual incorrecta"))
        }
        val salt = com.example.pagoflex.security.PasswordHasher.generateSalt()
        val hash = com.example.pagoflex.security.PasswordHasher.hash(newPassword, salt)
        db.userDao().updatePassword(id, hash, salt)
        return Result.success(Unit)
    }
    override suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        val user = db.userDao().getByEmail(email) ?: return Result.failure(IllegalArgumentException("Correo no registrado"))
        val salt = com.example.pagoflex.security.PasswordHasher.generateSalt()
        val hash = com.example.pagoflex.security.PasswordHasher.hash(newPassword, salt)
        db.userDao().updatePassword(user.id, hash, salt)
        return Result.success(Unit)
    }
}

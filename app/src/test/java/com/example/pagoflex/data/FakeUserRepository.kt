package com.example.pagoflex.data

import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeUserRepository : UserRepository {
    var loginLlamado = false
    var loginResult: Result<UserEntity> = Result.success(
        UserEntity(id = 1L, nombre = "Test", email = "test@test.cl", rol = "USUARIO", activo = true)
    )
    var registerResult: Result<Long> = Result.success(1L)
    var changePasswordResult: Result<Unit> = Result.success(Unit)
    var usuarioPorEmail: UserEntity? = null
    var usuarioGetById: UserEntity? = null

    override suspend fun register(
        nombre: String,
        email: String,
        password: String,
        rol: String,
        telefono: String?,
        empresa: String?,
        preguntaSeguridad: String,
        respuestaSeguridad: String
    ): Result<Long> = registerResult

    override suspend fun login(email: String, password: String): Result<UserEntity> {
        loginLlamado = true
        return loginResult
    }

    override suspend fun getById(id: Long): UserEntity? = usuarioGetById

    override suspend fun getByEmail(email: String): UserEntity? = usuarioPorEmail

    override fun getAll(): Flow<List<UserEntity>> = flowOf(emptyList())

    override suspend fun updateProfile(user: UserEntity) {}

    override suspend fun updateRol(id: Long, rol: String): Result<Unit> = Result.success(Unit)

    override suspend fun updateRolConEmpresa(id: Long, empresaId: Long): Result<Unit> = Result.success(Unit)

    override suspend fun updateFoto(id: Long, fotoUri: String?) {}

    override suspend fun updateActivo(id: Long, activo: Boolean): Result<Unit> = Result.success(Unit)

    override suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit> =
        changePasswordResult

    override suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = Result.success(Unit)
}

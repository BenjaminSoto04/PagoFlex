package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.PreguntasSeguridad
import com.example.pagoflex.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    companion object {
        const val MENSAJE_CUENTA_DESHABILITADA =
            "Esta cuenta ha sido deshabilitada. Contacta a un agente o administrador."
    }

    suspend fun register(
        nombre: String,
        email: String,
        password: String,
        rol: String,
        telefono: String? = null,
        empresa: String? = null,
        preguntaSeguridad: String = PreguntasSeguridad.PREGUNTA_DEFAULT,
        respuestaSeguridad: String = PreguntasSeguridad.RESPUESTA_DEFAULT
    ): Result<Long>
    suspend fun login(email: String, password: String): Result<UserEntity>
    suspend fun getById(id: Long): UserEntity?
    suspend fun getByEmail(email: String): UserEntity?
    fun getAll(): Flow<List<UserEntity>>
    suspend fun updateProfile(user: UserEntity)
    suspend fun updateRol(id: Long, rol: String): Result<Unit>
    suspend fun updateFoto(id: Long, fotoUri: String?)
    suspend fun updateActivo(id: Long, activo: Boolean): Result<Unit>
    suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit>
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit>
}

class UserRepositoryImpl(private val db: AppDatabase) : UserRepository {
    override suspend fun register(
        nombre: String,
        email: String,
        password: String,
        rol: String,
        telefono: String?,
        empresa: String?,
        preguntaSeguridad: String,
        respuestaSeguridad: String
    ): Result<Long> {
        AppDatabase.awaitSeed()
        val correoNormalizado = email.trim().lowercase()
        val existing = db.userDao().getByEmail(correoNormalizado)
        if (existing != null) return Result.failure(IllegalArgumentException("El correo ya está registrado"))
        val salt = com.example.pagoflex.security.PasswordHasher.generateSalt()
        val hash = com.example.pagoflex.security.PasswordHasher.hash(password, salt)
        // El registro publico siempre crea usuarios. Los roles ejecutivo/agente
        // solo se asignan desde la gestion de roles del agente.
        val rolAsignado = "USUARIO"
        val agentePorDefecto = db.userDao().getDefaultByRol("AGENTE")?.id
        val id = db.userDao().insert(
            UserEntity(
                nombre = nombre.trim(),
                email = correoNormalizado,
                passwordHash = hash,
                salt = salt,
                rol = rolAsignado,
                telefono = telefono ?: "+56 9 00000000",
                rut = null,
                comuna = "Santiago",
                fechaIncorporacion = "01-01-2024",
                empresa = empresa,
                agenteAsignadoId = agentePorDefecto,
                preguntaSeguridad = preguntaSeguridad.trim(),
                respuestaSeguridad = PreguntasSeguridad.normalizarRespuesta(respuestaSeguridad)
            )
        )
        return Result.success(id)
    }

    override suspend fun login(email: String, password: String): Result<UserEntity> {
        AppDatabase.awaitSeed()
        val user = db.userDao().getByEmail(email.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        if (!user.activo) {
            return Result.failure(IllegalStateException(UserRepository.MENSAJE_CUENTA_DESHABILITADA))
        }
        if (!com.example.pagoflex.security.PasswordHasher.verify(password, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Contraseña incorrecta"))
        }
        return Result.success(user)
    }

    override suspend fun getById(id: Long): UserEntity? {
        AppDatabase.awaitSeed()
        return db.userDao().getById(id)
    }

    override suspend fun getByEmail(email: String): UserEntity? = db.userDao().getByEmail(email.trim())

    override fun getAll(): Flow<List<UserEntity>> = db.userDao().getAll()

    override suspend fun updateProfile(user: UserEntity) = db.userDao().update(user)

    /**
     * Cambia el rol de un usuario y completa los datos minimos que cada rol
     * necesita para que sus pantallas funcionen al iniciar sesion.
     */
    override suspend fun updateRol(id: Long, rol: String): Result<Unit> {
        val user = db.userDao().getById(id)
            ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        val nuevoRol = rol.trim().uppercase()
        if (nuevoRol !in setOf("USUARIO", "EJECUTIVO", "AGENTE")) {
            return Result.failure(IllegalArgumentException("Rol invalido"))
        }

        val actualizado = when (nuevoRol) {
            "EJECUTIVO" -> {
                val empresa = user.empresaId?.let { db.empresaDao().getById(it) }
                    ?: db.empresaDao().getFirst()
                user.copy(
                    rol = nuevoRol,
                    empresaId = empresa?.id ?: user.empresaId,
                    empresa = empresa?.nombreFantasia ?: user.empresa,
                    area = user.area?.takeIf { it.isNotBlank() } ?: "Cobranza",
                    cargo = user.cargo?.takeIf { it.isNotBlank() } ?: "Ejecutivo"
                )
            }

            "AGENTE" -> user.copy(
                rol = nuevoRol,
                empresaId = null,
                empresa = null,
                area = user.area?.takeIf { it.isNotBlank() } ?: "Soporte",
                cargo = user.cargo?.takeIf { it.isNotBlank() } ?: "Agente de soporte",
                agenteAsignadoId = null
            )

            else -> {
                val agente = user.agenteAsignadoId
                    ?: db.userDao().getDefaultByRol("AGENTE")?.id
                user.copy(rol = nuevoRol, agenteAsignadoId = agente)
            }
        }
        db.userDao().update(actualizado)
        return Result.success(Unit)
    }

    override suspend fun updateFoto(id: Long, fotoUri: String?) = db.userDao().updateFoto(id, fotoUri)

    override suspend fun updateActivo(id: Long, activo: Boolean): Result<Unit> {
        val user = db.userDao().getById(id)
            ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        db.userDao().updateActivo(user.id, activo)
        return Result.success(Unit)
    }

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
        val user = db.userDao().getByEmail(email.trim())
            ?: return Result.failure(IllegalArgumentException("Correo no registrado"))
        val salt = com.example.pagoflex.security.PasswordHasher.generateSalt()
        val hash = com.example.pagoflex.security.PasswordHasher.hash(newPassword, salt)
        db.userDao().updatePassword(user.id, hash, salt)
        return Result.success(Unit)
    }
}
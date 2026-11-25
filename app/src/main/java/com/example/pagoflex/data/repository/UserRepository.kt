package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.PreguntasSeguridad
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.model.ApiUsuarioLoginResponse
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.UsuariosApiService
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
        respuestaSeguridad: String = PreguntasSeguridad.RESPUESTA_DEFAULT,
        rut: String? = null,
        comuna: String? = null
    ): Result<Long>
    suspend fun login(email: String, password: String): Result<UserEntity>
    suspend fun getById(id: Long): UserEntity?
    suspend fun getByEmail(email: String): UserEntity?
    fun getAll(): Flow<List<UserEntity>>
    suspend fun updateProfile(user: UserEntity)
    suspend fun updateRol(id: Long, rol: String): Result<Unit>
    suspend fun updateRolConEmpresa(id: Long, empresaId: Long): Result<Unit>
    suspend fun updateFoto(id: Long, fotoUri: String?)
    suspend fun updateActivo(id: Long, activo: Boolean): Result<Unit>
    suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit>
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit>
}

class UserRepositoryImpl(private val db: AppDatabase, private val apiService: UsuariosApiService = RetrofitClient.obtenerUsuarios()) : UserRepository {

    override suspend fun register(
        nombre: String,
        email: String,
        password: String,
        rol: String,
        telefono: String?,
        empresa: String?,
        preguntaSeguridad: String,
        respuestaSeguridad: String,
        rut: String?,
        comuna: String?
    ): Result<Long> {
        // Solo usamos la API Spring Boot sin fallback a Room
        val request = com.example.pagoflex.data.model.ApiUsuarioRegistroRequest(
            nombre = nombre.trim(),
            rut = rut ?: "",
            email = email.trim().lowercase(),
            password = password,
            telefono = telefono ?: "+56 9 00000000",
            comuna = comuna ?: "Santiago",
            empresa = empresa ?: "",
            preguntaSeguridad = preguntaSeguridad.trim(),
            respuestaSeguridad = PreguntasSeguridad.normalizarRespuesta(respuestaSeguridad)
        )

        return try {
            val response = apiService.registrar(request)
            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                // La API devuelve id=0, usamos el ID local para Room pero solo como referencia
                Result.success(0L)
            } else {
                // La API responde con código de error
                Result.failure(Exception("Error en registro: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<UserEntity> {
        // Solo usamos la API Spring Boot sin fallback a Room
        val loginRequest = com.example.pagoflex.data.model.ApiUsuarioLoginRequest(
            email = email.trim().lowercase(),
            password = password
        )

        return try {
            val response = apiService.login(loginRequest)
            if (response.isSuccessful && response.body() != null) {
                Result.success(ApiResponseToEntityMapper.toUserEntity(response.body() ?: ApiUsuarioLoginResponse.empty()))
            } else {
                // La API devuelve errores con códigos específicos, mapeamos a excepciones
                val codigo = response.code()
                when (codigo) {
                    401 -> Result.failure(IllegalArgumentException("Credenciales incorrectas"))
                    403 -> Result.failure(IllegalStateException(UserRepository.MENSAJE_CUENTA_DESHABILITADA))
                    else -> Result.failure(Exception("Error en login: $codigo"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getById(id: Long): UserEntity? {
        // Intentar API primero, luego fallback a Room
        try {
            val response = apiService.obtenerPorId(id)
            if (response.isSuccessful && response.body() != null) {
                return ApiResponseToEntityMapper.toUserEntity(response.body() ?: ApiUsuarioLoginResponse.empty())
            }
        } catch (e: Exception) {
            // Ignorar fallo de API
            e.printStackTrace()
        }

        return db.userDao().getById(id)
    }

    override suspend fun getByEmail(email: String): UserEntity? =
        try {
            val response = apiService.obtenerPorCorreo(email.trim())
            if (response.isSuccessful && response.body() != null) {
                ApiResponseToEntityMapper.toUserEntity(response.body() ?: ApiUsuarioLoginResponse.empty())
            } else {
                db.userDao().getByEmail(email.trim())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            db.userDao().getByEmail(email.trim())
        }

    override fun getAll(): Flow<List<UserEntity>> = db.userDao().getAll()

    override suspend fun updateProfile(user: UserEntity) {
        // Para simplicidad, actualizamos directamente en Room (la API no tiene un endpoint PUT parcial)
        // Esto es porque el perfil del usuario requiere actualización completa con los datos de la sesión
        try {
            val actualizado = user.copy(
                fechaIncorporacion = if (user.fechaIncorporacion.isNullOrEmpty()) "01-01-2024" else user.fechaIncorporacion,
                avisosDias = if (user.avisosDias == null) 3 else user.avisosDias,
                activo = true // Mantenemos activo por defecto
            )
            db.userDao().update(actualizado)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateRol(id: Long, rol: String): Result<Unit> {
        val user = getById(id) ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        val nuevoRol = rol.trim().uppercase()
        if (nuevoRol !in setOf("USUARIO", "EJECUTIVO", "AGENTE")) {
            return Result.failure(IllegalArgumentException("Rol invalido"))
        }

        // Actualizamos directamente en Room, ya que la API requiere llamada separada para asignar empresa
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

    override suspend fun updateRolConEmpresa(id: Long, empresaId: Long): Result<Unit> {
        val user = getById(id) ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        val empresa = db.empresaDao().getById(empresaId)
            ?: return Result.failure(IllegalArgumentException("Empresa no encontrada"))

        val actualizado = user.copy(
            rol = "EJECUTIVO",
            empresaId = empresa.id,
            empresa = empresa.nombreFantasia,
            area = user.area?.takeIf { it.isNotBlank() } ?: "Cobranza",
            cargo = user.cargo?.takeIf { it.isNotBlank() } ?: "Ejecutivo"
        )
        db.userDao().update(actualizado)
        return Result.success(Unit)
    }

    override suspend fun updateFoto(id: Long, fotoUri: String?) {
        val existingUser = getById(id) ?: return
        val actualizado = existingUser.copy(fotoUri = fotoUri)
        db.userDao().update(actualizado)
    }

    override suspend fun updateActivo(id: Long, activo: Boolean): Result<Unit> {
        val user = getById(id) ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        db.userDao().updateActivo(user.id, activo)
        return Result.success(Unit)
    }

    override suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit> {
        val user = getById(id) ?: return Result.failure(IllegalArgumentException("Usuario no encontrado"))
        if (!com.example.pagoflex.security.PasswordHasher.verify(oldPassword, user.salt, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Contraseña actual incorrecta"))
        }
        val salt = com.example.pagoflex.security.PasswordHasher.generateSalt()
        val hash = com.example.pagoflex.security.PasswordHasher.hash(newPassword, salt)
        db.userDao().updatePassword(id, hash, salt)
        return Result.success(Unit)
    }

    override suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        val user = getByEmail(email.trim()) ?: return Result.failure(IllegalArgumentException("Correo no registrado"))
        val salt = com.example.pagoflex.security.PasswordHasher.generateSalt()
        val hash = com.example.pagoflex.security.PasswordHasher.hash(newPassword, salt)
        db.userDao().updatePassword(user.id, hash, salt)
        return Result.success(Unit)
    }

    // Objeto auxiliar para convertir respuestas API a UserEntity
    private object ApiResponseToEntityMapper {
        fun toUserEntity(response: com.example.pagoflex.data.model.ApiUsuarioLoginResponse): UserEntity {
            val preguntaSeguridad = if (response.preguntaSeguridad.isNullOrEmpty())
                PreguntasSeguridad.PREGUNTA_DEFAULT
            else response.preguntaSeguridad

            return UserEntity(
                id = response.id,
                codigo = response.codigo,
                nombre = response.nombre,
                rut = response.rut ?: "",
                email = response.email,
                passwordHash = "", // La API no envía contraseña hashada, usaremos una por defecto o hash del email
                salt = "",
                rol = response.rol,
                telefono = response.telefono ?: "+56 9 00000000",
                comuna = response.comuna ?: "Santiago",
                empresaId = response.empresaId,
                area = response.area,
                cargo = response.cargo,
                activo = response.activo,
                fechaIncorporacion = response.fechaIncorporacion,
                avisosDias = if (response.avisosDias == null) 3 else response.avisosDias,
                avisosActivos = if (response.avisosActivos) true else false, // Default activo
                empresa = response.empresa ?: "",
                agenteAsignadoId = response.agenteAsignadoId,
                fotoUri = response.fotoUri,
                preguntaSeguridad = preguntaSeguridad
            )
        }
    }

}

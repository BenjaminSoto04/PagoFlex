package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.PreguntasSeguridad
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.model.ApiUsuarioActivoRequest
import com.example.pagoflex.data.model.ApiUsuarioLoginRequest
import com.example.pagoflex.data.model.ApiUsuarioLoginResponse
import com.example.pagoflex.data.model.ApiUsuarioPerfilRequest
import com.example.pagoflex.data.model.ApiUsuarioRegistroRequest
import com.example.pagoflex.data.model.ApiUsuarioRolRequest
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.UsuariosApiService
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch

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
    suspend fun updateRolConEmpresa(id: Long, empresaId: Long): Result<Unit>
    suspend fun updateFoto(id: Long, fotoUri: String?)
    suspend fun updateActivo(id: Long, activo: Boolean): Result<Unit>
    suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit>
    suspend fun resetPassword(email: String, newPassword: String): Result<Unit>
}

class UserRepositoryImpl(
    private val apiService: UsuariosApiService = RetrofitClient.obtenerUsuarios()
) : UserRepository {

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
        val request = com.example.pagoflex.data.model.ApiUsuarioRegistroRequest(
            nombre = nombre.trim(),
            rut = "",
            email = email.trim().lowercase(),
            password = password,
            telefono = telefono ?: "+56 9 00000000",
            comuna = "Santiago",
            empresa = empresa ?: "",
            preguntaSeguridad = preguntaSeguridad.trim(),
            respuestaSeguridad = PreguntasSeguridad.normalizarRespuesta(respuestaSeguridad)
        )

        return try {
            val response = apiService.registrar(request)
            if (!response.isSuccessful) {
                val mensaje = response.errorBody()?.string().orEmpty()
                return Result.failure(Exception(mensaje.ifBlank { "Error del servidor (${response.code()})" }))
            }
            val body = response.body()
                ?: return Result.failure(Exception("Respuesta vacia del servidor"))
            Result.success(body.id)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<UserEntity> {
        return try {
            val loginRequest = com.example.pagoflex.data.model.ApiUsuarioLoginRequest(
                email = email.trim().lowercase(),
                password = password
            )
            val response = apiService.login(loginRequest)
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(IllegalArgumentException("Usuario no encontrado"))
            }
            Result.success(ApiResponseToEntityMapper.toUserEntity(response.body()!!))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getById(id: Long): UserEntity? {
        return try {
            val response = apiService.obtenerPorId(id)
            if (response.isSuccessful && response.body() != null) {
                ApiResponseToEntityMapper.toUserEntity(response.body()!!)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getByEmail(email: String): UserEntity? {
        return try {
            val response = apiService.obtenerPorCorreo(email.trim())
            if (response.isSuccessful && response.body() != null) {
                ApiResponseToEntityMapper.toUserEntity(response.body()!!)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun getAll(): Flow<List<UserEntity>> = flow {
        val response = apiService.listarTodos()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.map(ApiResponseToEntityMapper::toUserEntity))
        } else {
            emit(emptyList())
        }
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(emptyList())
    }

    override suspend fun updateProfile(user: UserEntity) {
        val request = com.example.pagoflex.data.model.ApiUsuarioPerfilRequest(
            nombre = user.nombre,
            email = user.email,
            telefono = user.telefono,
            preguntaSeguridad = user.preguntaSeguridad,
            respuestaSeguridad = user.respuestaSeguridad,
            fotoUri = user.fotoUri
        )
        try {
            apiService.actualizarPerfil(user.id, request)
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

        val empresaId = if (nuevoRol == "EJECUTIVO") {
            try {
                RetrofitClient.obtenerEmpresas().listarTodos().body()?.firstOrNull()?.id
            } catch (e: Exception) {
                null
            }
        } else null

        val empresa = if (nuevoRol == "EJECUTIVO") {
            try {
                RetrofitClient.obtenerEmpresas().listarTodos().body()?.firstOrNull()?.nombreFantasia
            } catch (e: Exception) {
                null
            }
        } else null

        return try {
            val response = apiService.cambiarRol(
                id,
                com.example.pagoflex.data.model.ApiUsuarioRolRequest(
                    rol = nuevoRol,
                    empresaId = empresaId,
                    empresa = empresa
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Error del servidor (${response.code()})"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateRolConEmpresa(id: Long, empresaId: Long): Result<Unit> {
        val empresaResponse = try {
            RetrofitClient.obtenerEmpresas().obtenerPorId(empresaId)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
        if (!empresaResponse.isSuccessful || empresaResponse.body() == null) {
            return Result.failure(Exception("Empresa no encontrada"))
        }
        val empresa = empresaResponse.body()!!
        return try {
            val response = apiService.cambiarRol(
                id,
                com.example.pagoflex.data.model.ApiUsuarioRolRequest(
                    rol = "EJECUTIVO",
                    empresaId = empresa.id,
                    empresa = empresa.nombreFantasia
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Error del servidor (${response.code()})"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateFoto(id: Long, fotoUri: String?) {
        val user = getById(id) ?: return
        val request = com.example.pagoflex.data.model.ApiUsuarioPerfilRequest(
            nombre = user.nombre,
            email = user.email,
            telefono = user.telefono,
            preguntaSeguridad = user.preguntaSeguridad,
            respuestaSeguridad = user.respuestaSeguridad,
            fotoUri = fotoUri
        )
        try {
            apiService.actualizarPerfil(id, request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun updateActivo(id: Long, activo: Boolean): Result<Unit> {
        return try {
            val response = apiService.cambiarActivo(id, com.example.pagoflex.data.model.ApiUsuarioActivoRequest(activo))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Error del servidor (${response.code()})"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun changePassword(id: Long, oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val response = apiService.cambiarContrasena(id, com.example.pagoflex.data.model.ApiUsuarioCambioContrasenaRequest(oldPassword, newPassword))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Error del servidor (${response.code()})"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        return try {
            val response = apiService.resetContrasena(com.example.pagoflex.data.model.ApiUsuarioResetContrasenaRequest(email.trim(), newPassword))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Error del servidor (${response.code()})"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private object ApiResponseToEntityMapper {
        fun toUserEntity(response: com.example.pagoflex.data.model.ApiUsuarioLoginResponse): UserEntity {
            val preguntaSeguridad = if (response.preguntaSeguridad.isNullOrEmpty())
                PreguntasSeguridad.PREGUNTA_DEFAULT
            else response.preguntaSeguridad

            return UserEntity(
                id = response.id,
                codigo = response.codigo,
                nombre = response.nombre,
                rut = response.rut,
                email = response.email,
                passwordHash = "",
                salt = "",
                rol = response.rol,
                telefono = response.telefono,
                comuna = response.comuna,
                empresaId = response.empresaId,
                area = response.area,
                cargo = response.cargo,
                activo = response.activo,
                fechaIncorporacion = response.fechaIncorporacion,
                avisosDias = if (response.avisosDias == null) 3 else response.avisosDias,
                avisosActivos = if (response.avisosActivos) true else false,
                empresa = response.empresa,
                agenteAsignadoId = response.agenteAsignadoId,
                fotoUri = response.fotoUri,
                preguntaSeguridad = preguntaSeguridad
            )
        }
    }
}




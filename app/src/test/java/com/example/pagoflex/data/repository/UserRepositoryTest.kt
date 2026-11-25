package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.model.ApiUsuarioLoginResponse
import com.example.pagoflex.data.remote.UsuariosApiService
import com.example.pagoflex.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class UserRepositoryTest {

    @Test
    fun login_passwordCorrecta_retornaUsuario() = runBlocking {
        val api = FakeUsuariosApiService()
        val repo = UserRepositoryImpl(api)

        val result = repo.login("test@test.cl", "Password123!")

        assertTrue(result.isSuccess)
        assertEquals("test@test.cl", result.getOrNull()?.email)
    }

    @Test
    fun register_correoDuplicado_retornaFailure() = runBlocking {
        val api = FakeUsuariosApiService()
        val repo = UserRepositoryImpl(api)

        val result = repo.register(
            nombre = "Otro",
            email = "dup@test.cl",
            password = "Password123!",
            rol = "USUARIO"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun changePassword_passwordActualIncorrecta_retornaFailure() = runBlocking {
        val api = FakeUsuariosApiService()
        val repo = UserRepositoryImpl(api)

        val result = repo.changePassword(1L, "WrongPass123!", "NewPass123!")

        assertTrue(result.isFailure)
    }
}

private class FakeUsuariosApiService : UsuariosApiService {
    override suspend fun registrar(request: com.example.pagoflex.data.model.ApiUsuarioRegistroRequest): Response<com.example.pagoflex.data.model.ApiUsuarioRegistroResponse> {
        return Response.error(409, "correo duplicado".toResponseBody(null))
    }

    override suspend fun login(request: com.example.pagoflex.data.model.ApiUsuarioLoginRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(
            ApiUsuarioLoginResponse(
                id = 1L,
                nombre = "Test",
                email = "test@test.cl",
                rol = "USUARIO",
                activo = true
            )
        )

    override suspend fun listarTodos(): Response<List<ApiUsuarioLoginResponse>> =
        Response.success(emptyList())

    override suspend fun obtenerPorId(id: Long): Response<ApiUsuarioLoginResponse> =
        Response.error(404, "not found".toResponseBody(null))

    override suspend fun obtenerPorCorreo(email: String): Response<ApiUsuarioLoginResponse> =
        Response.error(404, "not found".toResponseBody(null))

    override suspend fun actualizarPerfil(id: Long, request: com.example.pagoflex.data.model.ApiUsuarioPerfilRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun cambiarRol(id: Long, request: com.example.pagoflex.data.model.ApiUsuarioRolRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun cambiarActivo(id: Long, request: com.example.pagoflex.data.model.ApiUsuarioActivoRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun cambiarContrasena(id: Long, request: com.example.pagoflex.data.model.ApiUsuarioCambioContrasenaRequest): Response<Unit> =
        Response.error(400, "incorrecta".toResponseBody(null))

    override suspend fun resetContrasena(request: com.example.pagoflex.data.model.ApiUsuarioResetContrasenaRequest): Response<Unit> =
        Response.success(Unit)
}

package com.example.pagoflex.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.UserEntity
import com.example.pagoflex.data.model.ApiUsuarioLoginResponse
import com.example.pagoflex.data.model.ApiUsuarioRegistroResponse
import com.example.pagoflex.data.remote.UsuariosApiService
import com.example.pagoflex.security.PasswordHasher
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: UserRepository

    @Before
    fun preparar() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = UserRepositoryImpl(db)
    }

    @Test
    fun login_passwordCorrecta_retornaUsuario() = runBlocking {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash("Password123!", salt)
        db.userDao().insert(
            UserEntity(nombre = "Test", email = "test@test.cl", passwordHash = hash, salt = salt, rol = "USUARIO")
        )

        val result = repo.login("test@test.cl", "Password123!")

        assertTrue(result.isSuccess)
        assertEquals("test@test.cl", result.getOrNull()?.email)
    }

    @Test
    fun register_correoDuplicado_retornaFailure() = runBlocking {
        val api = FakeUsuariosApiService()
        val repoConApi = UserRepositoryImpl(db, api)

        val result = repoConApi.register(
            nombre = "Otro",
            email = "dup@test.cl",
            password = "Password123!",
            rol = "USUARIO"
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun changePassword_passwordActualIncorrecta_retornaFailure() = runBlocking {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash("Password123!", salt)
        val id = db.userDao().insert(
            UserEntity(nombre = "Test", email = "cp@test.cl", passwordHash = hash, salt = salt, rol = "USUARIO")
        )

        val result = repo.changePassword(id, "WrongPass123!", "NewPass123!")

        assertTrue(result.isFailure)
    }
}

private class FakeUsuariosApiService : UsuariosApiService {
    override suspend fun registrar(request: com.example.pagoflex.data.model.ApiUsuarioRegistroRequest): Response<ApiUsuarioRegistroResponse> {
        return Response.error(409, "correo duplicado".toResponseBody(null))
    }

    override suspend fun login(request: com.example.pagoflex.data.model.ApiUsuarioLoginRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun listarTodos(): Response<List<ApiUsuarioLoginResponse>> =
        Response.success(emptyList())

    override suspend fun obtenerPorId(id: Long): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun obtenerPorCorreo(email: String): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun actualizarPerfil(id: Long, request: com.example.pagoflex.data.model.ApiUsuarioPerfilRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun cambiarRol(id: Long, request: com.example.pagoflex.data.model.ApiUsuarioRolRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())

    override suspend fun cambiarActivo(id: Long, request: com.example.pagoflex.data.model.ApiUsuarioActivoRequest): Response<ApiUsuarioLoginResponse> =
        Response.success(ApiUsuarioLoginResponse.empty())
}

package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiUsuarioActivoRequest
import com.example.pagoflex.data.model.ApiUsuarioLoginRequest
import com.example.pagoflex.data.model.ApiUsuarioLoginResponse
import com.example.pagoflex.data.model.ApiUsuarioPerfilRequest
import com.example.pagoflex.data.model.ApiUsuarioRegistroRequest
import com.example.pagoflex.data.model.ApiUsuarioRegistroResponse
import com.example.pagoflex.data.model.ApiUsuarioRolRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Microservicio de usuarios (PagoFlexMS).
 * Base URL: http://10.0.2.2:8083/
 *
 * Endpoints mapeados desde `UsuarioController` (`/api/usuarios`).
 */
interface UsuariosApiService {

    @POST("api/usuarios")
    suspend fun registrar(
        @Body request: ApiUsuarioRegistroRequest
    ): Response<ApiUsuarioRegistroResponse>

    @POST("api/usuarios/login")
    suspend fun login(
        @Body request: ApiUsuarioLoginRequest
    ): Response<ApiUsuarioLoginResponse>

    @GET("api/usuarios")
    suspend fun listarTodos(): Response<List<ApiUsuarioLoginResponse>>

    @GET("api/usuarios/{id}")
    suspend fun obtenerPorId(@Path("id") id: Long): Response<ApiUsuarioLoginResponse>

    @GET("api/usuarios/correo/{email}")
    suspend fun obtenerPorCorreo(@Path("email") email: String): Response<ApiUsuarioLoginResponse>

    @PUT("api/usuarios/{id}/perfil")
    suspend fun actualizarPerfil(
        @Path("id") id: Long,
        @Body request: ApiUsuarioPerfilRequest
    ): Response<ApiUsuarioLoginResponse>

    @PUT("api/usuarios/{id}/rol")
    suspend fun cambiarRol(
        @Path("id") id: Long,
        @Body request: ApiUsuarioRolRequest
    ): Response<ApiUsuarioLoginResponse>

    @PATCH("api/usuarios/{id}/activo")
    suspend fun cambiarActivo(
        @Path("id") id: Long,
        @Body request: ApiUsuarioActivoRequest
    ): Response<ApiUsuarioLoginResponse>
}

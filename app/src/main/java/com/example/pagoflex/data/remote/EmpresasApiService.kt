package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiEmpresaRegistroRequest
import com.example.pagoflex.data.model.ApiEmpresaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Microservicio de empresas (PagoFlexMS).
 * Base URL: http://10.0.2.2:8085/
 *
 * Endpoints mapeados desde `EmpresasController` (`/api/empresas`).
 */
interface EmpresasApiService {

    @POST("api/empresas")
    suspend fun crear(@Body request: ApiEmpresaRegistroRequest): Response<ApiEmpresaResponse>

    @GET("api/empresas")
    suspend fun listarTodos(): Response<List<ApiEmpresaResponse>>

    @GET("api/empresas/{id}")
    suspend fun obtenerPorId(@Path("id") id: Long): Response<ApiEmpresaResponse>

    @GET("api/empresas/codigo/{codigo}")
    suspend fun obtenerPorCodigo(@Path("codigo") codigo: String): Response<ApiEmpresaResponse>

    @GET("api/empresas/rut/{rut}")
    suspend fun obtenerPorRut(@Path("rut") rut: String): Response<ApiEmpresaResponse>

    @PUT("api/empresas/{id}")
    suspend fun actualizar(
        @Path("id") id: Long,
        @Body request: ApiEmpresaRegistroRequest
    ): Response<ApiEmpresaResponse>

    @DELETE("api/empresas/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}

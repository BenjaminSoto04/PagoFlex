package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiCompromisoActualizacionRequest
import com.example.pagoflex.data.model.ApiCompromisoPagoRequest
import com.example.pagoflex.data.model.ApiCompromisoRegistroRequest
import com.example.pagoflex.data.model.ApiCompromisoResponse
import com.example.pagoflex.data.model.ApiCompromisoVencimientoRequest
import com.example.pagoflex.data.model.ApiAnulacionRequest
import com.example.pagoflex.data.model.ApiAnulacionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Microservicio de compromisos (PagoFlexMS).
 * Base URL: http://10.0.2.2:8081/
 *
 * Endpoints mapeados desde `CompromisoController` (`/api/compromisos`).
 */
interface CompromisosApiService {

    @POST("api/compromisos")
    suspend fun registrar(@Body request: ApiCompromisoRegistroRequest): Response<ApiCompromisoResponse>

    @GET("api/compromisos")
    suspend fun listarTodos(): Response<List<ApiCompromisoResponse>>

    @GET("api/compromisos/{id}")
    suspend fun obtenerPorId(@Path("id") id: Int): Response<ApiCompromisoResponse>

    @GET("api/compromisos/folio/{folio}")
    suspend fun obtenerPorFolio(@Path("folio") folio: String): Response<ApiCompromisoResponse>

    @GET("api/compromisos/usuario/{usuarioId}")
    suspend fun listarPorUsuario(@Path("usuarioId") usuarioId: Int): Response<List<ApiCompromisoResponse>>

    @GET("api/compromisos/empresa/{empresaId}")
    suspend fun listarPorEmpresa(@Path("empresaId") empresaId: Int): Response<List<ApiCompromisoResponse>>

    @GET("api/compromisos/usuario/{usuarioId}/empresa/{empresaId}")
    suspend fun listarPorUsuarioYEmpresa(
        @Path("usuarioId") usuarioId: Int,
        @Path("empresaId") empresaId: Int
    ): Response<List<ApiCompromisoResponse>>

    @GET("api/compromisos/estado/{estado}")
    suspend fun listarPorEstado(@Path("estado") estado: String): Response<List<ApiCompromisoResponse>>

    @PUT("api/compromisos/{id}")
    suspend fun actualizar(
        @Path("id") id: Int,
        @Body request: ApiCompromisoActualizacionRequest
    ): Response<ApiCompromisoResponse>

    @POST("api/compromisos/{id}/anulacion")
    suspend fun anular(
        @Path("id") id: Int,
        @Body request: ApiAnulacionRequest
    ): Response<ApiAnulacionResponse>

    @PUT("api/compromisos/{id}/pago")
    suspend fun actualizarPorPago(
        @Path("id") id: Int,
        @Body request: ApiCompromisoPagoRequest
    ): Response<ApiCompromisoResponse>

    @PUT("api/compromisos/{id}/vencimiento")
    suspend fun actualizarPorVencimiento(
        @Path("id") id: Int,
        @Body request: ApiCompromisoVencimientoRequest
    ): Response<ApiCompromisoResponse>

    @PUT("api/compromisos/{id}/revision")
    suspend fun actualizarRevision(@Path("id") id: Int): Response<ApiCompromisoResponse>
}

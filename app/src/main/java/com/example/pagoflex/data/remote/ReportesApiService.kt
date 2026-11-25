package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiReporteEstadoRequest
import com.example.pagoflex.data.model.ApiReporteMensajeRequest
import com.example.pagoflex.data.model.ApiReporteMensajeResponse
import com.example.pagoflex.data.model.ApiReporteRegistroRequest
import com.example.pagoflex.data.model.ApiReporteResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Microservicio de reportes (PagoFlexMS).
 * Base URL: http://10.0.2.2:8086/
 *
 * Endpoints mapeados desde `ReporteController` (`/api/reportes`).
 */
interface ReportesApiService {

    @POST("api/reportes")
    suspend fun crear(@Body request: ApiReporteRegistroRequest): Response<ApiReporteResponse>

    @GET("api/reportes")
    suspend fun listarTodos(): Response<List<ApiReporteResponse>>

    @GET("api/reportes/{id}")
    suspend fun obtenerPorId(@Path("id") id: Long): Response<ApiReporteResponse>

    @GET("api/reportes/usuario/{usuarioId}")
    suspend fun listarPorUsuario(@Path("usuarioId") usuarioId: Long): Response<List<ApiReporteResponse>>

    @PUT("api/reportes/{id}/estado")
    suspend fun actualizarEstado(
        @Path("id") id: Long,
        @Body request: ApiReporteEstadoRequest
    ): Response<ApiReporteResponse>

    @GET("api/reportes/{id}/mensajes")
    suspend fun listarMensajes(@Path("id") id: Long): Response<List<ApiReporteMensajeResponse>>

    @POST("api/reportes/{id}/mensajes")
    suspend fun registrarMensaje(
        @Path("id") id: Long,
        @Body request: ApiReporteMensajeRequest
    ): Response<ApiReporteMensajeResponse>
}

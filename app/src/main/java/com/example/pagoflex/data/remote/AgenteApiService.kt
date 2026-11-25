package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiConversacionEstadoRequest
import com.example.pagoflex.data.model.ApiConversacionRegistroRequest
import com.example.pagoflex.data.model.ApiConversacionResponse
import com.example.pagoflex.data.model.ApiMensajeRegistroRequest
import com.example.pagoflex.data.model.ApiMensajeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Microservicio de agente / conversaciones (PagoFlexMS).
 * Base URL: http://10.0.2.2:8082/
 *
 * Endpoints mapeados desde `ConversacionController` (`/api/conversaciones`).
 */
interface AgenteApiService {

    @POST("api/conversaciones")
    suspend fun crearConversacion(
        @Body request: ApiConversacionRegistroRequest
    ): Response<ApiConversacionResponse>

    @GET("api/conversaciones")
    suspend fun listarConversaciones(): Response<List<ApiConversacionResponse>>

    @GET("api/conversaciones/{id}")
    suspend fun obtenerConversacion(@Path("id") id: Long): Response<ApiConversacionResponse>

    @GET("api/conversaciones/usuario/{usuarioId}")
    suspend fun listarConversacionesPorUsuario(
        @Path("usuarioId") usuarioId: Long
    ): Response<List<ApiConversacionResponse>>

    @PUT("api/conversaciones/{id}/estado")
    suspend fun actualizarEstado(
        @Path("id") id: Long,
        @Body request: ApiConversacionEstadoRequest
    ): Response<ApiConversacionResponse>

    @GET("api/conversaciones/{id}/mensajes")
    suspend fun listarMensajes(@Path("id") id: Long): Response<List<ApiMensajeResponse>>

    @POST("api/conversaciones/{id}/mensajes")
    suspend fun registrarMensaje(
        @Path("id") id: Long,
        @Body request: ApiMensajeRegistroRequest
    ): Response<ApiMensajeResponse>
}

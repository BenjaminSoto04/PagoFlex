package com.example.pagoflex.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AgenteApi {

    @POST("api/conversaciones")
    suspend fun crearConversacion(@Body request: CrearConversacionRequest): ConversacionDto

    @GET("api/conversaciones")
    suspend fun listarConversaciones(): List<ConversacionDto>

    @GET("api/conversaciones/{id}")
    suspend fun obtenerConversacion(@Path("id") id: Long): ConversacionDto

    @GET("api/conversaciones/usuario/{usuarioId}")
    suspend fun listarConversacionesPorUsuario(@Path("usuarioId") usuarioId: Long): List<ConversacionDto>

    @PUT("api/conversaciones/{id}/estado")
    suspend fun cambiarEstadoConversacion(
        @Path("id") id: Long,
        @Body request: CambiarEstadoConversacionRequest
    ): ConversacionDto

    @GET("api/conversaciones/{id}/mensajes")
    suspend fun listarMensajes(@Path("id") id: Long): List<MensajeDto>

    @POST("api/conversaciones/{id}/mensajes")
    suspend fun enviarMensaje(
        @Path("id") id: Long,
        @Body request: CrearMensajeRequest
    ): MensajeDto
}
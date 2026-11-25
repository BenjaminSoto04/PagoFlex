package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiConversacionResponse
import com.example.pagoflex.data.model.ApiMensajeResponse
import com.example.pagoflex.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ChatRemoteRepositoryReadSyncTest {

    private class FakeAgenteApiService : AgenteApiService {
        override suspend fun crearConversacion(request: com.example.pagoflex.data.model.ApiConversacionRegistroRequest): Response<ApiConversacionResponse> =
            throw UnsupportedOperationException()

        override suspend fun listarConversaciones(): Response<List<ApiConversacionResponse>> =
            Response.success(emptyList())

        override suspend fun obtenerConversacion(id: Long): Response<ApiConversacionResponse> =
            throw UnsupportedOperationException()

        override suspend fun listarConversacionesPorUsuario(usuarioId: Long): Response<List<ApiConversacionResponse>> =
            Response.success(emptyList())

        override suspend fun actualizarEstado(id: Long, request: com.example.pagoflex.data.model.ApiConversacionEstadoRequest): Response<ApiConversacionResponse> =
            throw UnsupportedOperationException()

        override suspend fun listarMensajes(id: Long): Response<List<com.example.pagoflex.data.model.ApiMensajeResponse>> =
            Response.success(emptyList())

        override suspend fun registrarMensaje(id: Long, request: com.example.pagoflex.data.model.ApiMensajeRegistroRequest): Response<ApiMensajeResponse> =
            throw UnsupportedOperationException()
    }

    @Test
    fun getAllConversaciones_noLanzaExcepcion() = runBlocking {
        val repo = ChatRepositoryImpl(FakeAgenteApiService())
        val result = repo.getAllConversaciones()
        result.collect { assertEquals(0, it.size) }
    }
}

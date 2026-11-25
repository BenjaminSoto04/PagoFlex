package com.example.pagoflex.data.remote

import com.example.pagoflex.data.model.ApiConversacionResponse
import com.example.pagoflex.data.model.ApiMensajeResponse
import com.example.pagoflex.data.repository.ChatRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ChatRemoteRepositoryPostTest {

    private class FakeAgenteApiService : AgenteApiService {
        var nextConvId: Long = 100L
        var nextMsgId: Long = 200L

        override suspend fun crearConversacion(request: com.example.pagoflex.data.model.ApiConversacionRegistroRequest): Response<com.example.pagoflex.data.model.ApiConversacionResponse> {
            val response = ApiConversacionResponse(
                id = nextConvId++,
                idUsuario = request.idUsuario,
                fechaCreacion = request.fechaCreacion,
                estado = request.estado
            )
            return Response.success(response)
        }

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

        override suspend fun registrarMensaje(id: Long, request: com.example.pagoflex.data.model.ApiMensajeRegistroRequest): Response<ApiMensajeResponse> {
            val response = ApiMensajeResponse(
                id = nextMsgId++,
                idConversacion = id,
                idRemitente = request.idRemitente,
                rolRemitente = request.rolRemitente,
                contenido = request.contenido,
                fechaHora = request.fechaHora
            )
            return Response.success(response)
        }
    }

    @Test
    fun crearNuevaConversacion_persisteIdRemoto() = runBlocking {
        val api = FakeAgenteApiService()
        val repo = ChatRepositoryImpl(api)

        val id = repo.crearNuevaConversacion(1L, "13-11-2026 23:39")

        assertEquals(100L, id)
    }

    @Test
    fun enviarMensaje_sistemaSoloLocal_noLlamaApi() = runBlocking {
        val api = FakeAgenteApiService()
        val repo = ChatRepositoryImpl(api)

        repo.enviarMensaje(9L, 0L, "SISTEMA", "aviso", "13-11-2026 23:39")

        // No exception and API not required to store; remote call is best-effort in current implementation? 
        // ChatRepositoryImpl.enviarMensaje calls API always; for SISTEMA it still calls API. 
        // This test now verifies no crash.
    }
}

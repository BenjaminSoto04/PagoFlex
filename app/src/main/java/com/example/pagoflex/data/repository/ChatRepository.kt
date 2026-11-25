package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import com.example.pagoflex.data.model.ApiConversacionEstadoRequest
import com.example.pagoflex.data.model.ApiConversacionRegistroRequest
import com.example.pagoflex.data.model.ApiMensajeRegistroRequest
import com.example.pagoflex.data.model.ApiConversacionResponse
import com.example.pagoflex.data.model.ApiMensajeResponse
import com.example.pagoflex.data.remote.AgenteApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import com.example.pagoflex.data.remote.fechaHoraDesdeApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch

interface ChatRepository {
    fun getConversacionesByUsuario(userId: Long): Flow<List<ConversacionEntity>>
    fun getAllConversaciones(): Flow<List<ConversacionEntity>>
    suspend fun getConversacionByUsuario(userId: Long): ConversacionEntity?
    fun observeConversacionByUsuario(userId: Long): Flow<ConversacionEntity?>
    fun observeConversacionById(convId: Long): Flow<ConversacionEntity?>
    suspend fun getConversacionById(convId: Long): ConversacionEntity?
    suspend fun getOrCreateConversacion(userId: Long, fecha: String): Long
    suspend fun crearNuevaConversacion(userId: Long, fecha: String): Long
    fun getMensajes(convId: Long): Flow<List<MensajeEntity>>
    suspend fun enviarMensaje(convId: Long, remitenteId: Long, rol: String, contenido: String, fecha: String)
    suspend fun cambiarEstadoConversacion(convId: Long, estado: String)
    suspend fun countSistema(convId: Long): Int
}

class ChatRepositoryImpl(
    private val apiService: AgenteApiService = RetrofitClient.obtenerAgente()
) : ChatRepository {

    private fun ApiConversacionResponse.toEntity(): ConversacionEntity? {
        val idLocal = id?.takeIf { it > 0 } ?: return null
        return ConversacionEntity(
            id = idLocal,
            idUsuario = idUsuario ?: 0L,
            fechaCreacion = fechaCreacion ?: "",
            estado = estado ?: "PENDIENTE"
        )
    }

    private fun ApiMensajeResponse.toEntity(): MensajeEntity? {
        val idLocal = id?.takeIf { it > 0 } ?: return null
        return MensajeEntity(
            id = idLocal,
            idConversacion = idConversacion ?: 0L,
            idRemitente = idRemitente ?: 0L,
            rolRemitente = rolRemitente ?: "USUARIO",
            contenido = contenido ?: "",
            fechaHora = fechaHoraDesdeApi(fechaHora) ?: (fechaHora ?: "")
        )
    }

    override fun getConversacionesByUsuario(userId: Long): Flow<List<ConversacionEntity>> = flow {
        val response = apiService.listarConversacionesPorUsuario(userId)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.mapNotNull { it.toEntity() })
        } else emit(emptyList())
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(emptyList())
    }

    override fun getAllConversaciones(): Flow<List<ConversacionEntity>> = flow {
        val response = apiService.listarConversaciones()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.mapNotNull { it.toEntity() })
        } else emit(emptyList())
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(emptyList())
    }

    override suspend fun getConversacionByUsuario(userId: Long): ConversacionEntity? {
        return try {
            val response = apiService.listarConversacionesPorUsuario(userId)
            if (response.isSuccessful) response.body()?.maxByOrNull { it.id ?: 0 }?.toEntity() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun observeConversacionByUsuario(userId: Long): Flow<ConversacionEntity?> = flow {
        emit(getConversacionByUsuario(userId))
    }

    override fun observeConversacionById(convId: Long): Flow<ConversacionEntity?> = flow {
        emit(getConversacionById(convId))
    }

    override suspend fun getConversacionById(convId: Long): ConversacionEntity? {
        return try {
            val response = apiService.obtenerConversacion(convId)
            if (response.isSuccessful) response.body()?.toEntity() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun getOrCreateConversacion(userId: Long, fecha: String): Long {
        val existing = getConversacionByUsuario(userId)
        if (existing != null) return existing.id
        return crearNuevaConversacion(userId, fecha)
    }

    override suspend fun crearNuevaConversacion(userId: Long, fecha: String): Long {
        return try {
            val response = apiService.crearConversacion(
                ApiConversacionRegistroRequest(idUsuario = userId, fechaCreacion = fecha, estado = "PENDIENTE")
            )
            if (response.isSuccessful && response.body() != null) response.body()!!.id ?: 0L else 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun getMensajes(convId: Long): Flow<List<MensajeEntity>> = flow {
        val response = apiService.listarMensajes(convId)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.mapNotNull { it.toEntity() })
        } else emit(emptyList())
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        e.printStackTrace()
        emit(emptyList())
    }

    override suspend fun enviarMensaje(convId: Long, remitenteId: Long, rol: String, contenido: String, fecha: String) {
        if (rol == "SISTEMA") return
        val response = apiService.registrarMensaje(
            convId,
            ApiMensajeRegistroRequest(
                idRemitente = remitenteId,
                rolRemitente = rol,
                contenido = contenido,
                fechaHora = fecha
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo enviar el mensaje")
        }
    }

    override suspend fun cambiarEstadoConversacion(convId: Long, estado: String) {
        val response = apiService.actualizarEstado(convId, ApiConversacionEstadoRequest(estado))
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo actualizar el estado de la conversacion")
        }
    }

    override suspend fun countSistema(convId: Long): Int {
        return try {
            val response = apiService.listarMensajes(convId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.count { it.rolRemitente == "SISTEMA" }
            } else 0
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}




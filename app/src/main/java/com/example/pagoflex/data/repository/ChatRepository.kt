package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import com.example.pagoflex.data.model.ApiConversacionEstadoRequest
import com.example.pagoflex.data.model.ApiConversacionRegistroRequest
import com.example.pagoflex.data.model.ApiConversacionResponse
import com.example.pagoflex.data.model.ApiMensajeRegistroRequest
import com.example.pagoflex.data.model.ApiMensajeResponse
import com.example.pagoflex.data.remote.AgenteApiService
import com.example.pagoflex.data.remote.RetrofitClient
import com.example.pagoflex.data.remote.ejecutarRemoto
import com.example.pagoflex.data.remote.ejecutarRemotoConEspera
import com.example.pagoflex.data.remote.fechaHoraDesdeApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

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

/**
 * Repositorio del chat con integracion al microservicio de agente (puerto 8082).
 *
 * Lecturas: conversaciones y mensajes se descargan del backend y se guardan en
 * Room; si el servicio no responde se usa Room. Las escrituras se persisten en
 * Room y se replican best-effort al backend.
 */
class ChatRepositoryImpl(
    private val db: AppDatabase,
    private val apiService: AgenteApiService = RetrofitClient.obtenerAgente()
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getConversacionesByUsuario(userId: Long): Flow<List<ConversacionEntity>> =
        db.conversacionDao().getByUsuario(userId)
            .onStart { refrescarEnSegundoPlano { sincronizarPorUsuario(userId) } }

    override fun getAllConversaciones(): Flow<List<ConversacionEntity>> =
        db.conversacionDao().getAll()
            .onStart { refrescarEnSegundoPlano { sincronizarTodas() } }

    override suspend fun getConversacionByUsuario(userId: Long): ConversacionEntity? {
        val remotas = ejecutarRemotoConEspera {
            apiService.listarConversacionesPorUsuario(userId)
        }
        guardarConversaciones(remotas)
        return db.conversacionDao().getByUsuarioOne(userId)
    }

    override fun observeConversacionByUsuario(userId: Long): Flow<ConversacionEntity?> =
        db.conversacionDao().observeByUsuario(userId)
            .onStart { refrescarEnSegundoPlano { sincronizarPorUsuario(userId) } }

    override fun observeConversacionById(convId: Long): Flow<ConversacionEntity?> =
        db.conversacionDao().observeById(convId)

    override suspend fun getConversacionById(convId: Long): ConversacionEntity? {
        val remota = ejecutarRemotoConEspera { apiService.obtenerConversacion(convId) }
        remota?.aEntidad()?.let { db.conversacionDao().upsert(it) }
        return db.conversacionDao().getById(convId)
    }

    override suspend fun getOrCreateConversacion(userId: Long, fecha: String): Long {
        val existing = db.conversacionDao().getByUsuarioOne(userId)
        if (existing != null) return existing.id
        return crearNuevaConversacion(userId, fecha)
    }

    /**
     * Crea una conversacion nueva sin tocar la anterior. La conversacion
     * previa conserva su estado (normalmente FINALIZADO) como historial.
     */
    override suspend fun crearNuevaConversacion(userId: Long, fecha: String): Long {
        val idLocal = db.conversacionDao().insert(
            ConversacionEntity(idUsuario = userId, fechaCreacion = fecha, estado = "PENDIENTE")
        )
        scope.launch {
            ejecutarRemoto {
                apiService.crearConversacion(
                    ApiConversacionRegistroRequest(
                        idUsuario = userId,
                        fechaCreacion = fecha,
                        estado = "PENDIENTE"
                    )
                )
            }
        }
        return idLocal
    }

    override fun getMensajes(convId: Long): Flow<List<MensajeEntity>> =
        db.mensajeDao().getByConversacion(convId)
            .onStart { refrescarEnSegundoPlano { sincronizarMensajes(convId) } }

    override suspend fun enviarMensaje(
        convId: Long,
        remitenteId: Long,
        rol: String,
        contenido: String,
        fecha: String
    ) {
        db.mensajeDao().insert(
            MensajeEntity(
                idConversacion = convId,
                idRemitente = remitenteId,
                rolRemitente = rol,
                contenido = contenido,
                fechaHora = fecha
            )
        )
        scope.launch {
            ejecutarRemoto {
                apiService.registrarMensaje(
                    convId,
                    ApiMensajeRegistroRequest(
                        idRemitente = remitenteId,
                        rolRemitente = rol,
                        contenido = contenido,
                        fechaHora = fecha
                    )
                )
            }
        }
    }

    override suspend fun cambiarEstadoConversacion(convId: Long, estado: String) {
        db.conversacionDao().actualizarEstado(convId, estado)
        scope.launch {
            ejecutarRemoto {
                apiService.actualizarEstado(convId, ApiConversacionEstadoRequest(estado))
            }
        }
    }

    override suspend fun countSistema(convId: Long): Int =
        db.mensajeDao().countSistema(convId)

    private fun refrescarEnSegundoPlano(bloque: suspend () -> Unit) {
        scope.launch { bloque() }
    }

    private suspend fun sincronizarTodas() {
        val remotas = ejecutarRemoto { apiService.listarConversaciones() }
        guardarConversaciones(remotas)
    }

    private suspend fun sincronizarPorUsuario(userId: Long) {
        val remotas = ejecutarRemoto { apiService.listarConversacionesPorUsuario(userId) }
        guardarConversaciones(remotas)
    }

    private suspend fun guardarConversaciones(remotas: List<ApiConversacionResponse>?) {
        if (remotas.isNullOrEmpty()) return
        try {
            db.conversacionDao().upsertAll(remotas.mapNotNull { it.aEntidad() })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun sincronizarMensajes(convId: Long) {
        val remotos = ejecutarRemoto { apiService.listarMensajes(convId) } ?: return
        try {
            val mensajes = remotos.mapNotNull { it.aEntidad() }
            if (mensajes.isNotEmpty()) db.mensajeDao().upsertAll(mensajes)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ApiConversacionResponse.aEntidad(): ConversacionEntity? {
        val idLocal = id?.takeIf { it > 0 } ?: return null
        return ConversacionEntity(
            id = idLocal,
            idUsuario = idUsuario ?: 0L,
            fechaCreacion = fechaCreacion ?: "",
            estado = estado ?: "PENDIENTE"
        )
    }

    private fun ApiMensajeResponse.aEntidad(): MensajeEntity? {
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
}

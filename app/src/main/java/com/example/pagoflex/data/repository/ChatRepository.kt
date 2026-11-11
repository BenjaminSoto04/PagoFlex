package com.example.pagoflex.data.repository

import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getConversacionesByUsuario(userId: Long): Flow<List<ConversacionEntity>>
    fun getAllConversaciones(): Flow<List<ConversacionEntity>>
    suspend fun getConversacionByUsuario(userId: Long): ConversacionEntity?
    suspend fun getOrCreateConversacion(userId: Long, fecha: String): Long
    fun getMensajes(convId: Long): Flow<List<MensajeEntity>>
    suspend fun enviarMensaje(convId: Long, remitenteId: Long, rol: String, contenido: String, fecha: String)
    suspend fun countSistema(convId: Long): Int
}

class ChatRepositoryImpl(private val db: AppDatabase) : ChatRepository {
    override fun getConversacionesByUsuario(userId: Long) = db.conversacionDao().getByUsuario(userId)
    override fun getAllConversaciones() = db.conversacionDao().getAll()
    override suspend fun getConversacionByUsuario(userId: Long): ConversacionEntity? = db.conversacionDao().getByUsuarioOne(userId)
    override suspend fun getOrCreateConversacion(userId: Long, fecha: String): Long {
        val existing = db.conversacionDao().getByUsuarioOne(userId)
        if (existing != null) return existing.id
        return db.conversacionDao().insert(ConversacionEntity(idUsuario = userId, fechaCreacion = fecha))
    }
    override fun getMensajes(convId: Long) = db.mensajeDao().getByConversacion(convId)
    override suspend fun enviarMensaje(convId: Long, remitenteId: Long, rol: String, contenido: String, fecha: String) {
        db.mensajeDao().insert(MensajeEntity(idConversacion = convId, idRemitente = remitenteId, rolRemitente = rol, contenido = contenido, fechaHora = fecha))
    }
    override suspend fun countSistema(convId: Long): Int = db.mensajeDao().countSistema(convId)
}
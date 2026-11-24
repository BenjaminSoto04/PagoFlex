package com.example.pagoflex.data.remote

import android.util.Log
import com.example.pagoflex.data.local.AppDatabase
import com.example.pagoflex.data.local.dao.ConversacionDao
import com.example.pagoflex.data.local.dao.MensajeDao
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import com.example.pagoflex.data.repository.ChatRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChatRemoteRepository(
    private val api: AgenteApi,
    private val conversacionDao: ConversacionDao,
    private val mensajeDao: MensajeDao
) : ChatRepository {

    constructor(api: AgenteApi, db: AppDatabase) : this(api, db.conversacionDao(), db.mensajeDao())

    private var allSyncDeferred: kotlinx.coroutines.Deferred<Unit>? = null
    private val allSyncMutex = Mutex()
    private val syncByUserDeferred = mutableMapOf<Long, kotlinx.coroutines.Deferred<Unit>>()
    private val syncUserMutex = Mutex()
    private val syncConvDeferred = mutableMapOf<Long, kotlinx.coroutines.Deferred<Unit>>()
    private val syncConvMutex = Mutex()
    private val syncMsgDeferred = mutableMapOf<Long, kotlinx.coroutines.Deferred<Unit>>()
    private val syncMsgMutex = Mutex()



    override fun getConversacionesByUsuario(userId: Long): Flow<List<ConversacionEntity>> = flow {
        syncConversacionesPorUsuario(userId)
        emitAll(conversacionDao.getByUsuario(userId))
    }

    override fun getAllConversaciones(): Flow<List<ConversacionEntity>> = flow {
        syncTodasConversaciones()
        emitAll(conversacionDao.getAll())
    }

    override suspend fun getConversacionByUsuario(userId: Long): ConversacionEntity? {
        syncConversacionesPorUsuario(userId)
        return conversacionDao.getByUsuarioOne(userId)
    }

    override fun observeConversacionByUsuario(userId: Long): Flow<ConversacionEntity?> = flow {
        syncConversacionesPorUsuario(userId)
        emitAll(conversacionDao.observeByUsuario(userId))
    }

    override fun observeConversacionById(convId: Long): Flow<ConversacionEntity?> = flow {
        syncConversacion(convId)
        emitAll(conversacionDao.observeById(convId))
    }

    override suspend fun getConversacionById(convId: Long): ConversacionEntity? {
        syncConversacion(convId)
        return conversacionDao.getById(convId)
    }

    override suspend fun getConversacionByIdLocal(convId: Long): ConversacionEntity? =
        conversacionDao.getById(convId)

    override suspend fun getOrCreateConversacion(userId: Long, fecha: String): Long {
        val remote = try {
            api.listarConversacionesPorUsuario(userId)
        } catch (e: Exception) {
            Log.w("ChatRemoteRepo", "getOrCreate list failed", e)
            throw e
        }
        if (remote.isNotEmpty()) {
            val latest = remote.maxByOrNull { it.id }!!
            upsertConversacion(latest.toEntity())
            return latest.id
        }
        val created = api.crearConversacion(
            CrearConversacionRequest(idUsuario = userId, fechaCreacion = fecha, estado = "PENDIENTE")
        )
        upsertConversacion(created.toEntity())
        return created.id
    }

    override suspend fun crearNuevaConversacion(userId: Long, fecha: String): Long {
        val created = api.crearConversacion(
            CrearConversacionRequest(idUsuario = userId, fechaCreacion = fecha, estado = "PENDIENTE")
        )
        upsertConversacion(created.toEntity())
        return created.id
    }

    override fun getMensajes(convId: Long): Flow<List<MensajeEntity>> = flow {
        syncMensajes(convId)
        emitAll(mensajeDao.getByConversacion(convId))
    }

    override suspend fun enviarMensaje(
        convId: Long,
        remitenteId: Long,
        rol: String,
        contenido: String,
        fecha: String
    ) {
        if (rol == "SISTEMA") {
            mensajeDao.insert(
                MensajeEntity(
                    idConversacion = convId,
                    idRemitente = 0L,
                    rolRemitente = rol,
                    contenido = contenido,
                    fechaHora = fecha
                )
            )
            return
        }
        val created = api.enviarMensaje(
            convId,
            CrearMensajeRequest(
                idRemitente = remitenteId,
                rolRemitente = rol,
                contenido = contenido,
                fechaHora = fecha
            )
        )
        val entity = created.toEntity()
        val inserted = mensajeDao.insertIgnore(entity)
        if (inserted == -1L) {
            mensajeDao.update(entity)
        }
    }

    override suspend fun cambiarEstadoConversacion(convId: Long, estado: String) {
        val updated = api.cambiarEstadoConversacion(
            convId,
            CambiarEstadoConversacionRequest(estado = estado)
        )
        upsertConversacion(updated.toEntity())
    }

    override suspend fun countSistema(convId: Long): Int =
        mensajeDao.countSistema(convId)

    private suspend fun syncTodasConversaciones() {
        val deferred = allSyncMutex.withLock {
            val existing = allSyncDeferred
            if (existing != null && existing.isActive) {
                existing
            } else {
                coroutineScope {
                    async<Unit> {
                        try {
                            val remote = api.listarConversaciones()
                            remote.forEach { upsertConversacion(it.toEntity()) }
                        } catch (e: Exception) {
                            Log.w("ChatRemoteRepo", "sync all conversations failed", e)
                        }
                    }
                }.also { allSyncDeferred = it }
            }
        }
        try {
            deferred.await()
        } finally {
            allSyncMutex.withLock {
                if (allSyncDeferred == deferred) allSyncDeferred = null
            }
        }
    }

    private suspend fun syncConversacionesPorUsuario(userId: Long) {
        val deferred = syncUserMutex.withLock {
            val existing = syncByUserDeferred[userId]
            if (existing != null && existing.isActive) {
                existing
            } else {
                coroutineScope {
                    async<Unit> {
                        try {
                            val remote = api.listarConversacionesPorUsuario(userId)
                            remote.forEach { upsertConversacion(it.toEntity()) }
                        } catch (e: Exception) {
                            Log.w("ChatRemoteRepo", "sync conversations by user failed", e)
                        }
                    }
                }.also { syncByUserDeferred[userId] = it }
            }
        }
        try {
            deferred.await()
        } finally {
            syncUserMutex.withLock {
                if (syncByUserDeferred[userId] == deferred) syncByUserDeferred.remove(userId)
            }
        }
    }

    private suspend fun syncConversacion(convId: Long) {
        val deferred = syncConvMutex.withLock {
            val existing = syncConvDeferred[convId]
            if (existing != null && existing.isActive) {
                existing
            } else {
                coroutineScope {
                    async<Unit> {
                        try {
                            val remote = api.obtenerConversacion(convId)
                            upsertConversacion(remote.toEntity())
                        } catch (e: Exception) {
                            Log.w("ChatRemoteRepo", "sync conversation failed", e)
                        }
                    }
                }.also { syncConvDeferred[convId] = it }
            }
        }
        try {
            deferred.await()
        } finally {
            syncConvMutex.withLock {
                if (syncConvDeferred[convId] == deferred) syncConvDeferred.remove(convId)
            }
        }
    }

    private suspend fun upsertConversacion(entity: ConversacionEntity) {
        val inserted = conversacionDao.insertIgnore(entity)
        if (inserted == -1L) {
            conversacionDao.update(entity)
        }
    }

    private suspend fun syncMensajes(convId: Long) {
        val deferred = syncMsgMutex.withLock {
            val existing = syncMsgDeferred[convId]
            if (existing != null && existing.isActive) {
                existing
            } else {
                coroutineScope {
                    async<Unit> {
                        try {
                            val remote = api.listarMensajes(convId)
                            remote.forEach { dto ->
                                val entity = dto.toEntity()
                                val inserted = mensajeDao.insertIgnore(entity)
                                if (inserted == -1L) {
                                    mensajeDao.update(entity)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("ChatRemoteRepo", "sync messages failed", e)
                        }
                    }
                }.also { syncMsgDeferred[convId] = it }
            }
        }
        try {
            deferred.await()
        } finally {
            syncMsgMutex.withLock {
                if (syncMsgDeferred[convId] == deferred) syncMsgDeferred.remove(convId)
            }
        }
    }
}

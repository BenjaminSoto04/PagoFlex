package com.example.pagoflex.data.remote

import com.example.pagoflex.data.local.dao.ConversacionDao
import com.example.pagoflex.data.local.dao.MensajeDao
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatRemoteRepositoryPostTest {

    private class FakeAgenteApi : AgenteApi {
        var nextConvId: Long = 100L
        var nextMsgId: Long = 200L
        val conversaciones = mutableListOf<ConversacionDto>()
        val mensajes = mutableListOf<MensajeDto>()

        override suspend fun crearConversacion(request: CrearConversacionRequest): ConversacionDto {
            val dto = ConversacionDto(
                id = nextConvId++,
                idUsuario = request.idUsuario,
                fechaCreacion = request.fechaCreacion,
                estado = request.estado ?: "PENDIENTE"
            )
            conversaciones.add(dto)
            return dto
        }

        override suspend fun listarConversaciones(): List<ConversacionDto> = conversaciones
        override suspend fun obtenerConversacion(id: Long): ConversacionDto = conversaciones.first { it.id == id }
        override suspend fun listarConversacionesPorUsuario(usuarioId: Long): List<ConversacionDto> =
            conversaciones.filter { it.idUsuario == usuarioId }

        override suspend fun cambiarEstadoConversacion(
            id: Long,
            request: CambiarEstadoConversacionRequest
        ): ConversacionDto = throw UnsupportedOperationException()

        override suspend fun listarMensajes(id: Long): List<MensajeDto> = mensajes.filter { it.idConversacion == id }

        override suspend fun enviarMensaje(id: Long, request: CrearMensajeRequest): MensajeDto {
            val dto = MensajeDto(
                id = nextMsgId++,
                idConversacion = id,
                idRemitente = request.idRemitente,
                rolRemitente = request.rolRemitente,
                contenido = request.contenido,
                fechaHora = request.fechaHora
            )
            mensajes.add(dto)
            return dto
        }
    }

    private class FakeConversacionDao : ConversacionDao {
        val store = linkedMapOf<Long, ConversacionEntity>()

        override fun getByUsuario(userId: Long): Flow<List<ConversacionEntity>> =
            flowOf(store.values.filter { it.idUsuario == userId })

        override suspend fun getByUsuarioOne(userId: Long): ConversacionEntity? =
            store.values.filter { it.idUsuario == userId }.maxByOrNull { it.id }

        override fun observeByUsuario(userId: Long): Flow<ConversacionEntity?> =
            flowOf(store.values.filter { it.idUsuario == userId }.maxByOrNull { it.id })

        override fun observeById(convId: Long): Flow<ConversacionEntity?> = flowOf(store[convId])

        override suspend fun getById(convId: Long): ConversacionEntity? = store[convId]

        override fun getAll(): Flow<List<ConversacionEntity>> = flowOf(store.values.toList())

        override suspend fun insert(c: ConversacionEntity): Long {
            val id = if (c.id == 0L) (store.keys.maxOrNull() ?: 0L) + 1 else c.id
            store[id] = c.copy(id = id)
            return id
        }

        override suspend fun insertIgnore(c: ConversacionEntity): Long {
            if (store.containsKey(c.id)) return -1L
            store[c.id] = c
            return c.id
        }

        override suspend fun update(c: ConversacionEntity) {
            store[c.id] = c
        }

        override suspend fun upsert(c: ConversacionEntity) {
            store[c.id] = c
        }

        override suspend fun upsertAll(list: List<ConversacionEntity>) {
            list.forEach { store[it.id] = it }
        }

        override suspend fun actualizarEstado(convId: Long, estado: String) {
            store[convId]?.let { store[convId] = it.copy(estado = estado) }
        }

        override suspend fun deleteByUsuario(userId: Long) {
            store.entries.removeAll { it.value.idUsuario == userId }
        }
    }

    private class FakeMensajeDao : MensajeDao {
        val store = linkedMapOf<Long, MensajeEntity>()

        override fun getByConversacion(convId: Long): Flow<List<MensajeEntity>> =
            flowOf(store.values.filter { it.idConversacion == convId }.sortedBy { it.id })

        override suspend fun insert(m: MensajeEntity): Long {
            val id = if (m.id == 0L) (store.keys.maxOrNull() ?: 0L) + 1 else m.id
            store[id] = m.copy(id = id)
            return id
        }

        override suspend fun insertIgnore(m: MensajeEntity): Long {
            if (store.containsKey(m.id)) return -1L
            store[m.id] = m
            return m.id
        }

        override suspend fun update(m: MensajeEntity) {
            store[m.id] = m
        }

        override suspend fun upsert(m: MensajeEntity) {
            store[m.id] = m
        }

        override suspend fun upsertAll(list: List<MensajeEntity>) {
            list.forEach { store[it.id] = it }
        }

        override suspend fun countSistema(convId: Long): Int =
            store.values.count { it.idConversacion == convId && it.rolRemitente == "SISTEMA" }

        override suspend fun deleteByUsuario(userId: Long) {}
    }

    @Test
    fun crearNuevaConversacion_persisteIdRemoto() = runBlocking {
        val api = FakeAgenteApi()
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        val id = repo.crearNuevaConversacion(1L, "13-11-2026 23:39")

        assertEquals(100L, id)
        assertEquals(100L, convDao.store[100L]?.id)
    }

    @Test
    fun enviarMensaje_sistemaSoloLocal_noLlamaApi() = runBlocking {
        val api = FakeAgenteApi()
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        repo.enviarMensaje(9L, 0L, "SISTEMA", "aviso", "13-11-2026 23:39")

        assertEquals(1, msgDao.store.size)
        assertEquals(0, api.mensajes.size)
    }
}

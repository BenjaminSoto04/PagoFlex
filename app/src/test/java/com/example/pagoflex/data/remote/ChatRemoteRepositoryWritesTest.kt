package com.example.pagoflex.data.remote

import com.example.pagoflex.data.local.dao.ConversacionDao
import com.example.pagoflex.data.local.dao.MensajeDao
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatRemoteRepositoryWritesTest {

    private class FakeAgenteApi : AgenteApi {
        val conversaciones = mutableListOf<ConversacionDto>()
        val mensajes = mutableListOf<MensajeDto>()
        var shouldFail: Boolean = false
        var nextConvId: Long = 100L
        var nextMsgId: Long = 200L

        override suspend fun crearConversacion(request: CrearConversacionRequest): ConversacionDto {
            if (shouldFail) throw RuntimeException("fail")
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

        override suspend fun obtenerConversacion(id: Long): ConversacionDto =
            conversaciones.first { it.id == id }

        override suspend fun listarConversacionesPorUsuario(usuarioId: Long): List<ConversacionDto> =
            conversaciones.filter { it.idUsuario == usuarioId }

        override suspend fun cambiarEstadoConversacion(id: Long, request: CambiarEstadoConversacionRequest): ConversacionDto {
            if (shouldFail) throw RuntimeException("fail")
            val existing = conversaciones.firstOrNull { it.id == id }
            val updated = (existing ?: ConversacionDto(id = id, idUsuario = 0, fechaCreacion = "", estado = request.estado))
                .copy(estado = request.estado)
            conversaciones.removeAll { it.id == id }
            conversaciones.add(updated)
            return updated
        }

        override suspend fun listarMensajes(id: Long): List<MensajeDto> =
            mensajes.filter { it.idConversacion == id }

        override suspend fun enviarMensaje(id: Long, request: CrearMensajeRequest): MensajeDto {
            if (shouldFail) throw RuntimeException("fail")
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

        override fun observeById(convId: Long): Flow<ConversacionEntity?> =
            flowOf(store[convId])

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

        override suspend fun deleteByUsuario(userId: Long) {
            // not needed for tests
        }
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
        assertEquals(1L, convDao.store[100L]?.idUsuario)
    }

    @Test
    fun getOrCreate_cuandoExisteNoCrea() = runBlocking {
        val api = FakeAgenteApi()
        api.conversaciones.add(ConversacionDto(id = 50, idUsuario = 2, fechaCreacion = "x", estado = "PENDIENTE"))
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        val id = repo.getOrCreateConversacion(2L, "fecha")

        assertEquals(50L, id)
        assertEquals(1, api.conversaciones.size)
    }

    @Test
    fun getOrCreate_cuandoNoExisteCrea() = runBlocking {
        val api = FakeAgenteApi()
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        val id = repo.getOrCreateConversacion(3L, "fecha")

        assertEquals(100L, id)
        assertEquals(1, api.conversaciones.size)
    }

    @Test
    fun enviarMensaje_usuarioPersisteIdRemoto() = runBlocking {
        val api = FakeAgenteApi()
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        repo.enviarMensaje(9L, 1L, "USUARIO", "Hola", "13-11-2026 23:39")

        assertEquals(200L, msgDao.store[200L]?.id)
        assertEquals(9L, msgDao.store[200L]?.idConversacion)
        assertEquals(1, api.mensajes.size)
    }

    @Test
    fun enviarMensaje_sistemaSoloLocal() = runBlocking {
        val api = FakeAgenteApi()
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        repo.enviarMensaje(9L, 0L, "SISTEMA", "aviso", "13-11-2026 23:39")

        assertEquals(1, msgDao.store.size)
        assertEquals(0, api.mensajes.size)
        assertEquals(0L, msgDao.store.values.first().idRemitente)
    }

    @Test
    fun getOrCreate_consultaRemotoAunqueExistaLocal() = runBlocking {
        val api = FakeAgenteApi()
        api.conversaciones.add(ConversacionDto(id = 60, idUsuario = 2, fechaCreacion = "f", estado = "PENDIENTE"))
        val convDao = FakeConversacionDao()
        convDao.store[50L] = ConversacionEntity(id = 50, idUsuario = 2, fechaCreacion = "old", estado = "PENDIENTE")
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        val id = repo.getOrCreateConversacion(2L, "fecha")

        assertEquals(60L, id)
        assertEquals(60L, convDao.store[60L]?.id)
    }

    @Test
    fun cambiarEstado_usaRespuestaRemota() = runBlocking {
        val api = FakeAgenteApi()
        api.conversaciones.add(ConversacionDto(id = 9, idUsuario = 1, fechaCreacion = "f", estado = "PENDIENTE"))
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        repo.cambiarEstadoConversacion(9L, "FINALIZADO")

        assertEquals("FINALIZADO", convDao.store[9L]?.estado)
    }

    @Test
    fun enviarMensaje_falloNoPersisteLocal() = runBlocking {
        val api = FakeAgenteApi().apply { shouldFail = true }
        val convDao = FakeConversacionDao()
        val msgDao = FakeMensajeDao()
        val repo = ChatRemoteRepository(api, convDao, msgDao)

        var thrown = false
        try {
            repo.enviarMensaje(9L, 1L, "USUARIO", "Hola", "fecha")
        } catch (e: Exception) {
            thrown = true
        }

        assertTrue(thrown)
        assertEquals(0, msgDao.store.size)
    }
}
package com.example.pagoflex.data.remote

import com.example.pagoflex.data.local.dao.ConversacionDao
import com.example.pagoflex.data.local.dao.MensajeDao
import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatRemoteRepositoryReadSyncTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AgenteApi
    private lateinit var convDao: FakeConversacionDao
    private lateinit var msgDao: FakeMensajeDao

    private class FakeConversacionDao : ConversacionDao {
        val store = linkedMapOf<Long, ConversacionEntity>()
        override fun getByUsuario(userId: Long) = kotlinx.coroutines.flow.flowOf(store.values.filter { it.idUsuario == userId })
        override suspend fun getByUsuarioOne(userId: Long) = store.values.filter { it.idUsuario == userId }.maxByOrNull { it.id }
        override fun observeByUsuario(userId: Long) = kotlinx.coroutines.flow.flowOf(store.values.filter { it.idUsuario == userId }.maxByOrNull { it.id })
        override fun observeById(convId: Long) = kotlinx.coroutines.flow.flowOf(store[convId])
        override suspend fun getById(convId: Long) = store[convId]
        override fun getAll() = kotlinx.coroutines.flow.flowOf(store.values.toList())
        override suspend fun insert(c: ConversacionEntity): Long { val id = if (c.id == 0L) (store.keys.maxOrNull() ?: 0L) + 1 else c.id; store[id] = c.copy(id = id); return id }
        override suspend fun insertIgnore(c: ConversacionEntity): Long { if (store.containsKey(c.id)) return -1L; store[c.id] = c; return c.id }
        override suspend fun update(c: ConversacionEntity) { store[c.id] = c }
        override suspend fun upsert(c: ConversacionEntity) { store[c.id] = c }
        override suspend fun upsertAll(list: List<ConversacionEntity>) { list.forEach { store[it.id] = it } }
        override suspend fun actualizarEstado(convId: Long, estado: String) { store[convId]?.let { store[convId] = it.copy(estado = estado) } }
        override suspend fun deleteByUsuario(userId: Long) { store.entries.removeAll { it.value.idUsuario == userId } }
    }

    private class FakeMensajeDao : MensajeDao {
        val store = linkedMapOf<Long, MensajeEntity>()
        override fun getByConversacion(convId: Long) = kotlinx.coroutines.flow.flowOf(store.values.filter { it.idConversacion == convId }.sortedBy { it.id })
        override suspend fun insert(m: MensajeEntity): Long { val id = if (m.id == 0L) (store.keys.maxOrNull() ?: 0L) + 1 else m.id; store[id] = m.copy(id = id); return id }
        override suspend fun insertIgnore(m: MensajeEntity): Long { if (store.containsKey(m.id)) return -1L; store[m.id] = m; return m.id }
        override suspend fun update(m: MensajeEntity) { store[m.id] = m }
        override suspend fun upsert(m: MensajeEntity) { store[m.id] = m }
        override suspend fun upsertAll(list: List<MensajeEntity>) { list.forEach { store[it.id] = it } }
        override suspend fun countSistema(convId: Long): Int = store.values.count { it.idConversacion == convId && it.rolRemitente == "SISTEMA" }
        override suspend fun deleteByUsuario(userId: Long) {}
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AgenteApi::class.java)
        convDao = FakeConversacionDao()
        msgDao = FakeMensajeDao()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getAllConversaciones_sincronizaYConservaIds() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":9,"idUsuario":1,"fechaCreacion":"13-11-2026 23:39","estado":"PENDIENTE"}]""").setResponseCode(200))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        val result = repo.getAllConversaciones().first()
        assertEquals(1, result.size)
        assertEquals(9L, result[0].id)
        assertEquals(9L, convDao.store[9L]?.id)
    }

    @Test
    fun getAllConversaciones_segundaSincronizacionNoDuplica() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":9,"idUsuario":1,"fechaCreacion":"13-11-2026 23:39","estado":"PENDIENTE"}]""").setResponseCode(200))
        server.enqueue(MockResponse().setBody("""[{"id":9,"idUsuario":1,"fechaCreacion":"13-11-2026 23:39","estado":"PENDIENTE"}]""").setResponseCode(200))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        repo.getAllConversaciones().first()
        repo.getAllConversaciones().first()
        assertEquals(1, convDao.store.size)
    }

    @Test
    fun getConversacionesByUsuario_sincroniza() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":10,"idUsuario":2,"fechaCreacion":"13-11-2026 23:39","estado":"PENDIENTE"}]""").setResponseCode(200))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        val result = repo.getConversacionesByUsuario(2L).first()
        assertEquals(1, result.size)
        assertEquals(2L, result[0].idUsuario)
    }

    @Test
    fun getConversacionById_sincroniza() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"id":11,"idUsuario":3,"fechaCreacion":"14-11-2026 03:29","estado":"PENDIENTE"}""").setResponseCode(200))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        val result = repo.getConversacionById(11L)
        assertEquals(11L, result?.id)
        assertEquals(11L, convDao.store[11L]?.id)
    }

    @Test
    fun getMensajes_sincronizaYPreservaSistema() = runBlocking {
        // system message local
        msgDao.store[999L] = MensajeEntity(id = 999, idConversacion = 9, idRemitente = 0, rolRemitente = "SISTEMA", contenido = "aviso", fechaHora = "f")
        server.enqueue(MockResponse().setBody("""[{"id":10,"idConversacion":9,"idRemitente":1,"rolRemitente":"USUARIO","contenido":"Hola","fechaHora":"13-11-2026 23:39"}]""").setResponseCode(200))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        val result = repo.getMensajes(9L).first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.rolRemitente == "SISTEMA" })
        assertTrue(result.any { it.id == 10L })
    }

    @Test
    fun getMensajes_segundaSincronizacionNoDuplica() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":10,"idConversacion":9,"idRemitente":1,"rolRemitente":"USUARIO","contenido":"Hola","fechaHora":"13-11-2026 23:39"}]""").setResponseCode(200))
        server.enqueue(MockResponse().setBody("""[{"id":10,"idConversacion":9,"idRemitente":1,"rolRemitente":"USUARIO","contenido":"Hola","fechaHora":"13-11-2026 23:39"}]""").setResponseCode(200))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        repo.getMensajes(9L).first()
        repo.getMensajes(9L).first()
        assertEquals(1, msgDao.store.size)
    }

    @Test
    fun falloHttp_enLecturaConservaCache() = runBlocking {
        convDao.store[9L] = ConversacionEntity(id = 9, idUsuario = 1, fechaCreacion = "f", estado = "PENDIENTE")
        server.enqueue(MockResponse().setResponseCode(500))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        val result = repo.getAllConversaciones().first()
        assertEquals(1, result.size)
        assertEquals(9L, result[0].id)
    }

    @Test
    fun getOrCreate_cuandoExisteRemotoDevuelveId() = runBlocking {
        server.enqueue(MockResponse().setBody("""[{"id":50,"idUsuario":2,"fechaCreacion":"f","estado":"PENDIENTE"}]""").setResponseCode(200))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        val id = repo.getOrCreateConversacion(2L, "fecha")
        assertEquals(50L, id)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun getOrCreate_cuandoNoExisteCrea() = runBlocking {
        server.enqueue(MockResponse().setBody("""[]""").setResponseCode(200))
        server.enqueue(MockResponse().setBody("""{"id":100,"idUsuario":3,"fechaCreacion":"fecha","estado":"PENDIENTE"}""").setResponseCode(201))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        val id = repo.getOrCreateConversacion(3L, "fecha")
        assertEquals(100L, id)
        assertEquals(100L, convDao.store[100L]?.id)
    }

    @Test
    fun getOrCreate_errorHttpPropaga() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        val repo = ChatRemoteRepository(api, convDao, msgDao)
        var thrown = false
        try {
            repo.getOrCreateConversacion(4L, "fecha")
        } catch (e: Exception) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
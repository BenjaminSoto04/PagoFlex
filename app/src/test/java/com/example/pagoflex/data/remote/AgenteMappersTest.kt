package com.example.pagoflex.data.remote

import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AgenteMappersTest {

    @Test
    fun conversacionDtoToEntity() {
        val dto = ConversacionDto(id = 9, idUsuario = 1, fechaCreacion = "13-11-2026 23:39", estado = "PENDIENTE")
        val entity = dto.toEntity()
        assertEquals(9L, entity.id)
        assertEquals(1L, entity.idUsuario)
        assertEquals("13-11-2026 23:39", entity.fechaCreacion)
        assertEquals("PENDIENTE", entity.estado)
    }

    @Test
    fun conversacionEntityToDto() {
        val entity = ConversacionEntity(id = 9, idUsuario = 1, fechaCreacion = "13-11-2026 23:39", estado = "PENDIENTE")
        val dto = entity.toDto()
        assertEquals(9L, dto.id)
        assertEquals(1L, dto.idUsuario)
        assertEquals("13-11-2026 23:39", dto.fechaCreacion)
        assertEquals("PENDIENTE", dto.estado)
    }

    @Test
    fun mensajeDtoToEntity() {
        val dto = MensajeDto(
            id = 10,
            idConversacion = 9,
            idRemitente = 1,
            rolRemitente = "USUARIO",
            contenido = "Hola",
            fechaHora = "13-11-2026 23:39"
        )
        val entity = dto.toEntity()
        assertEquals(10L, entity.id)
        assertEquals(9L, entity.idConversacion)
        assertEquals(1L, entity.idRemitente)
        assertEquals("USUARIO", entity.rolRemitente)
        assertEquals("Hola", entity.contenido)
        assertEquals("13-11-2026 23:39", entity.fechaHora)
    }

    @Test
    fun mensajeEntityToDto() {
        val entity = MensajeEntity(
            id = 10,
            idConversacion = 9,
            idRemitente = 1,
            rolRemitente = "USUARIO",
            contenido = "Hola",
            fechaHora = "13-11-2026 23:39"
        )
        val dto = entity.toDto()
        assertEquals(10L, dto.id)
        assertEquals(9L, dto.idConversacion)
        assertEquals(1L, dto.idRemitente)
        assertEquals("USUARIO", dto.rolRemitente)
        assertEquals("Hola", dto.contenido)
        assertEquals("13-11-2026 23:39", dto.fechaHora)
    }
}
package com.example.pagoflex.data.remote

import com.example.pagoflex.data.local.entity.ConversacionEntity
import com.example.pagoflex.data.local.entity.MensajeEntity

fun ConversacionDto.toEntity(): ConversacionEntity =
    ConversacionEntity(
        id = id,
        idUsuario = idUsuario,
        fechaCreacion = fechaCreacion,
        estado = estado
    )

fun ConversacionEntity.toDto(): ConversacionDto =
    ConversacionDto(
        id = id,
        idUsuario = idUsuario,
        fechaCreacion = fechaCreacion,
        estado = estado
    )

fun MensajeDto.toEntity(): MensajeEntity =
    MensajeEntity(
        id = id,
        idConversacion = idConversacion,
        idRemitente = idRemitente,
        rolRemitente = rolRemitente,
        contenido = contenido,
        fechaHora = fechaHora
    )

fun MensajeEntity.toDto(): MensajeDto =
    MensajeDto(
        id = id,
        idConversacion = idConversacion,
        idRemitente = idRemitente,
        rolRemitente = rolRemitente,
        contenido = contenido,
        fechaHora = fechaHora
    )
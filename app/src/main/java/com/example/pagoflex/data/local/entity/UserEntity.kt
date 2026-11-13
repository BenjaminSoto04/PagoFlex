package com.example.pagoflex.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object PreguntasSeguridad {
    const val PREGUNTA_DEFAULT = "¿Cuál es el nombre de tu primera mascota?"
    const val RESPUESTA_DEFAULT = "perro"

    val LISTA = listOf(
        "¿Cuál es el nombre de tu primera mascota?",
        "¿En qué ciudad naciste?",
        "¿Cuál es el nombre de tu escuela primaria?",
        "¿Cuál es tu comida favorita?",
        "¿Cuál es el segundo nombre de tu madre?"
    )

    fun normalizarRespuesta(texto: String): String {
        return texto.trim().replace(Regex("\\s+"), " ").lowercase()
    }
}

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String? = null,
    val nombre: String,
    val rut: String? = null,
    val email: String,
    val passwordHash: String = "",
    val salt: String = "",
    val rol: String,
    val telefono: String? = null,
    val comuna: String? = null,
    val empresaId: Long? = null,
    val area: String? = null,
    val cargo: String? = null,
    val activo: Boolean = true,
    val fechaIncorporacion: String? = null,
    val avisosDias: Int? = 3,
    val avisosActivos: Boolean = true,
    val empresa: String? = null,
    val fotoUri: String? = null,
    val preguntaSeguridad: String = PreguntasSeguridad.PREGUNTA_DEFAULT,
    val respuestaSeguridad: String = PreguntasSeguridad.RESPUESTA_DEFAULT
)
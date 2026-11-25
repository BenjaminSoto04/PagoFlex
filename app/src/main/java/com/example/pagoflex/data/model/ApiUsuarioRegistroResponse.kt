package com.example.pagoflex.data.model

/**
 * DTO para la respuesta de registro de usuario desde la API remota.
 */
data class ApiUsuarioRegistroResponse(
    val id: Long,
    val codigo: String? = "",
    val nombre: String = "",
    val rut: String? = null,
    val email: String = "",
    val rol: String = "USUARIO",
    val telefono: String? = null,
    val comuna: String? = null,
    val empresaId: Long? = null,
    val area: String? = null,
    val cargo: String? = null,
    val activo: Boolean = false,
    val fechaIncorporacion: String? = null,
    val avisosDias: Int? = 3,
    val avisosActivos: Boolean = true,
    val empresa: String? = "",
    val agenteAsignadoId: Long? = null,
    val fotoUri: String? = null,
    val preguntaSeguridad: String? = null
) {
    companion object {
        fun empty(): ApiUsuarioRegistroResponse = ApiUsuarioRegistroResponse(
            id = 0,
            email = "",
            nombre = ""
        )
    }
}

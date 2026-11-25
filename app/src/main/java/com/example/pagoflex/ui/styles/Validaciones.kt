package com.example.pagoflex.ui.styles

import com.example.pagoflex.security.PasswordHasher

/**
 * Reglas de validacion compartidas por los formularios.
 *
 * Cada funcion devuelve el mensaje que debe mostrarse debajo del campo
 * correspondiente, o `null` cuando el valor es valido.
 */
object Validaciones {

    private val formatoCorreo = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun nombre(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu nombre completo"
        valor.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres"
        else -> null
    }

    fun correo(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu correo electrónico"
        !formatoCorreo.matches(valor.trim()) -> "Ingresa un correo válido, por ejemplo nombre@dominio.cl"
        else -> null
    }

    fun contrasena(valor: String): String? {
        if (valor.isEmpty()) return "Ingresa una contraseña"
        val errores = PasswordHasher.validate(valor)
        return if (errores.isEmpty()) null else errores.joinToString("\n")
    }

    fun confirmacion(contrasena: String, confirmacion: String): String? = when {
        confirmacion.isEmpty() -> "Repite la contraseña"
        contrasena != confirmacion -> "Las contraseñas no coinciden"
        else -> null
    }

    /**
     * Validacion estricta para el perfil: el telefono es obligatorio y debe
     * corresponder a un numero chileno (movil +56 9 o fijo de Santiago +56 2).
     */
    fun telefonoChileno(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) return "Ingresa tu teléfono"

        val caracteresPermitidos = texto.all { it.isDigit() || it in " +-()" }
        if (!caracteresPermitidos) {
            return "El teléfono solo puede contener números, espacios y los signos +, - o paréntesis"
        }

        val digitos = texto.filter { it.isDigit() }
        val nacional = when {
            digitos.startsWith("56") && digitos.length > 9 -> digitos.drop(2)
            else -> digitos
        }

        val esMovil = nacional.length == 9 && nacional.startsWith("9")
        val esFijoSantiago = nacional.length == 9 && nacional.startsWith("2")
        return if (esMovil || esFijoSantiago) {
            null
        } else {
            "Ingresa un teléfono chileno válido, por ejemplo +56 9 1234 5678"
        }
    }

    fun telefono(valor: String): String? {
        if (valor.isBlank()) return null // Campo opcional en otros formularios
        return telefonoChileno(valor)
    }

    fun empresa(valor: String): String? {
        val texto = valor.trim()
        if (texto.isEmpty()) return null // Campo opcional
        return if (texto.length < 3) "El nombre de la empresa debe tener al menos 3 caracteres" else null
    }

    fun descripcion(valor: String, minimo: Int = 10): String? = when {
        valor.isBlank() -> "Escribe una descripción"
        valor.trim().length < minimo -> "La descripción debe tener al menos $minimo caracteres"
        else -> null
    }

    fun motivoAnulacion(valor: String): String? = when {
        valor.isBlank() -> "Indica el motivo de la anulación"
        valor.trim().length < 5 -> "El motivo debe tener al menos 5 caracteres"
        else -> null
    }
}

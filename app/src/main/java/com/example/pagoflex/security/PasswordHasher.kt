package com.example.pagoflex.security

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 120000
    private const val KEY_LENGTH = 256

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec)
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun verify(password: String, salt: String, hash: String): Boolean {
        return MessageDigest.isEqual(hash(password, salt).toByteArray(), hash.toByteArray())
    }

    fun validate(password: String): List<String> {
        val errors = mutableListOf<String>()
        if (password.length < 8) errors.add("Debe tener al menos 8 caracteres")
        if (!password.any { it.isUpperCase() }) errors.add("Debe incluir al menos una letra mayúscula")
        if (!password.any { it.isLowerCase() }) errors.add("Debe incluir al menos una letra minúscula")
        if (!password.any { it.isDigit() }) errors.add("Debe incluir al menos un número")
        if (!password.any { !it.isLetterOrDigit() }) errors.add("Debe incluir al menos un carácter especial")
        return errors
    }
}

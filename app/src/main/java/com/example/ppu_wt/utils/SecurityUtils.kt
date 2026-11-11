package com.example.ppu_wt.utils

import java.security.MessageDigest

object SecurityUtils {
    /**
     * Convierte un texto plano (contraseña) en un Hash SHA-256 seguro.
     */
    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
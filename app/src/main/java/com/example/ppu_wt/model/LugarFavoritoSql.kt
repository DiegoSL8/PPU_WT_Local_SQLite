package com.example.ppu_wt.model

/**
 * Modelo de datos puro para SQLite.
 * Representa un registro en la tabla de favoritos sin dependencias de la nube.
 */
data class LugarFavoritoSql(
    val id: Int = 0,               // SQLite usa enteros autoincrementales (Firebase usaba String)
    val idUsuario: String = "",    // Clave Foránea (El correo del usuario que lo guardó)
    val nombre: String = "",       // Nombre personalizado del lugar
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)
package com.example.ppu_wt.db

import android.provider.BaseColumns

/**
 * Contrato de la Base de Datos.
 * Centraliza los nombres de tablas, columnas y sentencias SQL para evitar errores de tipeo.
 */
object DatabaseContract {

    const val DATABASE_NAME = "ppu_weather.db"
    const val DATABASE_VERSION = 1

    // Definición de la tabla Favoritos
    object FavoritosEntry : BaseColumns {
        const val TABLE_NAME = "lugares_favoritos"

        // Las columnas de nuestra tabla relacional
        const val COLUMN_ID = BaseColumns._ID // Android prefiere _id para claves primarias
        const val COLUMN_ID_USUARIO = "id_usuario" // Clave foránea para saber de quién es el favorito
        const val COLUMN_NOMBRE = "nombre"
        const val COLUMN_LATITUD = "latitud"
        const val COLUMN_LONGITUD = "longitud"

        // Sentencia DDL (Data Definition Language) para construir la tabla
        const val SQL_CREATE_ENTRIES =
            "CREATE TABLE $TABLE_NAME (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_ID_USUARIO TEXT NOT NULL," +
                    "$COLUMN_NOMBRE TEXT NOT NULL," +
                    "$COLUMN_LATITUD REAL NOT NULL," +
                    "$COLUMN_LONGITUD REAL NOT NULL)"

        // Sentencia para destruir la tabla en caso de actualización
        const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_NAME"
    }

    // Aquí podríamos agregar object UsuariosEntry si decidimos guardar los usuarios localmente después.
}
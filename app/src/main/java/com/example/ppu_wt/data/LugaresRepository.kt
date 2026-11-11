package com.example.ppu_wt.data

import android.content.ContentValues
import android.content.Context
import com.example.ppu_wt.model.LugarFavoritoSql

class LugaresRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    // CREATE: Guarda un nuevo favorito
    fun insertarFavorito(usuarioId: String, nombre: String, lat: Double, lon: Double): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_USUARIO_ID, usuarioId)
            put(DatabaseHelper.COLUMN_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_LATITUD, lat)
            put(DatabaseHelper.COLUMN_LONGITUD, lon)
        }
        val id = db.insert(DatabaseHelper.TABLE_FAVORITOS, null, values)
        db.close()
        return id
    }

    // READ: Obtiene la lista de favoritos de un usuario
    fun obtenerFavoritos(usuarioId: String): List<LugarFavoritoSql> {
        val lista = mutableListOf<LugarFavoritoSql>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_FAVORITOS,
            null,
            "${DatabaseHelper.COLUMN_USUARIO_ID} = ?",
            arrayOf(usuarioId),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOMBRE))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LATITUD))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LONGITUD))

                // ¡Importante! Aquí usamos la clase del paquete .model.
                lista.add(LugarFavoritoSql(id, usuarioId, nombre, lat, lon))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return lista
    }

    // VALIDACIÓN: Evita que se guarde el mismo lugar dos veces
    fun existeFavorito(usuarioId: String, nombre: String, lat: Double, lon: Double): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_FAVORITOS,
            arrayOf(DatabaseHelper.COLUMN_ID),
            "${DatabaseHelper.COLUMN_USUARIO_ID} = ? AND ${DatabaseHelper.COLUMN_NOMBRE} = ? AND ${DatabaseHelper.COLUMN_LATITUD} = ? AND ${DatabaseHelper.COLUMN_LONGITUD} = ?",
            arrayOf(usuarioId, nombre, lat.toString(), lon.toString()),
            null, null, null
        )
        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    // DELETE: Elimina un lugar usando su ID
    fun eliminarFavorito(idFavorito: Int): Boolean {
        val db = dbHelper.writableDatabase
        val filasBorradas = db.delete(
            DatabaseHelper.TABLE_FAVORITOS,
            "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(idFavorito.toString())
        )
        db.close()
        return filasBorradas > 0
    }

    // UPDATE: Actualiza el nombre de un lugar existente
    fun actualizarNombreFavorito(idFavorito: Int, nuevoNombre: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_NOMBRE, nuevoNombre)
        }
        val filasActualizadas = db.update(
            DatabaseHelper.TABLE_FAVORITOS,
            values,
            "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(idFavorito.toString())
        )
        db.close()
        return filasActualizadas > 0
    }
}
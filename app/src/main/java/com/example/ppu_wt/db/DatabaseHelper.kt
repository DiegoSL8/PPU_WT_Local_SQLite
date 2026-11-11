package com.example.ppu_wt.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ppu_weather.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_FAVORITOS = "favoritos"
        const val COLUMN_ID = "id"
        const val COLUMN_USUARIO_ID = "usuario_id"
        const val COLUMN_NOMBRE = "nombre"
        const val COLUMN_LATITUD = "latitud"
        const val COLUMN_LONGITUD = "longitud"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_FAVORITOS ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "$COLUMN_USUARIO_ID TEXT,"
                + "$COLUMN_NOMBRE TEXT,"
                + "$COLUMN_LATITUD REAL,"
                + "$COLUMN_LONGITUD REAL" + ")")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITOS")
        onCreate(db)
    }
}
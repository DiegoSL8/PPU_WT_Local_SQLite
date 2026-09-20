package com.example.ppu_wt

// Este es el molde para Firebase
data class LugarFavorito(
    val id: String = "",           // Firebase le pondrá un ID único
    val nombre: String = "",       // Ej: "Playa Cavancha"
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)
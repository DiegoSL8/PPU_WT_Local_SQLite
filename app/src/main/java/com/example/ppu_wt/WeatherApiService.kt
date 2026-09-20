package com.example.ppu_wt

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    // Le indicamos la ruta de la API de Open-Meteo
    @GET("v1/forecast")
    fun obtenerClimaActual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        // Pedimos específicamente temperatura, viento y si es de día/noche
        @Query("current") variables: String = "temperature_2m,wind_speed_10m,is_day",
        @Query("timezone") zonaHoraria: String = "auto"
    ): Call<ClimaResponse>

}
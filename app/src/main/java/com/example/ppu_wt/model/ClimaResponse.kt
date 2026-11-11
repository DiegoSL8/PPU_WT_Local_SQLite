package com.example.ppu_wt.model

import com.google.gson.annotations.SerializedName

data class ClimaResponse(
    @SerializedName("current") val actual: CurrentWeather
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperatura: Double,
    @SerializedName("wind_speed_10m") val viento: Double,
    @SerializedName("is_day") val esDeDia: Int
)
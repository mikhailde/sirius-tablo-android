package com.example.tabloapp.data.remote.api

import com.example.tabloapp.data.remote.dto.ForecastWeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface WeatherApi {
    @GET("v2/forecast")
    suspend fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Header("X-Yandex-Weather-Key") apiKey: String
    ): Response<ForecastWeatherResponse>
}

package com.example.tabloapp.data.remote.api

import com.example.tabloapp.data.model.WeatherData
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface WeatherApi {

    companion object {
        private const val BASE_URL = "http://10.0.2.2/"

        fun create(): WeatherApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(WeatherApi::class.java)
        }
    }

    @GET("/api/v1/weather")
    suspend fun getWeatherData(): Response<WeatherData>
}
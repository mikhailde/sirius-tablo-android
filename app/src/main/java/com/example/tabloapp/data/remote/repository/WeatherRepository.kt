package com.example.tabloapp.data.remote.repository

import com.example.tabloapp.data.model.WeatherData
import com.example.tabloapp.data.remote.api.WeatherApi
import com.example.tabloapp.di.NetworkModule

class WeatherRepository {

    private val weatherApi: WeatherApi = NetworkModule.provideWeatherApi(NetworkModule.provideRetrofit(NetworkModule.provideOkHttpClient()))

    suspend fun getWeatherData(lat: Double, lon: Double, apiKey: String): WeatherData? {
        val response = weatherApi.getWeather(lat, lon, apiKey)
        return if (response.isSuccessful) {
            val weatherResponse = response.body()
            weatherResponse?.let {
                WeatherData(
                    temperature = it.fact.temp,
                    condition = it.fact.condition,
                    icon = it.fact.icon.substringAfterLast("/").substringBefore(".svg") // Извлекаем имя иконки
                )
            }
        } else {
            null
        }
    }
}
package com.example.tabloapp.data.repository

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
                    icon = "https://yastatic.net/weather/i/icons/funky/dark/${it.fact.icon}.svg"
                )
            }
        } else {
            null
        }
    }
}

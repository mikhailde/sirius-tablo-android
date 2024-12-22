package com.example.tabloapp.data.remote.repository

import android.util.Log
import com.example.tabloapp.data.model.WeatherData
import com.example.tabloapp.data.remote.api.WeatherApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherRepository(private val api: WeatherApi = WeatherApi.create()) {

    suspend fun getWeatherData(): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getWeatherData()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Log.e("WeatherRepository", "Request failed with code: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather data", e)
                null
            }
        }
    }
}
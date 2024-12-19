package com.example.tabloapp.data.remote.repository

import android.util.Log
import com.example.tabloapp.data.model.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WeatherRepository {

    private val client = OkHttpClient()

    suspend fun getWeatherData(lat: Double, lon: Double, apiKey: String): WeatherData? {
        // Заменить URL на ваш Weather Service, проходящий через API Gateway
        val url = "http://10.0.2.2/api/v1/weather" // Укажите ваш сервер

        val request = Request.Builder()
            .url(url)
            .build()

        return withContext(Dispatchers.IO) { // Переключаем контекст на IO dispatcher
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d("WeatherRepository", "Response body: $responseBody")
                        val jsonObject = JSONObject(responseBody)
                        Log.d("WeatherRepository", "JSON object: $jsonObject")
                        WeatherData(
                            temperature = jsonObject.getInt("temperature"),
                            condition = jsonObject.getString("condition"),
                            icon = jsonObject.getString("icon")
                        )
                    } else {
                        Log.e("WeatherRepository", "Request failed with code: ${response.code}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather data", e)
                null
            }
        }
    }
}
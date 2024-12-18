package com.example.tabloapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.tabloapp.data.repository.WeatherRepository
import com.example.tabloapp.service.mqtt.MqttService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), MqttService.MqttMessageListener {

    private lateinit var timeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var dayOfWeekTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var weatherIconWebView: WebView
    private lateinit var weatherDescriptionTextView: TextView
    private lateinit var messageTextView: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val weatherRepository = WeatherRepository()
    private val apiKey = BuildConfig.YANDEX_WEATHER_API_KEY
    private lateinit var mqttService: MqttService

    private val updateTimeRunnable: Runnable by lazy {
        Runnable {
            updateTime()
            handler.postDelayed(updateTimeRunnable, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timeTextView = findViewById(R.id.timeTextView)
        dateTextView = findViewById(R.id.dateTextView)
        dayOfWeekTextView = findViewById(R.id.dayOfWeekTextView)
        temperatureTextView = findViewById(R.id.temperatureTextView)
        weatherIconWebView = findViewById(R.id.weatherIconImageView)
        weatherDescriptionTextView = findViewById(R.id.weatherDescriptionTextView)
        messageTextView = findViewById(R.id.messageTextView)

        // Настройка MQTT
        mqttService = MqttService(applicationContext, this)
        mqttService.connect()

        fetchWeatherData()
    }

    override fun onResume() {
        super.onResume()
        startUpdatingTime()
    }

    override fun onPause() {
        super.onPause()
        stopUpdatingTime()
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            messageTextView.isSelected = false
            messageTextView.text = message
            messageTextView.isSelected = true // Start marquee
        }
    }

    private fun updateTime() {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        val currentDayOfWeek = SimpleDateFormat("EE", Locale.getDefault()).format(Date())

        timeTextView.text = currentTime
        dateTextView.text = currentDate
        dayOfWeekTextView.text = currentDayOfWeek
    }

    private fun startUpdatingTime() {
        handler.post(updateTimeRunnable)
    }

    private fun stopUpdatingTime() {
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun fetchWeatherData() {
        lifecycleScope.launch {
            if (apiKey.isNotEmpty()) {
                try {
                    val weatherData =
                        weatherRepository.getWeatherData(43.40, 39.96, apiKey) // Замените на нужные координаты
                    temperatureTextView.text =
                        getString(R.string.temperature_celsius, weatherData!!.temperature)
                    weatherDescriptionTextView.text = weatherData.condition

                    // Загрузка иконки в WebView
                    weatherIconWebView.loadUrl(weatherData.icon)

                    // Добавьте отображение пробок (заглушка)
                    val trafficTextView: TextView = findViewById(R.id.trafficTextView)
                    trafficTextView.text = getString(R.string.traffic_placeholder)

                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to fetch weather data: ${e.message}")
                }
            } else {
                Log.e("MainActivity", "API key is not set!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttService.disconnect()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error disconnecting from MQTT: ${e.message}")
        }
    }
}
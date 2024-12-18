package com.example.tabloapp.ui.features.main.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.tabloapp.BuildConfig
import com.example.tabloapp.R
import com.example.tabloapp.data.remote.repository.WeatherRepository
import com.example.tabloapp.service.mqtt.MqttService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), MqttService.MqttMessageListener {

    private val weatherRepository = WeatherRepository()
    private lateinit var mqttService: MqttService
    private lateinit var weatherIconImageView: ImageView
    private lateinit var imageLoader: ImageLoader

    // UI elements
    private val timeTextView: TextView by lazy { findViewById(R.id.timeTextView) }
    private val dateTextView: TextView by lazy { findViewById(R.id.dateTextView) }
    private val dayOfWeekTextView: TextView by lazy { findViewById(R.id.dayOfWeekTextView) }
    private val temperatureTextView: TextView by lazy { findViewById(R.id.temperatureTextView) }
    private val weatherDescriptionTextView: TextView by lazy { findViewById(R.id.weatherDescriptionTextView) }
    private val messageTextView: TextView by lazy { findViewById(R.id.messageTextView) }
    private val trafficTextView: TextView by lazy { findViewById(R.id.trafficTextView) }

    // Handlers
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create the ImageLoader
        imageLoader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()

        weatherIconImageView = findViewById(R.id.weatherIconImageView)
        mqttService = MqttService(applicationContext, this)
        mqttService.connect()

        handler.post(updateTimeRunnable)
        handler.post(updateWeatherRunnable)
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            messageTextView.text = message
            messageTextView.isSelected = true // Enable marquee effect for long messages
        }
    }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000) // Update every second
        }
    }

    private val updateWeatherRunnable = object : Runnable {
        override fun run() {
            fetchWeatherData()
            handler.postDelayed(this, 90 * 60 * 1000) // Update every 90 minutes
        }
    }

    private fun updateDateTime() {
        val currentDateTime = SimpleDateFormat("HH:mm dd.MM.yyyy EE", Locale.getDefault()).format(Date())
        timeTextView.text = currentDateTime.substringBefore(" ")
        dateTextView.text = currentDateTime.substringAfter(" ").substringBeforeLast(" ")
        dayOfWeekTextView.text = currentDateTime.substringAfterLast(" ")
    }

    private fun fetchWeatherData() {
        lifecycleScope.launch {
            try {
                val weatherData = weatherRepository.getWeatherData(
                    43.40, 39.96, BuildConfig.YANDEX_WEATHER_API_KEY
                )
                Log.d("IconValue", weatherData?.icon ?: "Icon is null")

                // Load the SVG icon using Coil
                val request = ImageRequest.Builder(applicationContext)
                    .data("https://yastatic.net/weather/i/icons/funky/dark/${weatherData?.icon}.svg")
                    .target(weatherIconImageView)
                    .build()
                imageLoader.enqueue(request)

                temperatureTextView.text = getString(R.string.temperature_celsius, weatherData?.temperature ?: "")
                weatherDescriptionTextView.text = weatherData?.condition ?: ""
                trafficTextView.text = getString(R.string.traffic_placeholder) // Consider fetching traffic data
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch weather data: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttService.disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}
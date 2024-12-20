package com.example.tabloapp.ui.features.main.view

import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.caverock.androidsvg.SVG
import com.example.tabloapp.BuildConfig
import com.example.tabloapp.R
import com.example.tabloapp.data.model.DeviceStatus
import com.example.tabloapp.data.remote.repository.WeatherRepository
import com.example.tabloapp.service.mqtt.MqttService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), MqttService.MqttMessageListener {

    companion object {
        private const val API_KEY = "your_api_key_here" // Замени на свой API ключ
        private const val DEVICE_ID = "1"
    }

    private val weatherRepository = WeatherRepository()
    private lateinit var mqttService: MqttService
    private lateinit var weatherIconImageView: ImageView

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

    private val conditionTranslations = mapOf(
        "clear" to "Ясно",
        "partly-cloudy" to "Малооблачно",
        "cloudy" to "Облачно с прояснениями",
        "overcast" to "Пасмурно",
        "drizzle" to "Морось",
        "light-rain" to "Небольшой дождь",
        "rain" to "Дождь",
        "moderate-rain" to "Умеренно сильный дождь",
        "heavy-rain" to "Сильный дождь",
        "continuous-heavy-rain" to "Длительный сильный дождь",
        "showers" to "Ливень",
        "wet-snow" to "Дождь со снегом",
        "light-snow" to "Небольшой снег",
        "snow" to "Снег",
        "snow-showers" to "Снегопад",
        "hail" to "Град",
        "thunderstorm" to "Гроза",
        "thunderstorm-with-rain" to "Дождь с грозой",
        "thunderstorm-with-hail" to "Гроза с градом"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        weatherIconImageView = findViewById(R.id.weatherIconImageView)
        mqttService = MqttService(applicationContext, this)
        mqttService.connect()

        handler.post(updateTimeRunnable)
        handler.post(updateWeatherRunnable)
        handler.post(sendStatusRunnable)
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

    private val sendStatusRunnable = object : Runnable {
        override fun run() {
            val status = "online"
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val message = messageTextView.text.toString()
            val brightness = 50 // Замени на реальное значение
            val temperature = 25 // Замени на реальное значение
            val freeSpace = getFreeSpace()
            val uptime = android.os.SystemClock.elapsedRealtime() / 1000

            // Передаем API_KEY как последний аргумент
            val deviceStatus = DeviceStatus(DEVICE_ID, status, currentTime, message, brightness, temperature, freeSpace, uptime, API_KEY)
            mqttService.publishDeviceStatus(deviceStatus)

            handler.postDelayed(this, 30000) // Update every 30 seconds
        }
    }

    private fun getFreeSpace(): Long {
        val stat = StatFs(cacheDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024) // Free space in MB
    }

    private fun updateDateTime() {
        val russianLocale = Locale("ru", "RU")
        val currentDateTime = SimpleDateFormat("HH:mm dd.MM.yyyy EE", russianLocale).format(Date())
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

                withContext(Dispatchers.IO) {
                    val pictureDrawable = loadSvgFromUrl(weatherData?.icon)
                    withContext(Dispatchers.Main) {
                        weatherIconImageView.setImageDrawable(pictureDrawable)
                    }
                }

                temperatureTextView.text = getString(R.string.temperature_celsius, weatherData?.temperature ?: "")
                weatherDescriptionTextView.text = conditionTranslations[weatherData?.condition] ?: weatherData?.condition ?: ""
                trafficTextView.text = getString(R.string.traffic_placeholder)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch weather data: ${e.message}")
            }
        }
    }

    private fun loadSvgFromUrl(iconUrl: String?): PictureDrawable? {
        iconUrl ?: return null

        return try {
            val inputStream: InputStream = URL(iconUrl).openStream()
            val svg = SVG.getFromInputStream(inputStream)
            PictureDrawable(svg.renderToPicture())
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading SVG from URL: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttService.disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}
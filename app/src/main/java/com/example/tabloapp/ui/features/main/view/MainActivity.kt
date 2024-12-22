package com.example.tabloapp.ui.features.main.view

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.tabloapp.R
import com.example.tabloapp.data.model.DeviceStatus
import com.example.tabloapp.data.remote.repository.WeatherRepository
import com.example.tabloapp.service.mqtt.MqttService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), MqttService.MqttMessageListener {

    companion object {
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

            val deviceStatus = DeviceStatus(DEVICE_ID, status, currentTime, message, brightness, temperature, freeSpace, uptime)
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
                val weatherData = weatherRepository.getWeatherData()
                Log.d("IconValue", weatherData?.icon ?: "Icon is null")

                withContext(Dispatchers.Main) {
                    val iconName = weatherData?.icon?.replace("-", "_")?.replace("+", "_")
                    val drawable = getLocalSvg(iconName)
                    weatherIconImageView.setImageDrawable(drawable)
                }

                temperatureTextView.text = getString(R.string.temperature_celsius, weatherData?.temperature ?: "")
                weatherDescriptionTextView.text = conditionTranslations[weatherData?.condition] ?: weatherData?.condition ?: ""
                trafficTextView.text = getString(R.string.traffic_placeholder)
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch weather data: ${e.message}")
            }
        }
    }

    private fun getLocalSvg(iconName: String?): Drawable? {
        if (iconName == null) return null

        return try {
            val resourceId = resources.getIdentifier(iconName, "drawable", packageName)
            if (resourceId != 0) {
                resources.getDrawable(resourceId, theme)
            } else {
                Log.w("MainActivity", "Icon not found in resources: $iconName")
                null
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading SVG from resources: ${e.message}")
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttService.disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}
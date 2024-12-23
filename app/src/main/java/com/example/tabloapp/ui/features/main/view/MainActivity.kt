package com.example.tabloapp.ui.features.main.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import android.widget.TextView
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.text.HtmlCompat
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
import android.provider.Settings
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MainActivity : ComponentActivity(), MqttService.MqttMessageListener, SensorEventListener {

    companion object {
        private const val DEVICE_ID = "1"
        private const val WEATHER_UPDATE_INTERVAL_MS = 90 * 60 * 1000L
        private const val STATUS_UPDATE_INTERVAL_MS = 30 * 1000L
    }

    private val weatherRepository = WeatherRepository()
    private lateinit var mqttService: MqttService
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private var temperatureSensor: Sensor? = null

    // UI elements
    private val timeTextView: TextView by lazy { findViewById(R.id.timeTextView) }
    private val dateTextView: TextView by lazy { findViewById(R.id.dateTextView) }
    private val dayOfWeekTextView: TextView by lazy { findViewById(R.id.dayOfWeekTextView) }
    private val temperatureTextView: TextView by lazy { findViewById(R.id.temperatureTextView) }
    private val messageTextView: TextView by lazy { findViewById(R.id.messageTextView) }
    private val weatherIconImageView: ImageView by lazy { findViewById(R.id.weatherIconImageView) }

    // Handlers
    private val handler = Handler(Looper.getMainLooper())

    // Sensor values
    private var currentBrightness: Float = 0f
    private var currentTemperature: Float = 0f

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

        mqttService = MqttService(applicationContext, this)
        mqttService.connect()
        messageTextView.text = getString(R.string.welcome_message)

        // Активируем marquee эффект
        messageTextView.isSelected = true

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        handler.post(updateTimeRunnable)
        handler.post(updateWeatherRunnable)
        handler.post(sendStatusRunnable)
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            val formattedMessage = message.replace("\n", "<br>")
            messageTextView.text = HtmlCompat.fromHtml(formattedMessage, HtmlCompat.FROM_HTML_MODE_COMPACT)
            messageTextView.isSelected = true // Повторная установка selected после обновления текста

            // Проверка длины текста и установка singleLine и ellipsize в зависимости от неё
            if (formattedMessage.length > 50) {
                messageTextView.setSingleLine(true)
                messageTextView.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
            } else {
                messageTextView.setSingleLine(false)
                messageTextView.ellipsize = null
            }
        }
    }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
        }
    }

    private val updateWeatherRunnable = object : Runnable {
        override fun run() {
            fetchWeatherData()
            handler.postDelayed(this, WEATHER_UPDATE_INTERVAL_MS)
        }
    }

    private val sendStatusRunnable = object : Runnable {
        override fun run() {
            sendStatusUpdate()
            handler.postDelayed(this, STATUS_UPDATE_INTERVAL_MS)
        }
    }

    private fun sendStatusUpdate() {
        val status = "online"
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val message = messageTextView.text.toString()
        val brightness = currentBrightness.toInt()
        val temperature = currentTemperature.toInt()
        val freeSpace = getFreeSpace()
        val uptime = android.os.SystemClock.elapsedRealtime() / 1000

        val deviceStatus = DeviceStatus(
            DEVICE_ID,
            status,
            currentTime,
            message,
            brightness,
            temperature,
            freeSpace,
            uptime
        )
        mqttService.publishDeviceStatus(deviceStatus)
    }

    private fun getFreeSpace(): Long {
        val stat = StatFs(cacheDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)
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
                withContext(Dispatchers.Main) {
                    val iconName = weatherData?.icon?.replace("-", "_")?.replace("+", "_")
                    val icon = resources.getIdentifier(iconName, "drawable", packageName)
                    weatherIconImageView.setImageResource(icon)
                    temperatureTextView.text =
                        getString(R.string.temperature_celsius, weatherData?.temperature ?: "")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to fetch weather data: ${e.message}")
                withContext(Dispatchers.Main) {
                    messageTextView.text = getString(R.string.data_unavailable)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            currentBrightness = event.values[0]
            Log.d("Sensor", "Light: ${event.values[0]}")
        } else if (event?.sensor?.type == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            currentTemperature = event.values[0]
            Log.d("Sensor", "Temperature: ${event.values[0]}")
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        temperatureSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttService.disconnect()
        handler.removeCallbacksAndMessages(null)
    }
}
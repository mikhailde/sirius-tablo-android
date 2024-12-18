package com.example.tabloapp.service.mqtt

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck
import java.util.UUID

class MqttService(private val context: Context, private val listener: MqttMessageListener) {

    private val client: Mqtt3AsyncClient = MqttClient.builder()
        .identifier(UUID.randomUUID().toString()) // Уникальный идентификатор клиента
        .serverHost("10.0.2.2") // Замените на адрес вашего MQTT брокера
        .serverPort(1883)
        .useMqttVersion3()
        .buildAsync()

    private val topic = "device/1/command" // Замените на топик для подписки

    fun connect() {
        client.connectWith()
            .simpleAuth()
            .username("your_username")  // Опционально: укажите имя пользователя
            .password("your_password".toByteArray()) // Опционально: укажите пароль
            .applySimpleAuth()
            .send()
            .whenComplete { connAck: Mqtt3ConnAck, throwable: Throwable? ->
                if (throwable != null) {
                    Log.e("MqttService", "Connection failure: ${throwable.message}")
                } else {
                    Log.d("MqttService", "Connection success: ${connAck.returnCode}")
                    subscribeToTopic()
                }
            }
    }

    private fun subscribeToTopic() {
        client.subscribeWith()
            .topicFilter(topic)
            .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
            .callback { publish: Mqtt3Publish ->
                val message = String(publish.payloadAsBytes)
                Log.d("MqttService", "Message received: $message")
                listener.onMessageReceived(message)
            }
            .send()
            .whenComplete { subAck: Mqtt3SubAck, throwable: Throwable? ->
                if (throwable != null) {
                    Log.w("MqttService", "Failed to subscribe to topic: $topic")
                } else {
                    Log.i("MqttService", "Subscribed to topic: $topic")
                }
            }
    }

    fun disconnect() {
        client.disconnect()
    }

    interface MqttMessageListener {
        fun onMessageReceived(message: String)
    }
}

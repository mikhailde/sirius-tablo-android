package com.example.tabloapp.service.mqtt

import android.content.Context
import android.util.Log
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.util.UUID

class MqttService(private val context: Context, private val messageListener: MqttMessageListener) {

    private val client: Mqtt3AsyncClient = MqttClient.builder()
        .identifier(UUID.randomUUID().toString())
        .serverHost("10.0.2.2") // Замените на адрес вашего MQTT брокера
        .serverPort(1883)
        .useMqttVersion3()
        .buildAsync()

    private val topic = "device/tablo_01/command"

    fun connect() {
        client.connectWith()
            .send()
            .whenComplete { connAck: Mqtt3ConnAck, throwable: Throwable? ->
                if (throwable != null) {
                    Log.e("MqttService", "Connection failure: ${throwable.message}", throwable)
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
                Log.d("MqttService", "Message received on topic '$topic': $message")
                messageListener.onMessageReceived(message)
            }
            .send()
            .whenComplete { subAck, throwable ->
                if (throwable != null) {
                    Log.e("MqttService", "Failed to subscribe to topic: $topic", throwable)
                } else {
                    Log.i("MqttService", "Subscribed to topic: $topic")
                }
            }
    }

    fun disconnect() {
        client.disconnect()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e("MqttService", "Disconnection failed", throwable)
                } else {
                    Log.i("MqttService", "Disconnected")
                }
            }
    }

    interface MqttMessageListener {
        fun onMessageReceived(message: String)
    }
}

package com.example.tabloapp.service.mqtt

import android.content.Context
import android.util.Log
import com.example.tabloapp.data.model.DeviceStatus
import com.google.gson.Gson
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.util.UUID

class MqttService(private val context: Context, private val messageListener: MqttMessageListener) {

    companion object {
        private const val TAG = "MqttService"
    }

    private val client: Mqtt3AsyncClient = MqttClient.builder()
        .identifier(UUID.randomUUID().toString())
        .serverHost("10.0.2.2") // Замените на адрес вашего MQTT брокера
        .serverPort(1883)
        .useMqttVersion3()
        .buildAsync()

    private val commandTopic = "device/1/command"
    private val statusTopic = "device/1/status" // Топик для отправки статуса

    fun connect() {
        client.connectWith()
            .send()
            .whenComplete { connAck: Mqtt3ConnAck, throwable: Throwable? ->
                if (throwable != null) {
                    Log.e(TAG, "Connection failure: ${throwable.message}", throwable)
                } else {
                    Log.d(TAG, "Connection success: ${connAck.returnCode}")
                    subscribeToTopic()
                }
            }
    }

    private fun subscribeToTopic() {
        client.subscribeWith()
            .topicFilter(commandTopic)
            .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
            .callback { publish: Mqtt3Publish ->
                val message = String(publish.payloadAsBytes)
                Log.d(TAG, "Message received on topic '$commandTopic': $message")
                messageListener.onMessageReceived(message)
            }
            .send()
            .whenComplete { subAck, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Failed to subscribe to topic: $commandTopic", throwable)
                } else {
                    Log.i(TAG, "Subscribed to topic: $commandTopic")
                }
            }
    }

    fun disconnect() {
        client.disconnect()
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Disconnection failed", throwable)
                } else {
                    Log.i(TAG, "Disconnected")
                }
            }
    }

    // Функция для публикации статуса устройства
    fun publishDeviceStatus(deviceStatus: DeviceStatus) {
        val payload = Gson().toJson(deviceStatus)
        client.publishWith()
            .topic(statusTopic)
            .payload(payload.toByteArray())
            .qos(com.hivemq.client.mqtt.datatypes.MqttQos.AT_LEAST_ONCE)
            .send()
            .whenComplete { publishResult, throwable ->
                if (throwable != null) {
                    Log.e(TAG, "Failed to publish device status", throwable)
                } else {
                    Log.d(TAG, "Device status published: $payload")
                }
            }
    }

    interface MqttMessageListener {
        fun onMessageReceived(message: String)
    }
}

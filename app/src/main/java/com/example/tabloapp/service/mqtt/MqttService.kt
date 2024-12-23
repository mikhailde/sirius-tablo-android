package com.example.tabloapp.service.mqtt

import android.util.Log
import com.example.tabloapp.data.model.DeviceStatus
import com.google.gson.Gson
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.util.UUID
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck
import com.hivemq.client.mqtt.datatypes.MqttQos

class MqttService(private val messageListener: MqttMessageListener) {

    companion object {
        private const val TAG = "MqttService"
        private const val SERVER_HOST = "10.0.2.2"
        private const val SERVER_PORT = 1883
        private const val CLIENT_ID_PREFIX = "tablo-android-"
        private const val COMMAND_TOPIC_FORMAT = "device/%s/command"
        private const val STATUS_TOPIC_FORMAT = "device/%s/status"
    }

    private val client: Mqtt3AsyncClient = MqttClient.builder()
        .identifier(CLIENT_ID_PREFIX + UUID.randomUUID().toString())
        .serverHost(SERVER_HOST)
        .serverPort(SERVER_PORT)
        .useMqttVersion3()
        .buildAsync()

    private lateinit var commandTopic: String
    private lateinit var statusTopic: String

    fun connect(deviceId: String = "1") {
        commandTopic = String.format(COMMAND_TOPIC_FORMAT, deviceId)
        statusTopic = String.format(STATUS_TOPIC_FORMAT, deviceId)

        client.connectWith()
            .send()
            .whenComplete { connAck: Mqtt3ConnAck, throwable: Throwable? ->
                handleConnectionResult(connAck, throwable)
            }
    }

    private fun handleConnectionResult(connAck: Mqtt3ConnAck, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(TAG, "Connection failure: ${throwable.message}", throwable)
        } else {
            Log.d(TAG, "Connection success: ${connAck.returnCode}")
            subscribeToCommandTopic()
        }
    }

    private fun subscribeToCommandTopic() {
        client.subscribeWith()
            .topicFilter(commandTopic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .callback(::handleIncomingMessage)
            .send()
            .whenComplete { _: Mqtt3SubAck, throwable: Throwable? ->
                handleSubscriptionResult(throwable)
            }
    }

    private fun handleIncomingMessage(publish: Mqtt3Publish) {
        val message = String(publish.payloadAsBytes)
        Log.d(TAG, "Message received on topic '$commandTopic': $message")
        messageListener.onMessageReceived(message)
    }

    private fun handleSubscriptionResult(throwable: Throwable?) {
        if (throwable != null) {
            Log.e(TAG, "Failed to subscribe to topic: $commandTopic", throwable)
        } else {
            Log.i(TAG, "Subscribed to topic: $commandTopic")
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

    fun publishDeviceStatus(deviceStatus: DeviceStatus) {
        val payload = Gson().toJson(deviceStatus)
        client.publishWith()
            .topic(statusTopic)
            .payload(payload.toByteArray())
            .qos(MqttQos.AT_LEAST_ONCE)
            .send()
            .whenComplete { _, throwable ->
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
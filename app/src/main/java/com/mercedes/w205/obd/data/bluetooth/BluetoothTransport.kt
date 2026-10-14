package com.mercedes.w205.obd.data.bluetooth

import kotlinx.coroutines.flow.StateFlow

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    INITIALIZING_ELM,
    CONNECTED,
    ERROR
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isBle: Boolean
)

interface BluetoothTransport {
    val status: StateFlow<ConnectionStatus>
    suspend fun connect(device: BluetoothDeviceInfo): Boolean
    suspend fun disconnect()
    suspend fun sendCommand(cmd: String): String
    fun isConnected(): Boolean
}

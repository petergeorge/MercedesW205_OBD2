package com.mercedes.w205.obd.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ClassicBluetoothTransport(
    private val bluetoothAdapter: BluetoothAdapter?
) : BluetoothTransport {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private val sendMutex = Mutex()

    companion object {
        // Standard SPP (Serial Port Profile) UUID for ELM327 Bluetooth Classic
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val DEFAULT_TIMEOUT_MS = 1500L
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: BluetoothDeviceInfo): Boolean = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _status.value = ConnectionStatus.ERROR
            return@withContext false
        }

        disconnect()
        _status.value = ConnectionStatus.CONNECTING

        try {
            val btDevice: BluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
            bluetoothAdapter.cancelDiscovery()

            // Try standard RFCOMM socket first
            var sock: BluetoothSocket? = null
            try {
                sock = btDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                sock.connect()
            } catch (e: Exception) {
                // Fallback for tricky Bluetooth devices (reflection fallback method 1)
                try {
                    val m = btDevice.javaClass.getMethod("createRfcommSocket", Integer.TYPE)
                    sock = m.invoke(btDevice, 1) as BluetoothSocket
                    sock.connect()
                } catch (e2: Exception) {
                    throw e
                }
            }

            socket = sock
            inputStream = sock.inputStream
            outputStream = sock.outputStream

            _status.value = ConnectionStatus.CONNECTED
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            _status.value = ConnectionStatus.ERROR
            return@withContext false
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream = null
            outputStream = null
            socket = null
            _status.value = ConnectionStatus.DISCONNECTED
        }
        Unit
    }

    override suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val out = outputStream ?: return@withContext "ERROR: NOT CONNECTED"
            val input = inputStream ?: return@withContext "ERROR: NOT CONNECTED"

            // 1. Flush any stale bytes left in the input buffer before sending
            try {
                while (input.available() > 0) {
                    input.read()
                }
            } catch (e: Exception) {
                // Ignore flush errors
            }

            // 2. Transmit command terminated by carriage return
            val formattedCmd = (cmd.trim() + "\r").toByteArray(Charsets.US_ASCII)
            out.write(formattedCmd)
            out.flush()

            // 3. Read response until prompt character '>' or timeout
            val responseBuilder = StringBuilder()
            val startTime = System.currentTimeMillis()
            val buffer = ByteArray(256)
            val timeoutMs = if (cmd.startsWith("0100") || cmd.startsWith("ATSP")) 4500L else DEFAULT_TIMEOUT_MS

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                val avail = try { input.available() } catch (e: Exception) { 0 }
                if (avail > 0) {
                    val bytesToRead = minOf(avail, buffer.size)
                    val bytesRead = input.read(buffer, 0, bytesToRead)
                    if (bytesRead > 0) {
                        for (i in 0 until bytesRead) {
                            val c = buffer[i].toInt().toChar()
                            if (c == '>') {
                                // Prompt reached: command execution completed
                                val res = responseBuilder.toString().replace(">", "").trim()
                                return@withContext res
                            }
                            responseBuilder.append(c)
                        }
                    }
                } else {
                    delay(8)
                }
            }

            val finalStr = responseBuilder.toString().replace(">", "").trim()
            return@withContext if (finalStr.isNotEmpty()) finalStr else "TIMEOUT"
        }
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true && _status.value == ConnectionStatus.CONNECTED
    }
}

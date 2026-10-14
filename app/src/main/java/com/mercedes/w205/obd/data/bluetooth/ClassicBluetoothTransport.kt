package com.mercedes.w205.obd.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
        private const val READ_TIMEOUT_MS = 2500L
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

            val sock = btDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()

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

            val formattedCmd = (cmd.trim() + "\r").toByteArray(Charsets.US_ASCII)
            out.write(formattedCmd)
            out.flush()

            // Read until prompt character '>' from ELM327
            val responseBuilder = StringBuilder()
            val startTime = System.currentTimeMillis()

            val readResult = withTimeoutOrNull(READ_TIMEOUT_MS) {
                while (true) {
                    if (input.available() > 0) {
                        val b = input.read()
                        if (b == -1) break
                        val char = b.toChar()
                        if (char == '>') {
                            break // ELM327 command response finished
                        }
                        responseBuilder.append(char)
                    } else {
                        kotlinx.coroutines.delay(5)
                    }
                    if (System.currentTimeMillis() - startTime > READ_TIMEOUT_MS) {
                        break
                    }
                }
                responseBuilder.toString()
            }

            return@withContext readResult?.trim() ?: "TIMEOUT"
        }
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true && _status.value == ConnectionStatus.CONNECTED
    }
}

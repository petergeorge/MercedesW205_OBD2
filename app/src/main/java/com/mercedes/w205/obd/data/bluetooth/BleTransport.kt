package com.mercedes.w205.obd.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

@SuppressLint("MissingPermission")
class BleTransport(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) : BluetoothTransport {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private val sendMutex = Mutex()
    private val responseBuffer = StringBuilder()
    private var responseReady = false

    companion object {
        // Nordic UART Service & Common Transparent BLE UART UUIDs
        val NUS_SERVICE: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_RX_CHAR: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Write to Dongle
        val NUS_TX_CHAR: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Read/Notify from Dongle
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        
        // Alternative TI/vGate BLE UART UUIDs
        val VGATE_SERVICE: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        val VGATE_CHAR: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _status.value = ConnectionStatus.CONNECTING
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _status.value = ConnectionStatus.DISCONNECTED
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                var notifyChar: BluetoothGattCharacteristic? = null

                // Look for Nordic UART or vGate Transparent UART
                val nus = gatt.getService(NUS_SERVICE)
                if (nus != null) {
                    writeCharacteristic = nus.getCharacteristic(NUS_RX_CHAR)
                    notifyChar = nus.getCharacteristic(NUS_TX_CHAR)
                } else {
                    val vgate = gatt.getService(VGATE_SERVICE)
                    if (vgate != null) {
                        writeCharacteristic = vgate.getCharacteristic(VGATE_CHAR)
                        notifyChar = writeCharacteristic
                    }
                }

                if (notifyChar != null) {
                    gatt.setCharacteristicNotification(notifyChar, true)
                    val descriptor = notifyChar.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                    _status.value = ConnectionStatus.CONNECTED
                } else {
                    _status.value = ConnectionStatus.ERROR
                }
            } else {
                _status.value = ConnectionStatus.ERROR
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            val text = String(data, Charsets.US_ASCII)
            synchronized(responseBuffer) {
                responseBuffer.append(text)
                if (text.contains(">")) {
                    responseReady = true
                }
            }
        }
    }

    override suspend fun connect(device: BluetoothDeviceInfo): Boolean = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _status.value = ConnectionStatus.ERROR
            return@withContext false
        }

        disconnect()
        _status.value = ConnectionStatus.CONNECTING

        try {
            val btDevice = bluetoothAdapter.getRemoteDevice(device.address)
            gatt = btDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            _status.value = ConnectionStatus.ERROR
            return@withContext false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            gatt = null
            writeCharacteristic = null
            _status.value = ConnectionStatus.DISCONNECTED
        }
    }

    override suspend fun sendCommand(cmd: String): String = withContext(Dispatchers.IO) {
        sendMutex.withLock {
            val ch = writeCharacteristic ?: return@withContext "ERROR: NOT CONNECTED"
            val g = gatt ?: return@withContext "ERROR: NOT CONNECTED"

            synchronized(responseBuffer) {
                responseBuffer.clear()
                responseReady = false
            }

            val payload = (cmd.trim() + "\r").toByteArray(Charsets.US_ASCII)
            ch.value = payload
            g.writeCharacteristic(ch)

            val startTime = System.currentTimeMillis()
            val timeoutMs = 2500L

            val result = withTimeoutOrNull(timeoutMs) {
                while (!responseReady) {
                    kotlinx.coroutines.delay(10)
                    if (System.currentTimeMillis() - startTime > timeoutMs) break
                }
                synchronized(responseBuffer) {
                    responseBuffer.toString().replace(">", "").trim()
                }
            }

            return@withContext result ?: "TIMEOUT"
        }
    }

    override fun isConnected(): Boolean {
        return _status.value == ConnectionStatus.CONNECTED && gatt != null
    }
}

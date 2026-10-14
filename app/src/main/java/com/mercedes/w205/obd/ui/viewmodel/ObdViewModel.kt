package com.mercedes.w205.obd.ui.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mercedes.w205.obd.data.bluetooth.*
import com.mercedes.w205.obd.data.logger.TripDataLogger
import com.mercedes.w205.obd.data.obd.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class ObdViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var activeTransport: BluetoothTransport? = null
    private var elmProtocol: Elm327Protocol? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>("Not Connected")
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _vehicleLiveState = MutableStateFlow(VehicleLiveState())
    val vehicleLiveState: StateFlow<VehicleLiveState> = _vehicleLiveState.asStateFlow()

    private val _dtcFaults = MutableStateFlow<List<DtcFaultRecord>>(emptyList())
    val dtcFaults: StateFlow<List<DtcFaultRecord>> = _dtcFaults.asStateFlow()

    private val _isScanningDtc = MutableStateFlow(false)
    val isScanningDtc: StateFlow<Boolean> = _isScanningDtc.asStateFlow()

    private val _isClearingDtc = MutableStateFlow(false)
    val isClearingDtc: StateFlow<Boolean> = _isClearingDtc.asStateFlow()

    val tripLogger = TripDataLogger(application)
    private var tripStartTimeMs = 0L

    private var pollingJob: Job? = null

    fun connectDevice(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            disconnect()

            val transport: BluetoothTransport = if (device.isBle) {
                BleTransport(getApplication(), bluetoothAdapter)
            } else {
                ClassicBluetoothTransport(bluetoothAdapter)
            }
            activeTransport = transport

            // Forward status
            val statusJob = launch {
                transport.status.collect { s ->
                    _connectionStatus.value = s
                }
            }

            val connected = transport.connect(device)
            if (connected) {
                _connectedDeviceName.value = device.name
                _connectionStatus.value = ConnectionStatus.INITIALIZING_ELM

                val protocol = Elm327Protocol(transport)
                elmProtocol = protocol

                val initOk = protocol.initialize()
                if (initOk) {
                    _connectionStatus.value = ConnectionStatus.CONNECTED
                    startLivePolling()
                } else {
                    _connectionStatus.value = ConnectionStatus.ERROR
                    disconnect()
                }
            } else {
                _connectionStatus.value = ConnectionStatus.ERROR
                statusJob.cancel()
            }
        }
    }

    private fun startLivePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            val proto = elmProtocol ?: return@launch
            var slowCounter = 0

            while (isActive && activeTransport?.isConnected() == true) {
                try {
                    val updatedState = proto.pollCockpitCycle(_vehicleLiveState.value, slowCounter)
                    _vehicleLiveState.value = updatedState
                    slowCounter++

                    // Log sample if recording
                    if (tripLogger.isRecording.value) {
                        tripLogger.recordSample(updatedState, tripStartTimeMs)
                    }

                    delay(25) // High-frequency loop
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(500)
                }
            }
        }
    }

    fun startTripRecording() {
        viewModelScope.launch {
            tripStartTimeMs = System.currentTimeMillis()
            tripLogger.startRecording()
        }
    }

    fun stopTripRecording(onFinished: (File?) -> Unit) {
        viewModelScope.launch {
            val file = tripLogger.stopRecording()
            onFinished(file)
        }
    }

    fun scanDtcFaults() {
        viewModelScope.launch(Dispatchers.IO) {
            val proto = elmProtocol ?: return@launch
            _isScanningDtc.value = true
            try {
                // Pause live telemetry briefly during diagnostic query
                pollingJob?.cancel()
                val codes = proto.readDtcs()
                _dtcFaults.value = codes.map { DtcDatabase.getFaultInfo(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanningDtc.value = false
                startLivePolling()
            }
        }
    }

    fun clearDtcFaults(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val proto = elmProtocol ?: return@launch
            _isClearingDtc.value = true
            var success = false
            try {
                pollingJob?.cancel()
                success = proto.clearDtcs()
                if (success) {
                    _dtcFaults.value = emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isClearingDtc.value = false
                startLivePolling()
                withContext(Dispatchers.Main) {
                    onComplete(success)
                }
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        viewModelScope.launch {
            activeTransport?.disconnect()
            activeTransport = null
            elmProtocol = null
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            _connectedDeviceName.value = "Not Connected"
        }
    }

    fun getPairedClassicDevices(): List<BluetoothDeviceInfo> {
        val devices = mutableListOf<BluetoothDeviceInfo>()
        try {
            bluetoothAdapter?.bondedDevices?.forEach { dev ->
                devices.add(
                    BluetoothDeviceInfo(
                        name = dev.name ?: "Unknown ELM327",
                        address = dev.address,
                        isBle = false
                    )
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
        return devices
    }
}

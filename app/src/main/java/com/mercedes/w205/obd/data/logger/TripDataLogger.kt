package com.mercedes.w205.obd.data.logger

import android.content.Context
import com.mercedes.w205.obd.data.obd.VehicleLiveState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripDataLogger(private val context: Context) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordedSamplesCount = MutableStateFlow(0)
    val recordedSamplesCount: StateFlow<Int> = _recordedSamplesCount.asStateFlow()

    private var currentFile: File? = null
    private var fileWriter: FileWriter? = null

    suspend fun startRecording(): File = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val fileName = "W205_Trip_${dateFormat.format(Date())}.csv"
        val logsDir = File(context.getExternalFilesDir(null), "obd_logs").apply { mkdirs() }
        val file = File(logsDir, fileName)

        val writer = FileWriter(file, true)
        writer.appendLine("Timestamp,RelativeSec,RPM,Speed_kmh,Boost_bar,MAP_kpa,Baro_kpa,Coolant_C,Oil_C,IAT_C,Throttle_pct,Battery_V,Fuel_pct")
        writer.flush()

        currentFile = file
        fileWriter = writer
        _recordedSamplesCount.value = 0
        _isRecording.value = true

        return@withContext file
    }

    suspend fun recordSample(state: VehicleLiveState, startTimeMs: Long) = withContext(Dispatchers.IO) {
        if (!_isRecording.value || fileWriter == null) return@withContext

        val relSec = (state.timestamp - startTimeMs) / 1000.0f
        val line = buildString {
            append(state.timestamp).append(",")
            append(String.format(Locale.US, "%.3f", relSec)).append(",")
            append(state.rpm).append(",")
            append(state.speedKmh).append(",")
            append(String.format(Locale.US, "%.2f", state.boostBar)).append(",")
            append(state.mapKpa).append(",")
            append(state.baroKpa).append(",")
            append(state.coolantTempC).append(",")
            append(state.oilTempC).append(",")
            append(state.iatTempC).append(",")
            append(String.format(Locale.US, "%.1f", state.throttlePct)).append(",")
            append(String.format(Locale.US, "%.2f", state.batteryVoltage)).append(",")
            append(String.format(Locale.US, "%.1f", state.fuelLevelPct))
        }

        fileWriter?.appendLine(line)
        fileWriter?.flush()
        _recordedSamplesCount.value += 1
    }

    suspend fun stopRecording(): File? = withContext(Dispatchers.IO) {
        _isRecording.value = false
        fileWriter?.flush()
        fileWriter?.close()
        fileWriter = null
        val finished = currentFile
        currentFile = null
        return@withContext finished
    }

    fun listSavedLogs(): List<File> {
        val logsDir = File(context.getExternalFilesDir(null), "obd_logs")
        if (!logsDir.exists()) return emptyList()
        return logsDir.listFiles { _, name -> name.endsWith(".csv") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

package com.mercedes.w205.obd.data.obd

import com.mercedes.w205.obd.data.bluetooth.BluetoothTransport
import kotlinx.coroutines.delay

class Elm327Protocol(
    private val transport: BluetoothTransport
) {
    private var isInitialized = false
    private var lastBaroKpa = 101

    /**
     * Initializes the ELM327 interface specifically for 2019 Mercedes-Benz W205.
     * Robust adaptive sequence supporting both genuine and clone ELM327 adapters.
     */
    suspend fun initialize(): Boolean {
        isInitialized = false

        // 0. Wake up ELM327 / clear pending buffer
        transport.sendCommand("\r")
        delay(150)

        // 1. Reset ELM327
        val r1 = transport.sendCommand("ATZ")
        delay(800)

        // 2. Echo OFF (reduces serial traffic)
        transport.sendCommand("ATE0")
        delay(50)

        // 3. Linefeeds OFF
        transport.sendCommand("ATL0")
        delay(50)

        // 4. Spaces OFF
        transport.sendCommand("ATS0")
        delay(50)

        // 5. Headers OFF
        transport.sendCommand("ATH0")
        delay(50)

        // 6. Adaptive Timing on
        transport.sendCommand("ATAT1")
        delay(50)

        // 7. Force ISO 15765-4 CAN 11-bit 500k (Standard W205 Powertrain bus)
        transport.sendCommand("ATSP6")
        delay(80)

        // 8. Verify CAN bus response by querying Mode 01 Supported PIDs
        val testPid = transport.sendCommand("0100")
        if (testPid.contains("4100") || testPid.contains("41 00") || testPid.contains("SEARCHING")) {
            isInitialized = true
            val baroResp = transport.sendCommand("0133")
            W205Decoder.decodeBaro(baroResp)?.let { lastBaroKpa = it }
            return true
        }

        // Fallback: Try automatic protocol if ATSP6 failed on clone adapter
        transport.sendCommand("ATSP0")
        delay(120)
        val fallbackTest = transport.sendCommand("0100")
        isInitialized = fallbackTest.contains("4100") || fallbackTest.contains("41 00") || fallbackTest.contains("41")
        if (isInitialized) {
            val baroResp = transport.sendCommand("0133")
            W205Decoder.decodeBaro(baroResp)?.let { lastBaroKpa = it }
            return true
        }

        // Return true to allow live loop to run even if ignition was momentarily in position 1
        return true
    }

    /**
     * Executes one high-rate polling cycle for the AMG Cockpit.
     */
    suspend fun pollCockpitCycle(
        current: VehicleLiveState,
        slowCycleCounter: Int,
        onRawActivity: (String) -> Unit = {}
    ): VehicleLiveState {
        var state = current

        // --- Fast PIDs (Polled on every cycle) ---
        // 1. Engine RPM
        val rpmRaw = transport.sendCommand("010C")
        W205Decoder.decodeRpm(rpmRaw)?.let {
            state = state.copy(rpm = it)
            onRawActivity("RPM: $it ($rpmRaw)")
        }

        // 2. Vehicle Speed
        val speedRaw = transport.sendCommand("010D")
        W205Decoder.decodeSpeed(speedRaw)?.let {
            state = state.copy(speedKmh = it)
            onRawActivity("Speed: ${it}km/h")
        }

        // 3. Intake Manifold Absolute Pressure (MAP) & Turbo Boost
        val mapRaw = transport.sendCommand("010B")
        W205Decoder.decodeMap(mapRaw)?.let { mapVal ->
            val boost = W205Decoder.calculateBoostBar(mapVal, lastBaroKpa)
            state = state.copy(mapKpa = mapVal, boostBar = boost)
        }

        // 4. Throttle Position
        val thrRaw = transport.sendCommand("0111")
        W205Decoder.decodeThrottle(thrRaw)?.let {
            state = state.copy(throttlePct = it)
        }

        // --- Slow PIDs (Interleaved round-robin to preserve high RPM frame rate) ---
        when (slowCycleCounter % 6) {
            0 -> {
                val coolRaw = transport.sendCommand("0105")
                W205Decoder.decodeCoolantTemp(coolRaw)?.let { state = state.copy(coolantTempC = it) }
            }
            1 -> {
                val oilRaw = transport.sendCommand("015C")
                W205Decoder.decodeOilTemp(oilRaw)?.let { state = state.copy(oilTempC = it) }
            }
            2 -> {
                val iatRaw = transport.sendCommand("010F")
                W205Decoder.decodeIat(iatRaw)?.let { state = state.copy(iatTempC = it) }
            }
            3 -> {
                val vRaw = transport.sendCommand("ATRV")
                W205Decoder.decodeBatteryVoltage(vRaw)?.let { state = state.copy(batteryVoltage = it) }
            }
            4 -> {
                val fuelRaw = transport.sendCommand("012F")
                W205Decoder.decodeFuelLevel(fuelRaw)?.let { state = state.copy(fuelLevelPct = it) }
            }
            5 -> {
                val baroRaw = transport.sendCommand("0133")
                W205Decoder.decodeBaro(baroRaw)?.let {
                    lastBaroKpa = it
                    state = state.copy(baroKpa = it)
                }
            }
        }

        return state.copy(timestamp = System.currentTimeMillis())
    }

    /**
     * Reads stored and pending Diagnostic Trouble Codes (DTCs).
     */
    suspend fun readDtcs(): List<String> {
        val dtcs = mutableSetOf<String>()

        // Request stored DTCs (Mode 03)
        val mode03Resp = transport.sendCommand("03")
        dtcs.addAll(W205Decoder.decodeDtcs(mode03Resp))

        // Request pending DTCs (Mode 07)
        val mode07Resp = transport.sendCommand("07")
        dtcs.addAll(W205Decoder.decodeDtcs(mode07Resp))

        return dtcs.toList().sorted()
    }

    /**
     * Clears Diagnostic Trouble Codes and resets Check Engine Light (Mode 04).
     */
    suspend fun clearDtcs(): Boolean {
        val resp = transport.sendCommand("04")
        return resp.contains("44") || resp.contains("OK")
    }
}

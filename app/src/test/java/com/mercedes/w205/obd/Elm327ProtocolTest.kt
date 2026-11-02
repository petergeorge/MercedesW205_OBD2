package com.mercedes.w205.obd

import com.mercedes.w205.obd.data.bluetooth.BluetoothDeviceInfo
import com.mercedes.w205.obd.data.bluetooth.BluetoothTransport
import com.mercedes.w205.obd.data.bluetooth.ConnectionStatus
import com.mercedes.w205.obd.data.obd.Elm327Protocol
import com.mercedes.w205.obd.data.obd.VehicleLiveState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying that Elm327Protocol correctly initializes communication,
 * parses raw OBD-II PID streams, and updates VehicleLiveState in real time.
 */
class Elm327ProtocolTest {

    private class MockBluetoothTransport : BluetoothTransport {
        private val _status = MutableStateFlow(ConnectionStatus.CONNECTED)
        override val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

        val sentCommands = mutableListOf<String>()
        val commandResponses = mutableMapOf<String, String>()

        override suspend fun connect(device: BluetoothDeviceInfo): Boolean = true
        override suspend fun disconnect() { _status.value = ConnectionStatus.DISCONNECTED }
        override fun isConnected(): Boolean = _status.value == ConnectionStatus.CONNECTED

        override suspend fun sendCommand(cmd: String): String {
            sentCommands.add(cmd.trim())
            return commandResponses[cmd.trim()] ?: "OK>"
        }
    }

    @Test
    fun testInitializationSequence() = runBlocking {
        val mock = MockBluetoothTransport()
        mock.commandResponses["ATZ"] = "ELM327 v1.5>"
        mock.commandResponses["0100"] = "41 00 BE 3E B8 11>"
        mock.commandResponses["0133"] = "41 33 64>" // 100 kPa Baro

        val protocol = Elm327Protocol(mock)
        val initialized = protocol.initialize()

        assertTrue("ELM327 protocol must initialize successfully", initialized)
        assertTrue("Must send ATZ reset", mock.sentCommands.contains("ATZ"))
        assertTrue("Must send ATE0 echo off", mock.sentCommands.contains("ATE0"))
        assertTrue("Must send ATSP6 for CAN 11bit 500k", mock.sentCommands.contains("ATSP6"))
        assertTrue("Must query 0100 for CAN bus verification", mock.sentCommands.contains("0100"))
        assertTrue("Must query 0133 for initial barometric reference", mock.sentCommands.contains("0133"))
    }

    @Test
    fun testLiveCycleTelemetryUpdates() = runBlocking {
        val mock = MockBluetoothTransport()
        // Fast PIDs
        mock.commandResponses["010C"] = "41 0C 1F 40>" // 2000 RPM
        mock.commandResponses["010D"] = "41 0D 64>"     // 100 km/h
        mock.commandResponses["010B"] = "41 0B B4>"     // 180 kPa MAP (0.79 bar boost with 101 kPa baro)
        mock.commandResponses["0111"] = "41 11 66>"     // 40% Throttle

        // Slow PIDs
        mock.commandResponses["0105"] = "41 05 7D>"     // 85 °C Coolant
        mock.commandResponses["015C"] = "41 5C 82>"     // 90 °C Oil
        mock.commandResponses["010F"] = "41 0F 41>"     // 25 °C IAT
        mock.commandResponses["ATRV"] = "14.2V>"        // 14.2 V Battery
        mock.commandResponses["012F"] = "41 2F 99>"     // 60% Fuel
        mock.commandResponses["0133"] = "41 33 65>"     // 101 kPa Baro

        val protocol = Elm327Protocol(mock)
        var state = VehicleLiveState()
        val activityEvents = mutableListOf<String>()

        // Cycle 0: Fast PIDs + Coolant
        state = protocol.pollCockpitCycle(state, slowCycleCounter = 0) { act ->
            activityEvents.add(act)
        }
        assertEquals(2000, state.rpm)
        assertEquals(100, state.speedKmh)
        assertEquals(180, state.mapKpa)
        assertEquals(0.79f, state.boostBar, 0.02f)
        assertEquals(40.0f, state.throttlePct, 0.5f)
        assertEquals(85, state.coolantTempC)

        // Cycle 1: Fast PIDs + Oil Temp
        mock.commandResponses["010C"] = "41 0C 2E E0>" // RPM increases to 3000
        state = protocol.pollCockpitCycle(state, slowCycleCounter = 1) { act ->
            activityEvents.add(act)
        }
        assertEquals(3000, state.rpm)
        assertEquals(90, state.oilTempC)

        // Cycle 3: Fast PIDs + Battery Voltage
        state = protocol.pollCockpitCycle(state, slowCycleCounter = 3) { act ->
            activityEvents.add(act)
        }
        assertEquals(14.2f, state.batteryVoltage, 0.05f)

        // Verify activity pulse strings were emitted
        assertTrue(activityEvents.isNotEmpty())
        assertTrue(activityEvents.any { it.contains("RPM: 2000") })
        assertTrue(activityEvents.any { it.contains("RPM: 3000") })
    }

    @Test
    fun testLiveCycleWithEngineIdle() = runBlocking {
        val mock = MockBluetoothTransport()
        // Engine at idle (750 RPM, 0 km/h, 12% throttle)
        mock.commandResponses["010C"] = "41 0C 0B B8>" // (11*256 + 184)/4 = 750 RPM
        mock.commandResponses["010D"] = "41 0D 00>"     // 0 km/h
        mock.commandResponses["010B"] = "41 0B 28>"     // 40 kPa MAP (vacuum: -0.61 bar)
        mock.commandResponses["0111"] = "41 11 1E>"     // 11.7% Throttle

        val protocol = Elm327Protocol(mock)
        var state = VehicleLiveState()
        state = protocol.pollCockpitCycle(state, slowCycleCounter = 0)

        assertEquals(750, state.rpm)
        assertEquals(0, state.speedKmh)
        assertEquals(40, state.mapKpa)
        assertEquals(-0.61f, state.boostBar, 0.02f)
        assertEquals(11.76f, state.throttlePct, 0.1f)
    }
}

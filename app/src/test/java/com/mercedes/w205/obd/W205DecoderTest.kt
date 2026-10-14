package com.mercedes.w205.obd

import com.mercedes.w205.obd.data.obd.W205Decoder
import org.junit.Assert.*
import org.junit.Test

class W205DecoderTest {

    @Test
    fun testRpmDecoding() {
        // 41 0C 1F 40 -> A = 0x1F (31), B = 0x40 (64)
        // ((31 * 256) + 64) / 4 = 8000 / 4 = 2000 RPM
        val raw = "41 0C 1F 40"
        val rpm = W205Decoder.decodeRpm(raw)
        assertEquals(2000, rpm)
    }

    @Test
    fun testSpeedDecoding() {
        // 41 0D 64 -> A = 0x64 = 100 km/h
        val raw = "41 0D 64"
        val speed = W205Decoder.decodeSpeed(raw)
        assertEquals(100, speed)
    }

    @Test
    fun testBoostCalculation() {
        // MAP = 180 kPa, Baro = 100 kPa -> Boost = (180 - 100) * 0.01 = 0.80 bar
        val boost = W205Decoder.calculateBoostBar(mapKpa = 180, baroKpa = 100)
        assertEquals(0.80f, boost, 0.01f)
    }

    @Test
    fun testCoolantAndOilTemp() {
        // 41 05 7D -> A = 0x7D (125) -> Coolant = 125 - 40 = 85 °C
        assertEquals(85, W205Decoder.decodeCoolantTemp("41 05 7D"))

        // 41 5C 82 -> A = 0x82 (130) -> Oil = 130 - 40 = 90 °C
        assertEquals(90, W205Decoder.decodeOilTemp("41 5C 82"))
    }

    @Test
    fun testBatteryVoltage() {
        // ATRV response format
        assertEquals(14.2f, W205Decoder.decodeBatteryVoltage("14.2V") ?: 0f, 0.05f)
        assertEquals(12.6f, W205Decoder.decodeBatteryVoltage("12.6V") ?: 0f, 0.05f)

        // PID 0142 format: 41 42 35 E8 -> (0x35E8 = 13800) / 1000 = 13.8V
        assertEquals(13.8f, W205Decoder.decodeBatteryVoltage("41 42 35 E8") ?: 0f, 0.05f)
    }

    @Test
    fun testDtcDecoding() {
        // Mode 03 response: 43 02 01 28 02 99 (P0128 and P0299)
        val raw = "43 02 01 28 02 99"
        val dtcs = W205Decoder.decodeDtcs(raw)
        assertEquals(2, dtcs.size)
        assertEquals("P0128", dtcs[0])
        assertEquals("P0299", dtcs[1])
    }
}

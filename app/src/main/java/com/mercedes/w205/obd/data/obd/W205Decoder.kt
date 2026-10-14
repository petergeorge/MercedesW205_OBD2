package com.mercedes.w205.obd.data.obd

data class VehicleLiveState(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val boostBar: Float = 0.0f,
    val mapKpa: Int = 101,
    val baroKpa: Int = 101,
    val coolantTempC: Int = 85,
    val oilTempC: Int = 90,
    val iatTempC: Int = 25,
    val throttlePct: Float = 0.0f,
    val batteryVoltage: Float = 14.1f,
    val fuelLevelPct: Float = 60.0f,
    val timestamp: Long = System.currentTimeMillis()
)

object W205Decoder {

    /**
     * Cleans an ELM327 hex response by removing spaces, headers, and newlines.
     */
    fun cleanHex(raw: String): String {
        return raw.replace(" ", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace(">", "")
            .trim()
            .uppercase()
    }

    /**
     * Engine RPM (PID 010C) -> Formula: ((A * 256) + B) / 4
     * Response: 41 0C A B
     */
    fun decodeRpm(raw: String): Int? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("410C")
        if (idx != -1 && clean.length >= idx + 8) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 4
        }
        return null
    }

    /**
     * Vehicle Speed (PID 010D) -> Formula: A km/h
     * Response: 41 0D A
     */
    fun decodeSpeed(raw: String): Int? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("410D")
        if (idx != -1 && clean.length >= idx + 6) {
            return clean.substring(idx + 4, idx + 6).toIntOrNull(16)
        }
        return null
    }

    /**
     * Intake Manifold Absolute Pressure (PID 010B) -> Formula: A kPa
     * Response: 41 0B A
     */
    fun decodeMap(raw: String): Int? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("410B")
        if (idx != -1 && clean.length >= idx + 6) {
            return clean.substring(idx + 4, idx + 6).toIntOrNull(16)
        }
        return null
    }

    /**
     * Barometric Pressure (PID 0133) -> Formula: A kPa
     * Response: 41 33 A
     */
    fun decodeBaro(raw: String): Int? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("4133")
        if (idx != -1 && clean.length >= idx + 6) {
            return clean.substring(idx + 4, idx + 6).toIntOrNull(16)
        }
        return null
    }

    /**
     * Turbo Boost Pressure calculation for Mercedes C180 (W205 Turbo Petrol)
     * Output in BAR: (MAP - Baro) * 0.01 bar
     */
    fun calculateBoostBar(mapKpa: Int, baroKpa: Int): Float {
        val diffKpa = mapKpa - baroKpa
        return (diffKpa / 100.0f).coerceIn(-1.0f, 2.5f)
    }

    /**
     * Engine Coolant Temperature (PID 0105) -> Formula: A - 40 (°C)
     * Response: 41 05 A
     */
    fun decodeCoolantTemp(raw: String): Int? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("4105")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return a - 40
        }
        return null
    }

    /**
     * Engine Oil Temperature (PID 015C) -> Formula: A - 40 (°C)
     * Standard on Euro 6 Mercedes W205 Petrol
     * Response: 41 5C A
     */
    fun decodeOilTemp(raw: String): Int? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("415C")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return a - 40
        }
        return null
    }

    /**
     * Intake Air Temperature (PID 010F) -> Formula: A - 40 (°C)
     * Response: 41 0F A
     */
    fun decodeIat(raw: String): Int? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("410F")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return a - 40
        }
        return null
    }

    /**
     * Throttle Position (PID 0111) -> Formula: (A * 100) / 255 (%)
     * Response: 41 11 A
     */
    fun decodeThrottle(raw: String): Float? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("4111")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a * 100.0f) / 255.0f
        }
        return null
    }

    /**
     * Battery Voltage -> Either from ELM327 "ATRV" command (e.g. "14.2V")
     * or Control Module Voltage PID 0142 -> ((A * 256) + B) / 1000.0 V
     */
    fun decodeBatteryVoltage(raw: String): Float? {
        // Check ATRV format first (e.g. "12.6V" or "14.2V")
        val vMatch = Regex("([0-9]{1,2}\\.[0-9]+)").find(raw)
        if (vMatch != null && !raw.contains("41")) {
            return vMatch.groupValues[1].toFloatOrNull()
        }

        // Check PID 0142
        val clean = cleanHex(raw)
        val idx = clean.indexOf("4142")
        if (idx != -1 && clean.length >= idx + 8) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            val b = clean.substring(idx + 6, idx + 8).toIntOrNull(16) ?: return null
            return ((a * 256) + b) / 1000.0f
        }
        return null
    }

    /**
     * Fuel Tank Level (PID 012F) -> Formula: (A * 100) / 255 (%)
     */
    fun decodeFuelLevel(raw: String): Float? {
        val clean = cleanHex(raw)
        val idx = clean.indexOf("412F")
        if (idx != -1 && clean.length >= idx + 6) {
            val a = clean.substring(idx + 4, idx + 6).toIntOrNull(16) ?: return null
            return (a * 100.0f) / 255.0f
        }
        return null
    }

    /**
     * Decodes Mode 03 / Mode 07 DTC diagnostic responses into standard fault codes.
     * Response: 43 02 01 28 02 99 ... (P0128, P0299)
     */
    fun decodeDtcs(raw: String): List<String> {
        val clean = cleanHex(raw)
        val dtcs = mutableListOf<String>()

        // Look for Mode 03 response prefix "43" or Mode 07 prefix "47"
        val prefixIdx = clean.indexOf("43").takeIf { it != -1 } ?: clean.indexOf("47")
        if (prefixIdx == -1) return emptyList()

        // Skip Mode byte (2 chars) and DTC count byte (2 chars, e.g. "4302" -> skip 4 chars)
        val payload = if (clean.length >= prefixIdx + 4) clean.substring(prefixIdx + 4) else clean.substring(prefixIdx + 2)
        var offset = 0
        while (offset + 4 <= payload.length) {
            val chunk = payload.substring(offset, offset + 4)
            if (chunk == "0000" || chunk == "NODATA") break

            val firstByte = chunk.substring(0, 2).toIntOrNull(16) ?: break
            val secondByte = chunk.substring(2, 4)

            val category = when ((firstByte and 0xC0) shr 6) {
                0 -> "P" // Powertrain
                1 -> "C" // Chassis
                2 -> "B" // Body
                3 -> "U" // Network
                else -> "P"
            }
            val digit1 = (firstByte and 0x30) shr 4
            val digit2 = (firstByte and 0x0F)
            val dtcCode = "$category$digit1$digit2$secondByte"
            dtcs.add(dtcCode)

            offset += 4
        }
        return dtcs
    }
}

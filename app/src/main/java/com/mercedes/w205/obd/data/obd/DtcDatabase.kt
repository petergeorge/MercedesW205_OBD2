package com.mercedes.w205.obd.data.obd

data class DtcFaultRecord(
    val code: String,
    val description: String,
    val subsystem: String,
    val severity: DtcSeverity,
    val mercedesNote: String
)

enum class DtcSeverity {
    CRITICAL,
    WARNING,
    INFO
}

object DtcDatabase {

    private val MERCEDES_DTC_MAP = mapOf(
        "P0299" to DtcFaultRecord(
            code = "P0299",
            description = "Turbocharger / Supercharger Underboost",
            subsystem = "Turbo / Forced Induction",
            severity = DtcSeverity.WARNING,
            mercedesNote = "Common on M274/M264: Inspect boost pressure control valve, vacuum hoses, or wastegate actuator linkage."
        ),
        "P052E" to DtcFaultRecord(
            code = "P052E",
            description = "Positive Crankcase Ventilation (PCV) Regulator Valve Performance",
            subsystem = "Engine Crankcase Ventilation",
            severity = DtcSeverity.WARNING,
            mercedesNote = "Extremely common W205 issue: Crankcase ventilation line / oil separator heater assembly diaphragm failure."
        ),
        "P0128" to DtcFaultRecord(
            code = "P0128",
            description = "Coolant Thermostat (Coolant Temp Below Thermostat Regulating Temp)",
            subsystem = "Cooling System",
            severity = DtcSeverity.WARNING,
            mercedesNote = "W205 thermostat stuck open. Engine takes too long to reach 85°C. Replace thermostat housing."
        ),
        "P0171" to DtcFaultRecord(
            code = "P0171",
            description = "System Too Lean (Bank 1)",
            subsystem = "Fuel & Air Metering",
            severity = DtcSeverity.WARNING,
            mercedesNote = "Check for intake vacuum leaks, dirty Mass Airflow Sensor (MAF), or weak low-pressure fuel pump."
        ),
        "P0172" to DtcFaultRecord(
            code = "P0172",
            description = "System Too Rich (Bank 1)",
            subsystem = "Fuel & Air Metering",
            severity = DtcSeverity.WARNING,
            mercedesNote = "Leaking high-pressure fuel injector or contaminated engine oil with fuel dilution."
        ),
        "P0300" to DtcFaultRecord(
            code = "P0300",
            description = "Random / Multiple Cylinder Misfire Detected",
            subsystem = "Ignition System",
            severity = DtcSeverity.CRITICAL,
            mercedesNote = "Inspect spark plugs, Delphi/Bosch direct ignition coils, or carbon buildup on intake valves."
        ),
        "P0301" to DtcFaultRecord(
            code = "P0301",
            description = "Cylinder 1 Misfire Detected",
            subsystem = "Ignition System",
            severity = DtcSeverity.CRITICAL,
            mercedesNote = "Inspect Cylinder 1 spark plug, ignition coil, and fuel injector."
        ),
        "P0302" to DtcFaultRecord(
            code = "P0302",
            description = "Cylinder 2 Misfire Detected",
            subsystem = "Ignition System",
            severity = DtcSeverity.CRITICAL,
            mercedesNote = "Inspect Cylinder 2 spark plug and ignition coil."
        ),
        "P0303" to DtcFaultRecord(
            code = "P0303",
            description = "Cylinder 3 Misfire Detected",
            subsystem = "Ignition System",
            severity = DtcSeverity.CRITICAL,
            mercedesNote = "Inspect Cylinder 3 spark plug and ignition coil."
        ),
        "P0304" to DtcFaultRecord(
            code = "P0304",
            description = "Cylinder 4 Misfire Detected",
            subsystem = "Ignition System",
            severity = DtcSeverity.CRITICAL,
            mercedesNote = "Inspect Cylinder 4 spark plug and ignition coil."
        ),
        "P0016" to DtcFaultRecord(
            code = "P0016",
            description = "Crankshaft Position - Camshaft Position Correlation (Bank 1 Sensor A)",
            subsystem = "Timing / Camshaft",
            severity = DtcSeverity.CRITICAL,
            mercedesNote = "Camshaft adjuster gear or timing chain stretch on M274/M264. Check camshaft pulse wheels."
        ),
        "P0017" to DtcFaultRecord(
            code = "P0017",
            description = "Crankshaft Position - Camshaft Position Correlation (Bank 1 Sensor B)",
            subsystem = "Timing / Camshaft",
            severity = DtcSeverity.CRITICAL,
            mercedesNote = "Exhaust camshaft position sensor correlation error or camshaft adjuster failure."
        ),
        "P0420" to DtcFaultRecord(
            code = "P0420",
            description = "Catalyst System Efficiency Below Threshold (Bank 1)",
            subsystem = "Exhaust / Emissions",
            severity = DtcSeverity.WARNING,
            mercedesNote = "Downstream O2 oxygen sensor degradation or catalytic converter restriction."
        ),
        "P2610" to DtcFaultRecord(
            code = "P2610",
            description = "ECM/PCM Engine Off Timer Performance",
            subsystem = "Engine Control Module",
            severity = DtcSeverity.INFO,
            mercedesNote = "Main engine ECU internal sleep timer drift. Usually cleared by software update or battery reset."
        )
    )

    fun getFaultInfo(dtcCode: String): DtcFaultRecord {
        val upper = dtcCode.uppercase().trim()
        return MERCEDES_DTC_MAP[upper] ?: DtcFaultRecord(
            code = upper,
            description = "Standard Diagnostic Trouble Code",
            subsystem = when {
                upper.startsWith("P") -> "Powertrain"
                upper.startsWith("C") -> "Chassis"
                upper.startsWith("B") -> "Body"
                upper.startsWith("U") -> "Network / Communication"
                else -> "Generic Subsystem"
            },
            severity = DtcSeverity.WARNING,
            mercedesNote = "Refer to Mercedes-Benz Xentry Diagnostics / WIS documentation for detailed guided test steps."
        )
    }
}

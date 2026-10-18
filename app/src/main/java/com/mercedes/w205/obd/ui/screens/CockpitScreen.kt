package com.mercedes.w205.obd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercedes.w205.obd.data.bluetooth.ConnectionStatus
import com.mercedes.w205.obd.data.obd.VehicleLiveState
import com.mercedes.w205.obd.ui.components.*
import com.mercedes.w205.obd.ui.theme.*
import java.util.Locale

@Composable
fun CockpitScreen(
    state: VehicleLiveState,
    connectionStatus: ConnectionStatus,
    deviceName: String?,
    liveActivityMsg: String = "",
    livePulse: Boolean = false,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmgBlack)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header Status Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AmgCardBg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MERCEDES C180 (W205)",
                    color = AmgSilver,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> "ONLINE: ${deviceName ?: "ELM327"}"
                        ConnectionStatus.CONNECTING -> "CONNECTING..."
                        ConnectionStatus.INITIALIZING_ELM -> "INIT ELM CAN 500K..."
                        ConnectionStatus.ERROR -> "CONNECTION FAILED"
                        ConnectionStatus.DISCONNECTED -> "OFFLINE"
                    },
                    color = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> AmgGreen
                        ConnectionStatus.CONNECTING, ConnectionStatus.INITIALIZING_ELM -> AmgAmber
                        ConnectionStatus.ERROR -> AmgRed
                        ConnectionStatus.DISCONNECTED -> AmgSilverMuted
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (connectionStatus == ConnectionStatus.CONNECTED) {
                Button(
                    onClick = onDisconnectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C38)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("DISCONNECT", color = AmgSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AmgRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("CONNECT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (connectionStatus == ConnectionStatus.CONNECTED) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF131722))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (livePulse) "🟢" else "⚪",
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = liveActivityMsg.ifEmpty { "Polling live PIDs (010C RPM, 010D Speed, 010B Boost)..." },
                    color = AmgSilverMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Main AMG Circular Tachometer & Speedometer ---
        CircularTachometer(
            rpm = state.rpm,
            speedKmh = state.speedKmh
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- Turbo Boost Gauge ---
        BoostBarGauge(boostBar = state.boostBar)

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4x Telemetry Sensor Cards Grid ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactTempDial(
                label = "Coolant",
                value = "${state.coolantTempC}",
                unit = "°C",
                isWarning = state.coolantTempC >= 105,
                modifier = Modifier.weight(1f)
            )
            CompactTempDial(
                label = "Engine Oil",
                value = "${state.oilTempC}",
                unit = "°C",
                isWarning = state.oilTempC >= 115,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactTempDial(
                label = "Intake Air",
                value = "${state.iatTempC}",
                unit = "°C",
                isWarning = state.iatTempC >= 60,
                modifier = Modifier.weight(1f)
            )
            CompactTempDial(
                label = "Battery",
                value = String.format(Locale.US, "%.1f", state.batteryVoltage),
                unit = "V",
                isWarning = state.batteryVoltage < 11.8f || state.batteryVoltage > 15.0f,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Throttle Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(AmgCardBg)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "THROTTLE",
                color = AmgSilverMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = (state.throttlePct / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AmgCyan,
                trackColor = Color(0xFF131722),
            )
            Text(
                text = "${state.throttlePct.toInt()}%",
                color = AmgSilver,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

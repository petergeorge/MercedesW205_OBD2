package com.mercedes.w205.obd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercedes.w205.obd.data.bluetooth.ConnectionStatus
import com.mercedes.w205.obd.data.obd.DtcFaultRecord
import com.mercedes.w205.obd.data.obd.DtcSeverity
import com.mercedes.w205.obd.ui.theme.*

@Composable
fun DtcScreen(
    dtcs: List<DtcFaultRecord>,
    isScanning: Boolean,
    isClearing: Boolean,
    connectionStatus: ConnectionStatus,
    onScanClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmgBlack)
            .padding(16.dp)
    ) {
        // --- Control Buttons ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onScanClick,
                enabled = connectionStatus == ConnectionStatus.CONNECTED && !isScanning && !isClearing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6091)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCANNING...")
                } else {
                    Text("🔍 SCAN FAULTS")
                }
            }

            Button(
                onClick = { showClearConfirmDialog = true },
                enabled = connectionStatus == ConnectionStatus.CONNECTED && dtcs.isNotEmpty() && !isScanning && !isClearing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AmgRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isClearing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CLEARING...")
                } else {
                    Text("🗑️ CLEAR CODES")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Status Header ---
        Text(
            text = if (dtcs.isEmpty()) "DIAGNOSTIC STATUS: CLEAN (0 FAULTS)" else "ACTIVE FAULTS (${dtcs.size} DETECTED)",
            color = if (dtcs.isEmpty()) AmgGreen else AmgRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (dtcs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmgCardBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Fault Codes Stored in ECU",
                        color = AmgSilver,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Powertrain CAN bus (ISO 15765-4) reported no active MIL or pending trouble codes.",
                        color = AmgSilverMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dtcs) { fault ->
                    DtcFaultCard(fault = fault)
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = AmgCardBg,
            title = {
                Text("Confirm Clearing Fault Codes", color = AmgSilver, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will send OBD Mode 04 to your Mercedes C180 ECU, resetting the Check Engine Light (MIL) and erasing stored freeze-frame records.\n\nEnsure ignition is ON and engine is stopped.",
                    color = AmgSilverMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmgRed)
                ) {
                    Text("YES, CLEAR CODES", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("CANCEL", color = AmgSilver)
                }
            }
        )
    }
}

@Composable
fun DtcFaultCard(fault: DtcFaultRecord) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmgCardBg)
            .border(1.dp, if (fault.severity == DtcSeverity.CRITICAL) AmgRed else AmgBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fault.code,
                color = if (fault.severity == DtcSeverity.CRITICAL) AmgRed else AmgAmber,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Surface(
                color = when (fault.severity) {
                    DtcSeverity.CRITICAL -> Color(0xFF4A1015)
                    DtcSeverity.WARNING -> Color(0xFF4A3410)
                    DtcSeverity.INFO -> Color(0xFF10284A)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = fault.subsystem.uppercase(),
                    color = when (fault.severity) {
                        DtcSeverity.CRITICAL -> AmgRed
                        DtcSeverity.WARNING -> AmgAmber
                        DtcSeverity.INFO -> AmgCyan
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = fault.description,
            color = AmgSilver,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mercedes-Benz Repair Guidance:",
            color = AmgSilverMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = fault.mercedesNote,
            color = Color(0xFF8FD8FF),
            fontSize = 12.sp
        )
    }
}

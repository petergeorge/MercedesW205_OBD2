package com.mercedes.w205.obd.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.mercedes.w205.obd.data.bluetooth.ConnectionStatus
import com.mercedes.w205.obd.ui.theme.*
import java.io.File

@Composable
fun LoggerScreen(
    isRecording: Boolean,
    sampleCount: Int,
    connectionStatus: ConnectionStatus,
    savedLogs: List<File>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onRefreshLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmgBlack)
            .padding(16.dp)
    ) {
        // --- Recording Action Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = AmgCardBg),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isRecording) "RECORDING TELEMETRY..." else "TRIP DATA LOGGER",
                            color = if (isRecording) AmgRed else AmgSilver,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRecording) "$sampleCount samples captured" else "Captures RPM, Speed, Boost, Oil/Coolant Temp to CSV",
                            color = AmgSilverMuted,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isRecording) onStopRecording() else onStartRecording()
                        },
                        enabled = connectionStatus == ConnectionStatus.CONNECTED || isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) AmgRed else AmgGreen
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isRecording) "⏹️ STOP" else "⏺️ START",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED TRIP SESSIONS (${savedLogs.size})",
                color = AmgSilver,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            TextButton(onClick = onRefreshLogs) {
                Text("REFRESH", color = AmgCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (savedLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmgCardBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved CSV sessions yet.\nStart recording during a drive to generate telemetry logs.",
                    color = AmgSilverMuted,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedLogs) { file ->
                    SavedLogFileCard(file = file, onShare = { shareCsvFile(context, file) })
                }
            }
        }
    }
}

@Composable
fun SavedLogFileCard(file: File, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmgCardBg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = file.name,
                color = AmgSilver,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${file.length() / 1024} KB",
                color = AmgSilverMuted,
                fontSize = 11.sp
            )
        }

        Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C38)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("📤 SHARE CSV", color = AmgCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun shareCsvFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export W205 Trip CSV"))
    } catch (e: Exception) {
        // Fallback for simple share
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Log file: ${file.absolutePath}")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export Log"))
    }
}

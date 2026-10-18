package com.mercedes.w205.obd

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mercedes.w205.obd.ui.screens.*
import com.mercedes.w205.obd.ui.theme.*
import com.mercedes.w205.obd.ui.viewmodel.ObdViewModel

enum class NavigationTab(val title: String, val icon: ImageVector) {
    COCKPIT("Cockpit", Icons.Filled.Speed),
    DIAGNOSTICS("DTC Scan", Icons.Filled.Build),
    LOGGER("Telemetry", Icons.Filled.Timeline)
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "Bluetooth permissions are required to connect to ELM327", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            MercedesW205Theme {
                val viewModel: ObdViewModel = viewModel()
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }
}

@Composable
fun MainAppContent(viewModel: ObdViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.COCKPIT) }
    var showScanDialog by remember { mutableStateOf(false) }

    val liveState by viewModel.vehicleLiveState.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val dtcs by viewModel.dtcFaults.collectAsState()
    val isScanningDtc by viewModel.isScanningDtc.collectAsState()
    val isClearingDtc by viewModel.isClearingDtc.collectAsState()
    val liveActivityMsg by viewModel.liveActivityMsg.collectAsState()
    val livePulse by viewModel.livePulse.collectAsState()
    val isRecording by viewModel.tripLogger.isRecording.collectAsState()
    val sampleCount by viewModel.tripLogger.recordedSamplesCount.collectAsState()

    var savedLogs by remember { mutableStateOf(viewModel.tripLogger.listSavedLogs()) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF10141D),
                tonalElevation = 8.dp
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmgRed,
                            selectedTextColor = AmgSilver,
                            unselectedIconColor = AmgSilverMuted,
                            unselectedTextColor = AmgSilverMuted,
                            indicatorColor = Color(0xFF1D2230)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AmgBlack)
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                NavigationTab.COCKPIT -> {
                    CockpitScreen(
                        state = liveState,
                        connectionStatus = connectionStatus,
                        deviceName = connectedDeviceName,
                        liveActivityMsg = liveActivityMsg,
                        livePulse = livePulse,
                        onConnectClick = { showScanDialog = true },
                        onDisconnectClick = { viewModel.disconnect() }
                    )
                }
                NavigationTab.DIAGNOSTICS -> {
                    DtcScreen(
                        dtcs = dtcs,
                        isScanning = isScanningDtc,
                        isClearing = isClearingDtc,
                        connectionStatus = connectionStatus,
                        onScanClick = { viewModel.scanDtcFaults() },
                        onClearClick = {
                            viewModel.clearDtcFaults { success ->
                                // Triggered after clear
                            }
                        }
                    )
                }
                NavigationTab.LOGGER -> {
                    LoggerScreen(
                        isRecording = isRecording,
                        sampleCount = sampleCount,
                        connectionStatus = connectionStatus,
                        savedLogs = savedLogs,
                        onStartRecording = { viewModel.startTripRecording() },
                        onStopRecording = {
                            viewModel.stopTripRecording {
                                savedLogs = viewModel.tripLogger.listSavedLogs()
                            }
                        },
                        onRefreshLogs = {
                            savedLogs = viewModel.tripLogger.listSavedLogs()
                        }
                    )
                }
            }
        }
    }

    if (showScanDialog) {
        val paired = viewModel.getPairedClassicDevices()
        DeviceScanDialog(
            devices = paired,
            onSelectDevice = { dev ->
                showScanDialog = false
                viewModel.connectDevice(dev)
            },
            onDismiss = { showScanDialog = false }
        )
    }
}

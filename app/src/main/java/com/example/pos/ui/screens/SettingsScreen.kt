package com.example.pos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pos.util.PrinterUtils
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.entity.Settings
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.pos.ui.utils.getResponsivePadding


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDao: SettingsDao
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by settingsDao.getSettings().collectAsState(initial = null)
    
    var stallName by remember { mutableStateOf(settings?.stallName ?: "") }
    var address by remember { mutableStateOf(settings?.address ?: "") }
    var phone by remember { mutableStateOf(settings?.phone ?: "") }
    var pin by remember { mutableStateOf(settings?.pin ?: "1111") }
    var printerMac by remember { mutableStateOf(settings?.printerMac ?: "") }
    var paddingTop by remember { mutableStateOf(settings?.paddingTop?.toString() ?: "2") }
    var paddingBottom by remember { mutableStateOf(settings?.paddingBottom?.toString() ?: "2") }
    var printerWidth by remember { mutableStateOf(settings?.printerWidth?.toString() ?: "42") }
    var linesBeforeCut by remember { mutableStateOf(settings?.linesBeforeCut?.toString() ?: "2") }
    
    // Bluetooth device picker
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var bluetoothDevices by remember { mutableStateOf<List<PrinterUtils.BluetoothDeviceInfo>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<PrinterUtils.BluetoothDeviceInfo?>(null) }
    var bluetoothPermissionGranted by remember { mutableStateOf(false) }
    var bluetoothError by remember { mutableStateOf<String?>(null) }

    val minPrinterWidth = 20
    val printerWidthValue = (printerWidth.toIntOrNull() ?: 0).coerceAtLeast(minPrinterWidth)

    val receiptPreview by remember(
        stallName,
        address,
        phone,
        paddingTop,
        paddingBottom,
        linesBeforeCut,
        printerWidthValue
    ) {
        derivedStateOf {
            PrinterUtils.getTestReceiptPreview(
                stallName = stallName,
                address = address,
                phone = phone,
                paddingTop = paddingTop.toIntOrNull() ?: 0,
                paddingBottom = paddingBottom.toIntOrNull() ?: 0,
                linesBeforeCut = linesBeforeCut.toIntOrNull() ?: 3,
                printerWidth = printerWidthValue
            )
        }
    }

    LaunchedEffect(settings) {
        val current = settings
        if (current == null) {
            paddingTop = "2"
            paddingBottom = "2"
            printerWidth = "42"
            linesBeforeCut = "2"
            return@LaunchedEffect
        }

        val useNewDefaults = current.paddingTop == 0 &&
            current.paddingBottom == 0 &&
            current.printerWidth == 32 &&
            current.linesBeforeCut == 3

        stallName = current.stallName
        address = current.address
        phone = current.phone
        pin = current.pin.ifBlank { "1111" }
        printerMac = current.printerMac ?: ""
        paddingTop = if (useNewDefaults) "2" else current.paddingTop.toString()
        paddingBottom = if (useNewDefaults) "2" else current.paddingBottom.toString()
        printerWidth = if (useNewDefaults) "42" else current.printerWidth.toString()
        linesBeforeCut = if (useNewDefaults) "2" else current.linesBeforeCut.toString()
    }

    // Permission launcher for Bluetooth
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val bluetoothConnectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val bluetoothScanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        
        bluetoothPermissionGranted = bluetoothConnectGranted && bluetoothScanGranted
        
        if (bluetoothPermissionGranted) {
            bluetoothDevices = PrinterUtils.getPairedBluetoothDevices(context)
            bluetoothError = null
        } else {
            bluetoothError = "Bluetooth permissions are required to scan for devices"
        }
    }

    // Check permissions on startup
    LaunchedEffect(Unit) {
        val hasBluetoothConnect = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        val hasBluetoothScan = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
        
        bluetoothPermissionGranted = hasBluetoothConnect && hasBluetoothScan
        
        if (bluetoothPermissionGranted) {
            bluetoothDevices = PrinterUtils.getPairedBluetoothDevices(context)
        }
    }

    // Load Bluetooth devices when picker is shown
    LaunchedEffect(showBluetoothPicker) {
        if (showBluetoothPicker && bluetoothPermissionGranted) {
            bluetoothDevices = PrinterUtils.getPairedBluetoothDevices(context)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(getResponsivePadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        item {
            Text("Stall Details", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = stallName,
                onValueChange = { stallName = it },
                label = { Text("Stall Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }

        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Printer", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = printerMac,
                onValueChange = { printerMac = it },
                label = { Text("Printer MAC") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = paddingTop,
                    onValueChange = { paddingTop = it },
                    label = { Text("Top Padding") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = paddingBottom,
                    onValueChange = { paddingBottom = it },
                    label = { Text("Bottom Padding") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = printerWidth,
                onValueChange = { printerWidth = it },
                label = { Text("Printer Width") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if ((printerWidth.toIntOrNull() ?: 0) in 1 until minPrinterWidth) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Width below 20 is clipped to 20",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = linesBeforeCut,
                onValueChange = { linesBeforeCut = it },
                label = { Text("Lines Before Cut") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (bluetoothPermissionGranted) {
                            showBluetoothPicker = true
                        } else {
                            bluetoothPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                )
                            )
                        }
                    }
                ) {
                    Text("Scan Printers")
                }
                if (selectedDevice != null) {
                    Text(
                        text = "${selectedDevice!!.name} (${selectedDevice!!.address})",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (printerMac.isNotBlank()) {
                    Text(
                        text = "Current: $printerMac",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!bluetoothPermissionGranted) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Bluetooth permissions required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (bluetoothError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Error: $bluetoothError",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        settingsDao.insertOrUpdateSettings(
                            Settings(
                                stallName = stallName,
                                address = address,
                                phone = phone,
                                pin = pin,
                                printerMac = printerMac,
                                paddingTop = paddingTop.toIntOrNull() ?: 0,
                                paddingBottom = paddingBottom.toIntOrNull() ?: 0,
                                printerWidth = printerWidthValue,
                                linesBeforeCut = linesBeforeCut.toIntOrNull() ?: 3
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        val currentSettings = Settings(
                            stallName = stallName,
                            address = address,
                            phone = phone,
                            pin = pin,
                            printerMac = printerMac,
                            paddingTop = paddingTop.toIntOrNull() ?: 0,
                            paddingBottom = paddingBottom.toIntOrNull() ?: 0,
                            printerWidth = printerWidthValue,
                            linesBeforeCut = linesBeforeCut.toIntOrNull() ?: 3
                        )
                        PrinterUtils.testPrint(
                            context = context,
                            macAddress = printerMac,
                            stallName = currentSettings.stallName,
                            address = currentSettings.address,
                            phone = currentSettings.phone,
                            paddingTop = currentSettings.paddingTop,
                            paddingBottom = currentSettings.paddingBottom,
                            linesBeforeCut = currentSettings.linesBeforeCut,
                            printerWidth = currentSettings.printerWidth
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Print")
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Receipt Preview", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = receiptPreview,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }

    // Bluetooth Device Picker Dialog
    if (showBluetoothPicker) {
        AlertDialog(
            onDismissRequest = { showBluetoothPicker = false },
            title = { 
                Text(
                    "Select Bluetooth Printer",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    if (!bluetoothPermissionGranted) {
                        Text(
                            "Bluetooth permissions are required to scan for devices.\n\nPlease grant permissions to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                bluetoothPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.BLUETOOTH_CONNECT,
                                        Manifest.permission.BLUETOOTH_SCAN
                                    )
                                )
                            }
                        ) {
                            Text("Grant Permissions")
                        }
                    } else if (bluetoothDevices.isEmpty()) {
                        Text(
                            "No paired Bluetooth devices found.\n\nPlease pair your printer in Android Bluetooth settings first.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Steps to pair your printer:\n1. Go to Android Settings\n2. Go to Bluetooth\n3. Turn on Bluetooth\n4. Put your printer in pairing mode\n5. Select your printer from the list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "Choose your printer from the list below:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        bluetoothDevices.forEach { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedDevice = device
                                        printerMac = device.address
                                        showBluetoothPicker = false
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        device.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (device.isPrinter) {
                                        Text(
                                            "Recommended Printer",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                if (bluetoothPermissionGranted) {
                    TextButton(
                        onClick = { 
                            bluetoothDevices = PrinterUtils.getPairedBluetoothDevices(context)
                        }
                    ) {
                        Text("Refresh")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBluetoothPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
package com.example.pos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pos.util.PrinterUtils
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.entity.Settings
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.components.EnhancedCard
import com.example.pos.ui.components.EnhancedButton
import com.example.pos.ui.components.ButtonVariant
import com.example.pos.ui.components.ButtonSize
import kotlinx.coroutines.launch
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.pos.ui.utils.getResponsivePadding


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsDao: SettingsDao,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by settingsDao.getSettings().collectAsState(initial = null)
    
    var stallName by remember { mutableStateOf(settings?.stallName ?: "") }
    var address by remember { mutableStateOf(settings?.address ?: "") }
    var phone by remember { mutableStateOf(settings?.phone ?: "") }
    var pin by remember { mutableStateOf(settings?.pin ?: "1111") }
    var printerMac by remember { mutableStateOf(settings?.printerMac ?: "") }
    var paddingTop by remember { mutableStateOf(settings?.paddingTop?.toString() ?: "0") }
    var paddingBottom by remember { mutableStateOf(settings?.paddingBottom?.toString() ?: "0") }
    var printerWidth by remember { mutableStateOf(settings?.printerWidth?.toString() ?: "32") }
    var linesBeforeCut by remember { mutableStateOf(settings?.linesBeforeCut?.toString() ?: "3") }
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var showClearTransactionsDialog by remember { mutableStateOf(false) }
    
    // Confirmation text fields
    var clearConfirmText by remember { mutableStateOf("") }
    var resetConfirmText by remember { mutableStateOf("") }
    
    // Bluetooth device picker
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var bluetoothDevices by remember { mutableStateOf<List<PrinterUtils.BluetoothDeviceInfo>>(emptyList()) }
    var selectedDevice by remember { mutableStateOf<PrinterUtils.BluetoothDeviceInfo?>(null) }
    var bluetoothPermissionGranted by remember { mutableStateOf(false) }
    var bluetoothError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings) {
        if (settings != null) {
            stallName = settings?.stallName ?: ""
            address = settings?.address ?: ""
            phone = settings?.phone ?: ""
            pin = settings?.pin ?: "1111"
            printerMac = settings?.printerMac ?: ""
            paddingTop = settings?.paddingTop?.toString() ?: "0"
            paddingBottom = settings?.paddingBottom?.toString() ?: "0"
            printerWidth = settings?.printerWidth?.toString() ?: "32"
            linesBeforeCut = settings?.linesBeforeCut?.toString() ?: "3"
        }
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

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Settings",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
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
                                        printerWidth = printerWidth.toIntOrNull() ?: 32,
                                        linesBeforeCut = linesBeforeCut.toIntOrNull() ?: 3
                                    )
                                )
                                showSaveSuccess = true
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Check, "Save Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(getResponsivePadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                EnhancedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Stall Details", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = stallName,
                            onValueChange = { stallName = it },
                            label = { Text("Stall Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }
                }
            }

            item {
                EnhancedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Security", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 4) pin = it },
                            label = { Text("PIN") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                }
            }

            item {
                EnhancedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Printer Settings", style = MaterialTheme.typography.titleLarge)
                    
                    // Bluetooth Device Picker
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Select Printer",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Row {
                                    IconButton(
                                        onClick = { 
                                            if (bluetoothPermissionGranted) {
                                                bluetoothDevices = PrinterUtils.getPairedBluetoothDevices(context)
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
                                        Icon(Icons.Filled.Refresh, "Refresh")
                                    }
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
                                        Icon(Icons.Filled.Bluetooth, "Select Printer")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Choose Printer")
                                    }
                                }
                            }
                            
                            if (selectedDevice != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Selected: ${selectedDevice!!.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "MAC: ${selectedDevice!!.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (printerMac.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Current: $printerMac",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Permission status and error messages
                            if (!bluetoothPermissionGranted) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Bluetooth permissions required",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (bluetoothError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Error: $bluetoothError",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (bluetoothDevices.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No paired devices found. Please pair your printer in Android Bluetooth settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Manual MAC input (fallback)
                    OutlinedTextField(
                        value = printerMac,
                        onValueChange = { printerMac = it },
                        label = { Text("Printer MAC Address (Manual)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Use device picker above for easier setup") }
                    )
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
                    OutlinedTextField(
                        value = printerWidth,
                        onValueChange = { printerWidth = it },
                        label = { Text("Printer Width") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = linesBeforeCut,
                        onValueChange = { linesBeforeCut = it },
                        label = { Text("Lines Before Cut") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    EnhancedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val settings = Settings(
                                        stallName = stallName,
                                        address = address,
                                        phone = phone,
                                        pin = pin,
                                        printerMac = printerMac,
                                        paddingTop = paddingTop.toIntOrNull() ?: 0,
                                        paddingBottom = paddingBottom.toIntOrNull() ?: 0,
                                        printerWidth = printerWidth.toIntOrNull() ?: 32,
                                        linesBeforeCut = linesBeforeCut.toIntOrNull() ?: 3
                                    )
                                    
                                    val success = com.example.pos.util.PrinterUtils.testPrint(
                                        context = context,
                                        macAddress = printerMac,
                                        stallName = settings.stallName,
                                        address = settings.address,
                                        phone = settings.phone,
                                        paddingTop = settings.paddingTop,
                                        paddingBottom = settings.paddingBottom,
                                        linesBeforeCut = settings.linesBeforeCut,
                                        printerWidth = settings.printerWidth
                                    )
                                    
                                    if (success) {
                                        showSaveSuccess = true
                                    } else {
                                        // Show error
                                    }
                                } catch (e: Exception) {
                                    // Show error
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Large
                    ) {
                        Text("Test Print")
                    }
                    }
                }
            }

            item {
                EnhancedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var tapCount by remember { mutableStateOf(0) }
                        val context = LocalContext.current

                        Text(
                            text = "About",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                tapCount++
                                if (tapCount == 5) {
                                    Toast.makeText(context, "v1.0.0 (stable)", Toast.LENGTH_SHORT).show()
                                    tapCount = 0
                                }
                            }
                        )
                        Text(
                            text = "Crafted for offline sales",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Keep sales seamless and your day effortless.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        }
    }

    

    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = { showSaveSuccess = false },
            title = { Text("Success") },
            text = { Text("Settings saved successfully") },
            confirmButton = {
                TextButton(onClick = { showSaveSuccess = false }) {
                    Text("OK")
                }
            }
        )
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
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (device.isPrinter) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Bluetooth,
                                        "Bluetooth Device",
                                        tint = if (device.isPrinter) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            device.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (device.isPrinter) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
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
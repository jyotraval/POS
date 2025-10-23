package com.example.pos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.entity.Settings
import com.example.pos.ui.components.PosTopBar
import kotlinx.coroutines.launch
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight


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
    
    var showResetDialog by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var showClearTransactionsDialog by remember { mutableStateOf(false) }
    
    // Confirmation text fields
    var clearConfirmText by remember { mutableStateOf("") }
    var resetConfirmText by remember { mutableStateOf("") }

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
                                        printerWidth = printerWidth.toIntOrNull() ?: 32
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            }

            item {
                Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Security", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            }

            item {
                Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Printer Settings", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = printerMac,
                        onValueChange = { printerMac = it },
                        label = { Text("Printer MAC Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = paddingTop,
                            onValueChange = { paddingTop = it },
                            label = { Text("Top Padding") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paddingBottom,
                            onValueChange = { paddingBottom = it },
                            label = { Text("Bottom Padding") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = printerWidth,
                        onValueChange = { printerWidth = it },
                        label = { Text("Printer Width") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
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
                                        printerWidth = printerWidth.toIntOrNull() ?: 32
                                    )
                                    
                                    val success = com.example.pos.util.PrinterUtils.testPrint(
                                        context = context,
                                        macAddress = printerMac,
                                        stallName = settings.stallName,
                                        address = settings.address,
                                        phone = settings.phone,
                                        paddingTop = settings.paddingTop,
                                        paddingBottom = settings.paddingBottom
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Print")
                    }
                }
            }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
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
                                    Toast.makeText(context, "v1.0.0 (Stable)", Toast.LENGTH_SHORT).show()
                                    tapCount = 0
                                }
                            }
                        )
                        Text(
                            text = "Crafted by Jyot Raval",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text= "To keeping sales seamless and your day effortless.",
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

    
}
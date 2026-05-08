package com.example.pos.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.dao.ItemDao
import com.example.pos.ui.components.PosTopBar
import com.example.pos.util.InventoryUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    categoryDao: CategoryDao,
    itemDao: ItemDao,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Categories", "Items")
    
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showSampleDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<InventoryUtils.ImportResult?>(null) }
    var exportPath by remember { mutableStateOf<String?>(null) }
    
    // File picker for import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val result = InventoryUtils.importFromCsv(context, it, categoryDao, itemDao)
                importResult = result
                showImportDialog = true
            }
        }
    }
    
    // Export functionality
    fun exportInventory() {
        scope.launch {
            try {
                val path = InventoryUtils.exportToCsv(context, categoryDao, itemDao)
                exportPath = path
                showExportDialog = true
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                PosTopBar(
                    title = "Inventory",
                    onBackClick = onNavigateBack,
                    actions = {
                        IconButton(onClick = { showSampleDialog = true }) {
                            Icon(Icons.Default.Description, "Show Sample Format")
                        }
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.FileUpload, "Import CSV")
                        }
                        IconButton(onClick = { exportInventory() }) {
                            Icon(Icons.Default.FileDownload, "Export CSV")
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> CategoryListScreen(
                    categoryDao = categoryDao,
                    onNavigateBack = { selectedTab = 1 }
                )
                1 -> ItemListScreen(
                    categoryDao = categoryDao,
                    itemDao = itemDao,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
    
    // Import Result Dialog
    if (showImportDialog && importResult != null) {
        ImportResultDialog(
            result = importResult!!,
            onDismiss = { 
                showImportDialog = false
                importResult = null
            }
        )
    }
    
    // Export Success Dialog
    if (showExportDialog && exportPath != null) {
        ExportSuccessDialog(
            filePath = exportPath!!,
            onDismiss = { 
                showExportDialog = false
                exportPath = null
            }
        )
    }
    
    // Sample Format Dialog
    if (showSampleDialog) {
        SampleFormatDialog(
            onDismiss = { showSampleDialog = false }
        )
    }
}

@Composable
private fun ImportResultDialog(
    result: InventoryUtils.ImportResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (result.success) "Import Successful" else "Import Failed",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(result.message)
                if (result.success) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Items added: ${result.itemsAdded}")
                    Text("Categories added: ${result.categoriesAdded}")
                }
                if (result.errors.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Errors:", fontWeight = FontWeight.Bold)
                    result.errors.forEach { error ->
                        Text("• $error", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun ExportSuccessDialog(
    filePath: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Export Successful",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Inventory exported successfully!")
                Spacer(modifier = Modifier.height(8.dp))
                Text("File saved to:")
                Text(
                    filePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun SampleFormatDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "File Format Guide",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Your file should have the following format:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Supported format: CSV (.csv)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Column names: Item, Category, Price",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Sample data:")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = InventoryUtils.getSampleCsvContent(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Note: Categories will be created automatically if they don't exist. Column names are case-insensitive.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
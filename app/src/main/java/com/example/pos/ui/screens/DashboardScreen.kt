package com.example.pos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.dao.TransactionDao
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.components.EnhancedCard
import com.example.pos.ui.components.MetricCard
import com.example.pos.ui.components.StatusChip
import com.example.pos.ui.components.ChipStatus
import com.example.pos.ui.components.TrendDirection
import com.example.pos.ui.viewmodels.DashboardViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    transactionDao: TransactionDao,
    itemDao: ItemDao,
    onNavigateBack: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(transactionDao, itemDao)
    )

    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val salesData by viewModel.salesData.collectAsState()
    val topSellingItems by viewModel.topSellingItems.collectAsState()
    val totalSales by viewModel.totalSales.collectAsState()
    val averageDailySales by viewModel.averageDailySales.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Dashboard",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, "Select Date Range")
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            if (viewMode == DashboardViewModel.ViewMode.DAILY) 
                                Icons.Filled.CalendarMonth
                            else 
                                Icons.AutoMirrored.Filled.List,
                            "Toggle View Mode"
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Filled.FileDownload, "Export Data")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MetricCard(
                        title = "Total Sales",
                        value = formatPrice(totalSales),
                        icon = Icons.Filled.TrendingUp,
                        trend = TrendDirection.Up,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Average Daily Sales",
                        value = formatPrice(averageDailySales),
                        icon = Icons.Filled.CalendarMonth,
                        trend = TrendDirection.Neutral,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sales Chart with enhanced styling
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (viewMode == DashboardViewModel.ViewMode.DAILY)
                                    "Daily Sales"
                                else
                                    "Monthly Sales",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // Toggle buttons
                            Row {
                                FilterChip(
                                    selected = viewMode == DashboardViewModel.ViewMode.DAILY,
                                    onClick = { 
                                        if (viewMode != DashboardViewModel.ViewMode.DAILY) {
                                            viewModel.toggleViewMode()
                                        }
                                    },
                                    label = { Text("Daily") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = viewMode == DashboardViewModel.ViewMode.MONTHLY,
                                    onClick = { 
                                        if (viewMode != DashboardViewModel.ViewMode.MONTHLY) {
                                            viewModel.toggleViewMode()
                                        }
                                    },
                                    label = { Text("Monthly") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        // Simple Bar Chart Visualization
                        if (salesData.isNotEmpty()) {
                            val maxAmount = salesData.maxOfOrNull { it.total } ?: 1.0
                            
                            salesData.take(7).forEach { sale ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        SimpleDateFormat(
                                            if (viewMode == DashboardViewModel.ViewMode.DAILY) "dd MMM"
                                            else "MMM yyyy",
                                            Locale.getDefault()
                                        ).format(sale.date),
                                        modifier = Modifier.width(80.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Bar visualization
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(20.dp)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        // Background bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    RoundedCornerShape(10.dp)
                                                )
                                        )
                                        
                                        // Filled bar
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth((sale.total / maxAmount).toFloat())
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(10.dp)
                                                )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        formatPrice(sale.total),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(80.dp)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No sales data available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Top Selling Items
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Top Selling (Items)",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        topSellingItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        item.itemName,
                                        style = MaterialTheme.typography.titleMedium
                                        )
                                    Text(
                                        "Quantity Sold: ${item.quantitySold}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    formatPrice(item.revenue),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { start: Date, end: Date ->
                viewModel.setDateRange(start, end)
                showDatePicker = false
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data") },
            text = { Text("Choose what data to export:") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Implement export functionality
                        showExportDialog = false
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Date, Date) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    var startDate by remember { 
        mutableStateOf(calendar.apply { add(Calendar.MONTH, -1) }.time)
    }
    var endDate by remember { 
        mutableStateOf(Calendar.getInstance().time)
    }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start: ${dateFormat.format(startDate)}")
                }
                
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("End: ${dateFormat.format(endDate)}")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Set start date to beginning of day
                    val start = Calendar.getInstance().apply {
                        time = startDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time

                    // Set end date to end of day
                    val end = Calendar.getInstance().apply {
                        time = endDate
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.time

                    onConfirm(start, end)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(onClick = { showStartDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = startDate.time,
                yearRange = IntRange(2020, 2030),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
                }
            )
            LaunchedEffect(state.selectedDateMillis) {
                state.selectedDateMillis?.let { millis ->
                    startDate = Date(millis)
                }
            }
            DatePicker(
                state = state,
                showModeToggle = false,
                title = null
            )
        }
    }
    
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(onClick = { showEndDatePicker = false }) {
                    Text("OK")
                }
            }
        ) {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = endDate.time,
                yearRange = IntRange(2020, 2030),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
                }
            )
            LaunchedEffect(state.selectedDateMillis) {
                state.selectedDateMillis?.let { millis ->
                    endDate = Date(millis)
                }
            }
            DatePicker(
                state = state,
                showModeToggle = false,
                title = null
            )
        }
    }
}

private fun formatPrice(price: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        .format(price)
}
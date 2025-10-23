package com.example.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.dao.TransactionDao
import com.example.pos.data.entity.Transaction
import com.example.pos.data.entity.TransactionItem
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.viewmodels.TransactionViewModel
import com.example.pos.util.PrinterUtils
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactionDao: TransactionDao,
    itemDao: ItemDao,
    settingsDao: SettingsDao,
    onNavigateBack: () -> Unit
) {
    val viewModel: TransactionViewModel = viewModel(
        factory = TransactionViewModel.Factory(transactionDao, itemDao, settingsDao)
    )

    val transactions by viewModel.transactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTransaction by viewModel.selectedTransaction.collectAsState()
    val selectedTransactionItems by viewModel.selectedTransactionItems.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTransactionDetails by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        when (uiState) {
            is TransactionViewModel.TransactionUiState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as TransactionViewModel.TransactionUiState.Success).message
                    )
                }
                viewModel.clearState()
            }
            is TransactionViewModel.TransactionUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as TransactionViewModel.TransactionUiState.Error).message
                    )
                }
                viewModel.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            Column {
                PosTopBar(
                    title = "Transactions",
                    onBackClick = onNavigateBack,
                    actions = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Select Date Range")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by TXN ID, buyer name, or phone") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, "Search")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val calendar = Calendar.getInstance()
                
                // Group transactions by day
                val groupedTransactions = transactions.groupBy { transaction ->
                    Calendar.getInstance().apply {
                        time = transaction.dateTime
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                }
                
                // Sort groups by date in descending order
                groupedTransactions.toSortedMap(compareByDescending { it })
                    .forEach { (date, dayTransactions) ->
                        item {
                            Text(
                                text = dateFormat.format(date),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                    }
                    items(
                        items = dayTransactions,
                        key = { it.txnId } // Using txnId as a stable key for better performance
                    ) { transaction: Transaction ->
                        TransactionCard(
                            transaction = transaction,
                            onClick = {
                                viewModel.selectTransaction(transaction)
                                showTransactionDetails = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { startDate: Date, endDate: Date ->
                viewModel.setDateRange(startDate, endDate)
                showDatePicker = false
            }
        )
    }

    if (showTransactionDetails && selectedTransaction != null) {
        var itemNames by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
        
        LaunchedEffect(selectedTransactionItems) {
            itemNames = viewModel.getItemNames(selectedTransactionItems.map { it.itemId })
        }
        
        TransactionDetailsDialog(
            transaction = selectedTransaction!!,
            items = selectedTransactionItems,
            itemNames = itemNames,
            onDismiss = {
                showTransactionDetails = false
                viewModel.selectTransaction(null)
            },
            onReprint = {
                viewModel.reprintReceipt(selectedTransaction!!)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionCard(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaction.txnId,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatPrice(transaction.total),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaction.buyerName ?: "Walk-in Customer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(transaction.dateTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailsDialog(
    transaction: Transaction,
    items: List<TransactionItem>,
    itemNames: Map<Long, String>,
    onDismiss: () -> Unit,
    onReprint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaction Details") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("TXN ID: ${transaction.txnId}")
                Text(
                    "Date: ${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                        .format(transaction.dateTime)}"
                )
                if (!transaction.buyerName.isNullOrBlank()) {
                    Text("Buyer: ${transaction.buyerName}")
                }
                if (!transaction.buyerPhone.isNullOrBlank()) {
                    Text("Phone: ${transaction.buyerPhone}")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Items:", style = MaterialTheme.typography.titleMedium)
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.quantity}x ${itemNames[item.itemId] ?: "Unknown Item"}")
                        Text(formatPrice(item.lineTotal))
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:")
                    Text(formatPrice(transaction.subtotal))
                }
                if (transaction.discount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Discount:")
                        Text("-${formatPrice(transaction.discount)}")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatPrice(transaction.total),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onReprint) {
                Text("Reprint")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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
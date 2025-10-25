package com.example.pos.ui.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.dao.TransactionDao
import com.example.pos.data.entity.Transaction
import com.example.pos.util.PrinterUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val itemDao: ItemDao,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _startDate = MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    )

    private val _endDate = MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    )

    val transactions = combine(
        _searchQuery,
        _startDate,
        _endDate
    ) { query, start, end ->
        if (query.isBlank()) {
            transactionDao.getTransactionsBetweenDates(start, end)
        } else {
            transactionDao.searchTransactions(query)
        }
    }.flatMapLatest { it }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransaction.asStateFlow()

    val selectedTransactionItems = _selectedTransaction
        .filterNotNull()
        .flatMapLatest { transaction ->
            transactionDao.getTransactionItems(transaction.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<TransactionUiState>(TransactionUiState.Default)
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setDateRange(start: Date, end: Date) {
        _startDate.value = Calendar.getInstance().apply {
            time = start
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        _endDate.value = Calendar.getInstance().apply {
            time = end
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    fun selectTransaction(transaction: Transaction?) {
        _selectedTransaction.value = transaction
    }

    suspend fun getItemNames(itemIds: List<Long>): Map<Long, String> {
        return try {
            itemIds.associateWith { itemId ->
                itemDao.getItemById(itemId)?.name ?: "Unknown Item"
            }
        } catch (e: Exception) {
            itemIds.associateWith { "Unknown Item" }
        }
    }

    fun reprintReceipt(transaction: Transaction) {
        viewModelScope.launch {
            try {
                val settings = settingsDao.getSettings().first()
                if (settings == null) {
                    _uiState.value = TransactionUiState.Error("Printer not configured")
                    return@launch
                }

                val printerMac = settings.printerMac
                if (printerMac.isNullOrBlank()) {
                    _uiState.value = TransactionUiState.Error("Printer MAC address not set")
                    return@launch
                }

                val items = selectedTransactionItems.value.map { transactionItem ->
                    val item = itemDao.getItemById(transactionItem.itemId)
                    PrinterUtils.ReceiptItem(
                        name = item?.name ?: "Unknown Item",
                        quantity = transactionItem.quantity,
                        unitPrice = transactionItem.unitPrice,
                        total = transactionItem.lineTotal
                    )
                }

                val success = PrinterUtils.printReceipt(
                    context = context,
                    macAddress = printerMac,
                    stallName = settings.stallName,
                    address = settings.address,
                    phone = settings.phone,
                    txnId = transaction.txnId,
                    buyerName = transaction.buyerName,
                    buyerPhone = transaction.buyerPhone,
                    items = items,
                    subtotal = transaction.subtotal,
                    discount = transaction.discount,
                    total = transaction.total,
                    paddingTop = settings.paddingTop,
                    paddingBottom = settings.paddingBottom
                )

                if (success) {
                    _uiState.value = TransactionUiState.Success("Receipt reprinted successfully")
                } else {
                    _uiState.value = TransactionUiState.Error("Failed to print receipt")
                }
            } catch (e: Exception) {
                _uiState.value = TransactionUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun clearState() {
        _uiState.value = TransactionUiState.Default
    }

    sealed class TransactionUiState {
        object Default : TransactionUiState()
        data class Success(val message: String) : TransactionUiState()
        data class Error(val message: String) : TransactionUiState()
    }

    class Factory(
        private val context: Context,
        private val transactionDao: TransactionDao,
        private val itemDao: ItemDao,
        private val settingsDao: SettingsDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                return TransactionViewModel(context, transactionDao, itemDao, settingsDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
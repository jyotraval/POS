package com.example.pos.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.dao.TransactionDao
import kotlinx.coroutines.flow.*
import java.util.*

data class DailySales(
    val date: Date,
    val total: Double
)

data class TopSellingItem(
    val itemName: String,
    val quantity: Int,
    val revenue: Double
)

class DashboardViewModel(
    private val transactionDao: TransactionDao,
    private val itemDao: ItemDao
) : ViewModel() {

    private val _startDate = MutableStateFlow(
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    )
    val startDate: StateFlow<Date> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    )
    val endDate: StateFlow<Date> = _endDate.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.DAILY)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    val salesData = combine(_startDate, _endDate, _viewMode) { start, end, mode ->
        transactionDao.getTransactionsBetweenDates(start, end).first().let { transactions ->
            when (mode) {
                ViewMode.DAILY -> transactions
                    .groupBy { transaction ->
                        Calendar.getInstance().apply {
                            time = transaction.dateTime
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                    }
                    .map { (date, dayTransactions) ->
                        DailySales(
                            date = date,
                            total = dayTransactions.sumOf { it.total }
                        )
                    }
                    .sortedBy { it.date }

                ViewMode.MONTHLY -> transactions
                    .groupBy { transaction ->
                        Calendar.getInstance().apply {
                            time = transaction.dateTime
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                    }
                    .map { (date, monthTransactions) ->
                        DailySales(
                            date = date,
                            total = monthTransactions.sumOf { it.total }
                        )
                    }
                    .sortedBy { it.date }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val topSellingItems = combine(_startDate, _endDate) { start, end ->
        itemDao.getTopSellingItems(10).first()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalSales = salesData.map { data ->
        data.sumOf { it.total }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val averageDailySales = salesData.map { data ->
        if (data.isEmpty()) 0.0 else data.sumOf { it.total } / data.size
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun setDateRange(start: Date, end: Date) {
        _startDate.value = Calendar.getInstance().apply {
            time = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        _endDate.value = Calendar.getInstance().apply {
            time = end
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.DAILY -> ViewMode.MONTHLY
            ViewMode.MONTHLY -> ViewMode.DAILY
        }
    }

    fun exportData(): String {
        // TODO: Implement CSV export
        return ""
    }

    enum class ViewMode {
        DAILY, MONTHLY
    }

    class Factory(
        private val transactionDao: TransactionDao,
        private val itemDao: ItemDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(transactionDao, itemDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
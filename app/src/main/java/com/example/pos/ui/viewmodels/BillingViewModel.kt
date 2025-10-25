package com.example.pos.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.dao.TransactionDao
import com.example.pos.data.entity.Category
import com.example.pos.data.entity.Item
import com.example.pos.data.entity.Transaction
import com.example.pos.data.entity.TransactionItem
import com.example.pos.data.model.CartItem
import com.example.pos.util.PrinterUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class BillingViewModel(
    private val context: Context,
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val transactionDao: TransactionDao,
    private val settingsDao: SettingsDao
) : ViewModel() {
    val categories = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    val items = _selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) {
            itemDao.getAllItems()
        } else {
            itemDao.getItemsByCategory(categoryId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _cart = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    val cart: StateFlow<Map<Long, CartItem>> = _cart.asStateFlow()

    private val _buyerName = MutableStateFlow<String>("")
    val buyerName: StateFlow<String> = _buyerName.asStateFlow()

    private val _buyerPhone = MutableStateFlow<String>("")
    val buyerPhone: StateFlow<String> = _buyerPhone.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _discountPercent = MutableStateFlow(0.0)
    val discountPercent: StateFlow<Double> = _discountPercent.asStateFlow()

    private val _uiState = MutableStateFlow<BillingUiState>(BillingUiState.Default)
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    val subtotal = cart.map { items ->
        items.values.sumOf { it.lineTotal }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val totalDiscount = combine(subtotal, discountAmount, discountPercent) { subtotal, amount, percent ->
        amount + (subtotal * (percent / 100))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    val total = combine(subtotal, totalDiscount) { subtotal, discount ->
        subtotal - discount
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun updateBuyerName(name: String) {
        _buyerName.value = name
    }

    fun updateBuyerPhone(phone: String) {
        _buyerPhone.value = phone
    }

    fun setDiscountAmount(amount: Double) {
        _discountAmount.value = amount
        _discountPercent.value = 0.0
    }

    fun setDiscountPercent(percent: Double) {
        _discountPercent.value = percent
        _discountAmount.value = 0.0
    }

    fun addToCart(item: Item) {
        val currentCart = _cart.value.toMutableMap()
        val currentItem = currentCart[item.id]
        
        if (currentItem != null) {
            currentCart[item.id] = currentItem.copy(
                quantity = currentItem.quantity + 1,
                lineTotal = item.price * (currentItem.quantity + 1)
            )
        } else {
            currentCart[item.id] = CartItem(
                item = item,
                quantity = 1,
                lineTotal = item.price
            )
        }
        
        _cart.value = currentCart
    }

    fun removeFromCart(itemId: Long) {
        val currentCart = _cart.value.toMutableMap()
        currentCart.remove(itemId)
        _cart.value = currentCart
    }

    fun updateQuantity(itemId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(itemId)
            return
        }

        val currentCart = _cart.value.toMutableMap()
        val cartItem = currentCart[itemId] ?: return
        
        currentCart[itemId] = cartItem.copy(
            quantity = quantity,
            lineTotal = cartItem.item.price * quantity
        )
        
        _cart.value = currentCart
    }

    fun clearCart() {
        _cart.value = emptyMap()
        _buyerName.value = ""
        _buyerPhone.value = ""
        _discountAmount.value = 0.0
        _discountPercent.value = 0.0
    }

    fun printReceipt() {
        viewModelScope.launch {
            try {
                val settings = settingsDao.getSettings().first()
                if (settings == null) {
                    _uiState.value = BillingUiState.Error("Printer not configured")
                    return@launch
                }

                val printerMac = settings.printerMac
                if (printerMac.isNullOrBlank()) {
                    _uiState.value = BillingUiState.Error("Printer MAC address not set")
                    return@launch
                }

                // Create transaction
                val now = Date()
                val calendar = Calendar.getInstance()
                calendar.time = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time
                
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.time
                
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
                val txnId = "TXN${dateFormat.format(now)}-" +
                        String.format("%03d", transactionDao.getTransactionsForDate(startOfDay, endOfDay).first().size + 1)

                val transaction = Transaction(
                    txnId = txnId,
                    buyerName = buyerName.value.takeIf { it.isNotBlank() },
                    buyerPhone = buyerPhone.value.takeIf { it.isNotBlank() },
                    dateTime = now,
                    subtotal = subtotal.value,
                    discount = totalDiscount.value,
                    total = total.value
                )

                val items = cart.value.values.map { cartItem ->
                    TransactionItem(
                        transactionId = 0, // Will be set by Room
                        itemId = cartItem.item.id,
                        quantity = cartItem.quantity,
                        unitPrice = cartItem.item.price,
                        lineTotal = cartItem.lineTotal
                    )
                }

                val transactionId = transactionDao.insertTransactionWithItems(transaction, items)

                // Print receipt
                val success = PrinterUtils.printReceipt(
                    context = context,
                    macAddress = printerMac,
                    stallName = settings.stallName,
                    address = settings.address,
                    phone = settings.phone,
                    txnId = txnId,
                    buyerName = buyerName.value,
                    buyerPhone = buyerPhone.value,
                    items = cart.value.values.map { cartItem ->
                        PrinterUtils.ReceiptItem(
                            name = cartItem.item.name,
                            quantity = cartItem.quantity,
                            unitPrice = cartItem.item.price,
                            total = cartItem.lineTotal
                        )
                    },
                    subtotal = subtotal.value,
                    discount = totalDiscount.value,
                    total = total.value,
                    paddingTop = settings.paddingTop,
                    paddingBottom = settings.paddingBottom
                )

                if (success) {
                    _uiState.value = BillingUiState.Success("Receipt printed successfully")
                    clearCart()
                } else {
                    _uiState.value = BillingUiState.Error("Failed to print receipt")
                }
            } catch (e: Exception) {
                _uiState.value = BillingUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun saveTransactionOnly() {
        viewModelScope.launch {
            try {
                val settings = settingsDao.getSettings().first()
                if (settings == null) {
                    _uiState.value = BillingUiState.Error("Please configure settings first")
                    return@launch
                }

                val now = Date()
                val calendar = Calendar.getInstance()
                calendar.time = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.time
                val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
                val txnId = "TXN${dateFormat.format(now)}-" +
                        String.format("%03d", transactionDao.getTransactionsForDate(startOfDay, endOfDay).first().size + 1)

                val transaction = Transaction(
                    txnId = txnId,
                    buyerName = buyerName.value.takeIf { it.isNotBlank() },
                    buyerPhone = buyerPhone.value.takeIf { it.isNotBlank() },
                    dateTime = now,
                    subtotal = subtotal.value,
                    discount = totalDiscount.value,
                    total = total.value
                )

                val items = cart.value.values.map { cartItem ->
                    TransactionItem(
                        transactionId = 0, // Will be set by Room
                        itemId = cartItem.item.id,
                        quantity = cartItem.quantity,
                        unitPrice = cartItem.item.price,
                        lineTotal = cartItem.lineTotal
                    )
                }

                transactionDao.insertTransactionWithItems(transaction, items)
                clearCart()
                _uiState.value = BillingUiState.Success("Transaction saved successfully")
            } catch (e: Exception) {
                _uiState.value = BillingUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun clearState() {
        _uiState.value = BillingUiState.Default
    }

    sealed class BillingUiState {
        object Default : BillingUiState()
        data class Success(val message: String) : BillingUiState()
        data class Error(val message: String) : BillingUiState()
    }

    class Factory(
        private val context: Context,
        private val categoryDao: CategoryDao,
        private val itemDao: ItemDao,
        private val transactionDao: TransactionDao,
        private val settingsDao: SettingsDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
                return BillingViewModel(context, categoryDao, itemDao, transactionDao, settingsDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
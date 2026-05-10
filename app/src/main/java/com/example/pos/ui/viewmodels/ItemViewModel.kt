package com.example.pos.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.entity.Item
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class ItemViewModel(
    private val itemDao: ItemDao
) : ViewModel() {
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

    private val _uiState = MutableStateFlow<ItemUiState>(ItemUiState.Default)
    val uiState: StateFlow<ItemUiState> = _uiState.asStateFlow()

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun addItem(name: String, price: Double, categoryId: Long, isPinned: Boolean = false) {
        if (name.isBlank()) {
            _uiState.value = ItemUiState.Error("Item name cannot be empty")
            return
        }
        if (price <= 0) {
            _uiState.value = ItemUiState.Error("Price must be greater than 0")
            return
        }

        viewModelScope.launch {
            try {
                itemDao.insert(Item(name = name.trim(), price = price, categoryId = categoryId, isPinned = isPinned))
                _uiState.value = ItemUiState.Success("Item added successfully")
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Failed to add item")
            }
        }
    }

    fun updateItem(item: Item) {
        if (item.name.isBlank()) {
            _uiState.value = ItemUiState.Error("Item name cannot be empty")
            return
        }
        if (item.price <= 0) {
            _uiState.value = ItemUiState.Error("Price must be greater than 0")
            return
        }

        viewModelScope.launch {
            try {
                itemDao.update(item)
                _uiState.value = ItemUiState.Success("Item updated successfully")
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Failed to update item")
            }
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                itemDao.delete(item)
                _uiState.value = ItemUiState.Success("Item deleted successfully")
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Failed to delete item")
            }
        }
    }

    fun togglePin(item: Item) {
        viewModelScope.launch {
            try {
                itemDao.update(item.copy(isPinned = !item.isPinned))
                _uiState.value = ItemUiState.Success(if (!item.isPinned) "Item pinned" else "Item unpinned")
            } catch (e: Exception) {
                _uiState.value = ItemUiState.Error("Failed to toggle pin")
            }
        }
    }

    fun clearState() {
        _uiState.value = ItemUiState.Default
    }

    sealed class ItemUiState {
        object Default : ItemUiState()
        data class Success(val message: String) : ItemUiState()
        data class Error(val message: String) : ItemUiState()
    }

    class Factory(private val itemDao: ItemDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
                return ItemViewModel(itemDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
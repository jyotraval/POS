package com.example.pos.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.entity.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val categoryDao: CategoryDao
) : ViewModel() {
    val categories = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Default)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    fun addCategory(name: String) {
        if (name.isBlank()) {
            _uiState.value = CategoryUiState.Error("Category name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                categoryDao.insert(Category(name = name.trim()))
                _uiState.value = CategoryUiState.Success("Category added successfully")
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("Failed to add category")
            }
        }
    }

    fun updateCategory(category: Category) {
        if (category.name.isBlank()) {
            _uiState.value = CategoryUiState.Error("Category name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                categoryDao.update(category)
                _uiState.value = CategoryUiState.Success("Category updated successfully")
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("Failed to update category")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                categoryDao.delete(category)
                _uiState.value = CategoryUiState.Success("Category deleted successfully")
            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error("Failed to delete category")
            }
        }
    }

    fun clearState() {
        _uiState.value = CategoryUiState.Default
    }

    sealed class CategoryUiState {
        object Default : CategoryUiState()
        data class Success(val message: String) : CategoryUiState()
        data class Error(val message: String) : CategoryUiState()
    }

    class Factory(private val categoryDao: CategoryDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
                return CategoryViewModel(categoryDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
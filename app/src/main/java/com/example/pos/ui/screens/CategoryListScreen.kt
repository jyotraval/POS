package com.example.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.entity.Category
import com.example.pos.ui.components.EnhancedCard
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.utils.getResponsivePadding
import com.example.pos.ui.utils.getResponsiveSpacing
import com.example.pos.ui.viewmodels.CategoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    categoryDao: CategoryDao,
    onNavigateBack: () -> Unit
) {
    val viewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModel.Factory(categoryDao)
    )

    val categories by viewModel.categories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        when (uiState) {
            is CategoryViewModel.CategoryUiState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as CategoryViewModel.CategoryUiState.Success).message
                    )
                }
                viewModel.clearState()
            }
            is CategoryViewModel.CategoryUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as CategoryViewModel.CategoryUiState.Error).message
                    )
                }
                viewModel.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Categories",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add Category")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(getResponsivePadding()),
            verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
        ) {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { selectedCategory = it },
                    onDelete = {
                        selectedCategory = it
                        showDeleteDialog = true
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            category = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addCategory(name)
                showAddDialog = false
            }
        )
    }

    selectedCategory?.let { category ->
        if (!showDeleteDialog) {
            CategoryDialog(
                category = category,
                onDismiss = { selectedCategory = null },
                onConfirm = { name ->
                    viewModel.updateCategory(category.copy(name = name))
                    selectedCategory = null
                }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedCategory = null
            },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete this category? This will also delete all items in this category.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedCategory?.let { viewModel.deleteCategory(it) }
                        showDeleteDialog = false
                        selectedCategory = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedCategory = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryItem(
    modifier: Modifier = Modifier,
    category: Category,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    EnhancedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onEdit(category) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = { onDelete(category) }) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CategoryDialog(
    category: Category?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Category Name") },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Category name cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(name)
                    }
                }
            ) {
                Text(if (category == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
package com.example.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.entity.Category
import com.example.pos.data.entity.Item
import com.example.pos.ui.components.EnhancedCard
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.utils.getResponsivePadding
import com.example.pos.ui.utils.getResponsiveSpacing
import com.example.pos.ui.viewmodels.CategoryViewModel
import com.example.pos.ui.viewmodels.ItemViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    categoryDao: CategoryDao,
    itemDao: ItemDao,
    onNavigateBack: () -> Unit
) {
    val categoryViewModel: CategoryViewModel = viewModel(
        factory = CategoryViewModel.Factory(categoryDao)
    )
    val itemViewModel: ItemViewModel = viewModel(
        factory = ItemViewModel.Factory(itemDao)
    )

    val categories by categoryViewModel.categories.collectAsState()
    val items by itemViewModel.items.collectAsState()
    val selectedCategoryId by itemViewModel.selectedCategoryId.collectAsState()
    val uiState by itemViewModel.uiState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        when (uiState) {
            is ItemViewModel.ItemUiState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as ItemViewModel.ItemUiState.Success).message
                    )
                }
                itemViewModel.clearState()
            }
            is ItemViewModel.ItemUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as ItemViewModel.ItemUiState.Error).message
                    )
                }
                itemViewModel.clearState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            PosTopBar(
                title = "Items",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        enabled = categories.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, "Add Item")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category filter
            LazyRow(
                contentPadding = PaddingValues(getResponsivePadding()),
                horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { itemViewModel.setSelectedCategory(null) },
                        label = { Text("All") }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { itemViewModel.setSelectedCategory(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            // Items list
            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(getResponsivePadding()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add categories first before adding items",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val categoryMap = remember(categories) { categories.associateBy { it.id } }

                LazyColumn(
                    contentPadding = PaddingValues(getResponsivePadding()),
                    verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            category = categoryMap[item.categoryId],
                            onEdit = { selectedItem = it },
                            onDelete = {
                                selectedItem = it
                                showDeleteDialog = true
                            },
                            onPin = { itemViewModel.togglePin(it) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ItemDialog(
            item = null,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, price, categoryId, isPinned ->
                itemViewModel.addItem(name, price, categoryId, isPinned)
                showAddDialog = false
            }
        )
    }

    selectedItem?.let { item ->
        if (!showDeleteDialog) {
            ItemDialog(
                item = item,
                categories = categories,
                selectedCategoryId = item.categoryId,
                onDismiss = { selectedItem = null },
                onConfirm = { name, price, categoryId, isPinned ->
                    itemViewModel.updateItem(item.copy(name = name, price = price, categoryId = categoryId, isPinned = isPinned))
                    selectedItem = null
                }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedItem = null
            },
            title = { Text("Delete Item") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedItem?.let { itemViewModel.deleteItem(it) }
                        showDeleteDialog = false
                        selectedItem = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedItem = null
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
private fun ItemCard(
    modifier: Modifier = Modifier,
    item: Item,
    category: Category?,
    onEdit: (Item) -> Unit,
    onDelete: (Item) -> Unit,
    onPin: (Item) -> Unit
) {
    EnhancedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onEdit(item) }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = category?.name ?: "Unknown Category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }
                Text(
                    text = currencyFormatter.format(item.price),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { onPin(item) }) {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        contentDescription = if (item.isPinned) "Unpin" else "Pin",
                        tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onDelete(item) }) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDialog(
    item: Item?,
    categories: List<Category>,
    selectedCategoryId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var price by remember { mutableStateOf(item?.price?.toString() ?: "") }
    var categoryId by remember { mutableStateOf(item?.categoryId ?: selectedCategoryId ?: categories.firstOrNull()?.id ?: 0) }
    var isPinned by remember { mutableStateOf(item?.isPinned ?: false) }
    
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Item" else "Edit Item") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Item Name") },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Item name cannot be empty") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it
                        priceError = false
                    },
                    label = { Text("Price") },
                    singleLine = true,
                    isError = priceError,
                    supportingText = if (priceError) {
                        { Text("Enter a valid price") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = categories.find { it.id == categoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = { 
                                    categoryId = category.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = name.isBlank()
                    priceError = price.toDoubleOrNull() == null || price.toDouble() <= 0

                    if (!nameError && !priceError) {
                        onConfirm(name, price.toDouble(), categoryId, isPinned)
                    }
                }
            ) {
                Text(if (item == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
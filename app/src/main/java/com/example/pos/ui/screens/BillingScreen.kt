package com.example.pos.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.dao.ItemDao
import com.example.pos.data.dao.SettingsDao
import com.example.pos.data.dao.TransactionDao
import com.example.pos.data.entity.Item
import com.example.pos.data.model.CartItem
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.components.EnhancedCard
import com.example.pos.ui.components.EnhancedButton
import com.example.pos.ui.components.ButtonVariant
import com.example.pos.ui.components.ButtonSize
import com.example.pos.ui.components.StatusChip
import com.example.pos.ui.components.ChipStatus
import com.example.pos.ui.theme.SelectedItemBorder
import com.example.pos.ui.theme.CartHighlight
import com.example.pos.ui.utils.*
import com.example.pos.ui.viewmodels.BillingViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

// Reusable formatter to avoid allocating per-item
private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    context: Context,
    categoryDao: CategoryDao,
    itemDao: ItemDao,
    transactionDao: TransactionDao,
    settingsDao: SettingsDao,
    onNavigateBack: () -> Unit
) {
    val viewModel: BillingViewModel = viewModel(
        factory = BillingViewModel.Factory(context, categoryDao, itemDao, transactionDao, settingsDao)
    )

    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val items by viewModel.items.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val buyerName by viewModel.buyerName.collectAsState()
    val buyerPhone by viewModel.buyerPhone.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val discountPercent by viewModel.discountPercent.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val totalDiscount by viewModel.totalDiscount.collectAsState()
    val total by viewModel.total.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showDiscountDialog by remember { mutableStateOf(false) }
    var showClearCartDialog by remember { mutableStateOf(false) }
    var showBillPreview by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        when (uiState) {
            is BillingViewModel.BillingUiState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as BillingViewModel.BillingUiState.Success).message
                    )
                }
                viewModel.clearState()
            }
            is BillingViewModel.BillingUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        (uiState as BillingViewModel.BillingUiState.Error).message
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
                title = "Billing",
                onBackClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Categories with enhanced styling
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyRow(
                    contentPadding = PaddingValues(getResponsivePadding()),
                    horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { viewModel.setSelectedCategory(null) },
                            label = { 
                                Text(
                                    "All",
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { viewModel.setSelectedCategory(category.id) },
                            label = { 
                                Text(
                                    category.name,
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                    }
                }

                // Items Grid
                LazyVerticalGrid(
                columns = GridCells.Fixed(getResponsiveGridColumns()),
                contentPadding = PaddingValues(getResponsivePadding()),
                horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing()),
                verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing()),
                modifier = Modifier.weight(1f)
                ) {
                    items(
                            items = items,
                            key = { it.id }
                        ) { item ->
                        ItemTile(
                            item = item,
                            cartItem = cart[item.id],
                            onAdd = { viewModel.addToCart(item) },
                            onRemove = { viewModel.removeFromCart(item.id) },
                            onUpdateQuantity = { qty -> viewModel.updateQuantity(item.id, qty) }
                        )
                }
            }

            // Bottom Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = getResponsiveCardElevation()
            ) {
                BottomActionBar(
                    cart = cart,
                    subtotal = subtotal,
                    totalDiscount = totalDiscount,
                    total = total,
                    buyerName = buyerName,
                    buyerPhone = buyerPhone,
                    onUpdateBuyerName = viewModel::updateBuyerName,
                    onUpdateBuyerPhone = viewModel::updateBuyerPhone,
                    onShowDiscountDialog = { showDiscountDialog = true },
                    onShowClearCartDialog = { showClearCartDialog = true },
                    onShowBillPreview = { showBillPreview = true }
                )
            }
        }
    }

    if (showDiscountDialog) {
        DiscountDialog(
            currentAmount = discountAmount,
            currentPercent = discountPercent,
            onDismiss = { showDiscountDialog = false },
            onApplyAmount = {
                viewModel.setDiscountAmount(it)
                showDiscountDialog = false
            },
            onApplyPercent = {
                viewModel.setDiscountPercent(it)
                showDiscountDialog = false
            }
        )
    }

    if (showClearCartDialog) {
        AlertDialog(
            onDismissRequest = { showClearCartDialog = false },
            title = { Text("Clear Cart") },
            text = { Text("Are you sure you want to clear the cart?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCart()
                        showClearCartDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCartDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBillPreview) {
        BillPreviewDialog(
            cart = cart,
            subtotal = subtotal,
            totalDiscount = totalDiscount,
            total = total,
            buyerName = buyerName,
            buyerPhone = buyerPhone,
            onDismiss = { showBillPreview = false },
            onSaveOnly = {
                viewModel.saveTransactionOnly()
                showBillPreview = false
            },
            onSaveAndPrint = {
                viewModel.printReceipt()
                showBillPreview = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemTile(
    modifier: Modifier = Modifier,
    item: Item,
    cartItem: CartItem?,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    var showQuantityDialog by remember { mutableStateOf(false) }

    val tileShape = RoundedCornerShape(14.dp)
    val containerColor by animateColorAsState(
        targetValue = if (cartItem != null) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 120)
    )

    EnhancedCard(
        onClick = { 
            if (cartItem == null) {
                onAdd()
            } else {
                onUpdateQuantity(cartItem.quantity + 1)
            }
        },
        modifier = modifier
            .aspectRatio(0.9f)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Item Name
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Price
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                            val priceText = remember(item.price) { currencyFormatter.format(item.price) }
                            Text(
                                text = priceText,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
            }
            
            // Quantity Controls or Add Button
            if (cartItem != null) {
                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.quantity - 1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Filled.Remove,
                            "Decrease",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.clickable { showQuantityDialog = true }
                    ) {
                        Text(
                            text = cartItem.quantity.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { onUpdateQuantity(cartItem.quantity + 1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            "Add More",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Add to Cart Button
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add to Cart",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Manual Quantity Input Dialog
    if (showQuantityDialog) {
        ManualQuantityDialog(
            itemName = item.name,
            currentQuantity = cartItem?.quantity ?: 0,
            onDismiss = { showQuantityDialog = false },
            onConfirm = { quantity ->
                onUpdateQuantity(quantity)
                showQuantityDialog = false
            }
        )
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    onRemove: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = cartItem.item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                        val unitPriceText = remember(cartItem.item.price, cartItem.quantity) { "${currencyFormatter.format(cartItem.item.price)} × ${cartItem.quantity}" }
                        Text(
                            text = unitPriceText,
                            style = MaterialTheme.typography.bodyMedium
                        )
            }
            val lineTotalText = remember(cartItem.lineTotal) { currencyFormatter.format(cartItem.lineTotal) }
            Text(
                text = lineTotalText,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DiscountDialog(
    currentAmount: Double,
    currentPercent: Double,
    onDismiss: () -> Unit,
    onApplyAmount: (Double) -> Unit,
    onApplyPercent: (Double) -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (currentAmount > 0) 0 else 1) }
    var amount by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    var percent by remember { mutableStateOf(if (currentPercent > 0) currentPercent.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply Discount") },
        text = {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Amount") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Percentage") }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (selectedTab) {
                    0 -> OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    1 -> OutlinedTextField(
                        value = percent,
                        onValueChange = { percent = it },
                        label = { Text("Percentage") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (selectedTab) {
                        0 -> amount.toDoubleOrNull()?.let { onApplyAmount(it) }
                        1 -> percent.toDoubleOrNull()?.let { onApplyPercent(it) }
                    }
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
}

@Composable
private fun ManualQuantityDialog(
    itemName: String,
    currentQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var quantity by remember { mutableStateOf(currentQuantity.toString()) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Manual Quantity",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    "Item: $itemName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        quantity = it
                        isError = false
                    },
                    label = { Text("Quantity") },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Enter a valid quantity (1-999)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull()
                    if (qty != null && qty > 0 && qty <= 999) {
                        onConfirm(qty)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CartSection(
    buyerName: String,
    buyerPhone: String,
    cart: Map<Long, CartItem>,
    subtotal: Double,
    totalDiscount: Double,
    total: Double,
    onUpdateBuyerName: (String) -> Unit,
    onUpdateBuyerPhone: (String) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onUpdateQuantity: (Long, Int) -> Unit,
    onShowDiscountDialog: () -> Unit,
    onShowClearCartDialog: () -> Unit,
    onPrintReceipt: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(getResponsivePadding())
    ) {
        // Buyer Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(getResponsivePadding())
            ) {
                Text(
                    "Customer Info",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = buyerName,
                    onValueChange = onUpdateBuyerName,
                    label = { Text("Buyer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = buyerPhone,
                    onValueChange = onUpdateBuyerPhone,
                    label = { Text("Buyer Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(getResponsiveSpacing()))

        // Cart Items
        Text(
            text = "Cart (${cart.size} items)",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        if (cart.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(getResponsivePadding()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cart is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cart.values.toList()) { cartItem ->
                    CartItemRow(
                        cartItem = cartItem,
                        onRemove = { onRemoveFromCart(cartItem.item.id) },
                        onUpdateQuantity = { qty -> onUpdateQuantity(cartItem.item.id, qty) }
                    )
                }
            }
        }

        // Totals
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(getResponsivePadding())
            ) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal")
                    Text(formatPrice(subtotal))
                }
                if (totalDiscount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Discount")
                        Text("-${formatPrice(totalDiscount)}")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = formatPrice(total),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(getResponsiveSpacing()))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
        ) {
            OutlinedButton(
                onClick = onShowDiscountDialog,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Discount")
            }
            Button(
                onClick = onShowClearCartDialog,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onPrintReceipt,
            modifier = Modifier.fillMaxWidth(),
            enabled = cart.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CartHighlight
            )
        ) {
            Text(
                "Print Receipt",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun BottomActionBar(
    cart: Map<Long, CartItem>,
    subtotal: Double,
    totalDiscount: Double,
    total: Double,
    buyerName: String,
    buyerPhone: String,
    onUpdateBuyerName: (String) -> Unit,
    onUpdateBuyerPhone: (String) -> Unit,
    onShowDiscountDialog: () -> Unit,
    onShowClearCartDialog: () -> Unit,
    onShowBillPreview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(getResponsivePadding())
    ) {
        // Customer Info (Compact)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
        ) {
            OutlinedTextField(
                value = buyerName,
                onValueChange = onUpdateBuyerName,
                label = { Text("Customer Name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = buyerPhone,
                onValueChange = onUpdateBuyerPhone,
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(getResponsiveSpacing()))
        
        // Cart Summary with enhanced styling
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(getResponsivePadding()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Items: ${cart.size}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatPrice(total),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
                ) {
                    OutlinedButton(
                        onClick = onShowDiscountDialog,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Discount",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    EnhancedButton(
                        onClick = onShowClearCartDialog,
                        variant = ButtonVariant.Danger,
                        size = ButtonSize.Large
                    ) {
                        Text("Clear")
                    }
                    EnhancedButton(
                        onClick = onShowBillPreview,
                        enabled = cart.isNotEmpty(),
                        variant = ButtonVariant.Success,
                        size = ButtonSize.Large
                    ) {
                        Text("Preview")
                    }
                }
            }
        }
    }
}

@Composable
private fun BillPreviewDialog(
    cart: Map<Long, CartItem>,
    subtotal: Double,
    totalDiscount: Double,
    total: Double,
    buyerName: String,
    buyerPhone: String,
    onDismiss: () -> Unit,
    onSaveOnly: () -> Unit,
    onSaveAndPrint: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Bill Preview",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                // Customer Info
                if (buyerName.isNotBlank() || buyerPhone.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(getResponsivePadding())
                        ) {
                            if (buyerName.isNotBlank()) {
                                Text(
                                    "Customer: $buyerName",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (buyerPhone.isNotBlank()) {
                                Text(
                                    "Phone: $buyerPhone",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Items List
                Text(
                    "Items:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                cart.values.forEach { cartItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${cartItem.item.name} x${cartItem.quantity}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            formatPrice(cartItem.lineTotal),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Totals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal")
                    Text(formatPrice(subtotal))
                }
                if (totalDiscount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Discount")
                        Text("-${formatPrice(totalDiscount)}")
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        formatPrice(total),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveOnly,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Save Only")
                }
                Button(
                    onClick = onSaveAndPrint,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CartHighlight
                    )
                ) {
                    Text("Save & Print")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatPrice(price: Double): String {
    return currencyFormatter.format(price)
}
package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.components.EnhancedCard
import com.example.pos.ui.components.EnhancedButton
import com.example.pos.ui.components.ButtonVariant
import com.example.pos.ui.components.ButtonSize
import com.example.pos.ui.components.StatusChip
import com.example.pos.ui.components.ChipStatus
import com.example.pos.ui.navigation.Screen
import com.example.pos.ui.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            PosTopBar(
                title = "POS System",
                onBackClick = { (context as? Activity)?.finishAffinity() },
                navIcon = androidx.compose.material.icons.Icons.Default.ExitToApp,
                navIconTint = MaterialTheme.colorScheme.error,
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        var showContent by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            showContent = true
        }
        val heroEnter = fadeIn(tween(durationMillis = 250)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 250),
                initialOffsetY = { it / 6 }
            )
        val actionsEnter = fadeIn(tween(durationMillis = 280, delayMillis = 80)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 280, delayMillis = 80),
                initialOffsetY = { it / 6 }
            )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    )
                ),
            contentPadding = PaddingValues(getResponsivePadding()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = heroEnter
                ) {
                    HeroSection()
                }
            }
            
            // Quick Actions Section
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = actionsEnter
                ) {
                    QuickActionsSection(onNavigate = onNavigate)
                }
            }
        }
    }
}

// Hero Section - Elegant Landing Header
@Composable
private fun HeroSection() {
    EnhancedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "POS System - City Samosa, Idar",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Offline billing and local sales tracking",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            StatusChip(
                text = "Status: Ready...",
                status = ChipStatus.Success
            )
        }
    }
}

// Quick Actions Section - Main Navigation
@Composable
private fun QuickActionsSection(onNavigate: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (isTablet()) {
            // Tablet: 2x2 grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Inventory",
                        subtitle = "Manage Products",
                        icon = Icons.AutoMirrored.Filled.List,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigate(Screen.Inventory.route) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Billing",
                        subtitle = "Process Sales",
                        icon = Icons.Default.ShoppingCart,
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { onNavigate(Screen.Billing.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Dashboard",
                        subtitle = "View Analytics",
                        icon = Icons.Default.BarChart,
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { onNavigate(Screen.Dashboard.route) },
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Transactions",
                        subtitle = "Sales History",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onNavigate(Screen.Transactions.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Mobile: Vertical list
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    title = "Inventory",
                    subtitle = "Manage Products",
                    icon = Icons.AutoMirrored.Filled.List,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(Screen.Inventory.route) }
                )
                ActionCard(
                    title = "Billing",
                    subtitle = "Process Sales",
                    icon = Icons.Default.ShoppingCart,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onNavigate(Screen.Billing.route) }
                )
                ActionCard(
                    title = "Dashboard",
                    subtitle = "View Analytics",
                    icon = Icons.Default.BarChart,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onNavigate(Screen.Dashboard.route) }
                )
                ActionCard(
                    title = "Transactions",
                    subtitle = "Sales History",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigate(Screen.Transactions.route) }
                )
            }
        }
    }
}


// Action Card Component
@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EnhancedCard(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .then(
                if (isTablet()) {
                    Modifier.aspectRatio(1f)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .height(getResponsiveButtonHeight())
                }
            )
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = getResponsiveCardElevation(),
            pressedElevation = getResponsiveCardElevation() + 4.dp
        ),
        border = BorderStroke(
            2.dp, 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(getResponsivePadding()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with background circle
                Box(
                    modifier = Modifier
                        .size(getResponsiveIconSize() + 16.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                        modifier = Modifier.size(getResponsiveIconSize()),
                        tint = MaterialTheme.colorScheme.primary
            )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
            Text(
                text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet()) 18.sp else 16.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
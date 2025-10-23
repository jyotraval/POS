package com.example.pos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos.ui.components.PosTopBar
import com.example.pos.ui.navigation.Screen
import com.example.pos.ui.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            PosTopBar(
                title = "POS System",
                actions = {
                    IconButton(onClick = { onNavigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(getResponsivePadding()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
            ) {
                // Welcome Text
                Text(
                    text = "Welcome to POS System",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isTablet()) 32.sp else 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Choose an option to get started",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isTablet()) 18.sp else 16.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(getResponsiveSpacing()))
                
                // Menu Grid
                if (isTablet()) {
                    // Tablet: 2x2 grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
                        ) {
                            MainMenuButton(
                                text = "Inventory",
                                icon = Icons.AutoMirrored.Filled.List,
                                onClick = { onNavigate(Screen.Inventory.route) },
                                modifier = Modifier.weight(1f)
                            )
                            MainMenuButton(
                                text = "Billing",
                                icon = Icons.Default.ShoppingCart,
                                onClick = { onNavigate(Screen.Billing.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
                        ) {
                            MainMenuButton(
                                text = "Dashboard",
                                icon = Icons.Default.BarChart,
                                onClick = { onNavigate(Screen.Dashboard.route) },
                                modifier = Modifier.weight(1f)
                            )
                            MainMenuButton(
                                text = "Transactions",
                                icon = Icons.Default.Receipt,
                                onClick = { onNavigate(Screen.Transactions.route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // Mobile: Vertical list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(getResponsiveSpacing())
                    ) {
                        MainMenuButton(
                            text = "Inventory",
                            icon = Icons.AutoMirrored.Filled.List,
                            onClick = { onNavigate(Screen.Inventory.route) }
                        )
                        MainMenuButton(
                            text = "Billing",
                            icon = Icons.Default.ShoppingCart,
                            onClick = { onNavigate(Screen.Billing.route) }
                        )
                        MainMenuButton(
                            text = "Dashboard",
                            icon = Icons.Default.BarChart,
                            onClick = { onNavigate(Screen.Dashboard.route) }
                        )
                        MainMenuButton(
                            text = "Transactions",
                            icon = Icons.Default.Receipt,
                            onClick = { onNavigate(Screen.Transactions.route) }
                        )
                    }
                }
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
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = getResponsiveCardElevation()),
        border = BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(getResponsivePadding()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(getResponsiveIconSize()),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
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
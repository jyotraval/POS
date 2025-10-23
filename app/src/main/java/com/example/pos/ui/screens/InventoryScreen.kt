package com.example.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.pos.data.dao.CategoryDao
import com.example.pos.data.dao.ItemDao
import com.example.pos.ui.components.PosTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    categoryDao: CategoryDao,
    itemDao: ItemDao,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Categories", "Items")

    Scaffold(
        topBar = {
            Column {
                PosTopBar(
                    title = "Inventory",
                    onBackClick = onNavigateBack
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> CategoryListScreen(
                    categoryDao = categoryDao,
                    onNavigateBack = { selectedTab = 1 }
                )
                1 -> ItemListScreen(
                    categoryDao = categoryDao,
                    itemDao = itemDao,
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }
}
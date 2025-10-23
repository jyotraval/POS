package com.example.pos.ui.navigation

sealed class Screen(val route: String) {
    object Lock : Screen("lock")
    object Main : Screen("main")
    object Inventory : Screen("inventory")
    object Billing : Screen("billing")
    object Dashboard : Screen("dashboard")
    object Transactions : Screen("transactions")
    object Settings : Screen("settings")
    
    // Nested routes
    object CategoryList : Screen("inventory/categories")
    object ItemList : Screen("inventory/items")
    object TransactionDetail : Screen("transactions/{transactionId}") {
        fun createRoute(transactionId: Long) = "transactions/$transactionId"
    }
}
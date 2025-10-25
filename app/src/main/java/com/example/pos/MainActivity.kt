package com.example.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pos.ui.navigation.Screen
import com.example.pos.ui.screens.*
import com.example.pos.ui.theme.POSTheme
import com.example.pos.ui.components.ErrorMessage

class MainActivity : ComponentActivity() {
    private val database by lazy { (application as PosApplication).database }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            POSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isUnlocked by remember { mutableStateOf(false) }
                    val navController = rememberNavController()

                    if (!isUnlocked) {
                        LockScreen(
                            settingsDao = database.settingsDao(),
                            onUnlock = { isUnlocked = true }
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Main.route
                        ) {
                            composable(Screen.Main.route) {
                                MainScreen(
                                    onNavigate = { route -> navController.navigate(route) }
                                )
                            }
                            
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    settingsDao = database.settingsDao(),
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Inventory.route) {
                                InventoryScreen(
                                    categoryDao = database.categoryDao(),
                                    itemDao = database.itemDao(),
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Billing.route) {
                                BillingScreen(
                                    context = this@MainActivity,
                                    categoryDao = database.categoryDao(),
                                    itemDao = database.itemDao(),
                                    transactionDao = database.transactionDao(),
                                    settingsDao = database.settingsDao(),
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Transactions.route) {
                                TransactionsScreen(
                                    context = this@MainActivity,
                                    transactionDao = database.transactionDao(),
                                    itemDao = database.itemDao(),
                                    settingsDao = database.settingsDao(),
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    transactionDao = database.transactionDao(),
                                    itemDao = database.itemDao(),
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            // Other screen composables will be added here
                        }
                    }
                }
            }
        }
    }
}
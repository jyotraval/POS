package com.example.pos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.pos.data.dao.SettingsDao
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    settingsDao: SettingsDao,
    onUnlock: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val settings by settingsDao.getSettings().collectAsState(initial = null)
    var showCard by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showCard = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = showCard,
            enter = fadeIn(tween(durationMillis = 220)) +
                scaleIn(initialScale = 0.98f, animationSpec = tween(durationMillis = 220))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "POS System",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Enter PIN to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4) {
                                pin = it
                                error = null
                            }
                        },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = error != null,
                        supportingText = error?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                if (settings == null) {
                                    if (pin == "1111") {
                                        onUnlock()
                                    } else {
                                        error = "Invalid PIN"
                                    }
                                } else {
                                    if (pin == settings?.pin) {
                                        onUnlock()
                                    } else {
                                        error = "Invalid PIN"
                                    }
                                }
                            }
                        },
                        enabled = pin.length == 4,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock")
                    }
                }
            }
        }
    }
}
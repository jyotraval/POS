package com.example.pos.ui.screens

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Enter PIN",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
            supportingText = error?.let { { Text(it) } }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
            enabled = pin.length == 4
        ) {
            Text("Unlock")
        }
    }
}
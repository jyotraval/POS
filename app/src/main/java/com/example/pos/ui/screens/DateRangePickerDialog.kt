package com.example.pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime, LocalDateTime) -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusMonths(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Start Date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    DatePicker(
                        state = startDatePickerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        showModeToggle = false
                    )
                }
                
                Column {
                    Text(
                        text = "End Date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    DatePicker(
                        state = endDatePickerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        showModeToggle = false
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startInstant = Instant.ofEpochMilli(startDatePickerState.selectedDateMillis ?: return@TextButton)
                    val endInstant = Instant.ofEpochMilli(endDatePickerState.selectedDateMillis ?: return@TextButton)
                    val start = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault())
                    val end = LocalDateTime.ofInstant(endInstant, ZoneId.systemDefault())
                    onConfirm(start.with(LocalTime.MIN), end.with(LocalTime.MAX))
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
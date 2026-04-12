package com.example.graymatter.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.graymatter.android.ui.theme.GrayMatterColors

@Composable
fun RenameTopicDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newTopicName by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Topic", color = Color.White) },
        text = {
            OutlinedTextField(
                value = newTopicName,
                onValueChange = { newTopicName = it },
                label = { Text("Topic Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = GrayMatterColors.Primary,
                    focusedLabelColor = GrayMatterColors.Primary,
                    unfocusedLabelColor = GrayMatterColors.Neutral500
                )
            )
        },
        containerColor = Color(0xFF1A1A1E),
        confirmButton = {
            TextButton(
                onClick = {
                    if (newTopicName.isNotBlank()) {
                        onConfirm(newTopicName)
                    }
                }
            ) {
                Text("Rename", color = GrayMatterColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

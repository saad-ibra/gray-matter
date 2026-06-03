package com.example.graymatter.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.theme.GrayMatterTheme

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
                    cursorColor = GrayMatterTheme.colors.primary,
                    focusedLabelColor = GrayMatterTheme.colors.primary,
                    unfocusedLabelColor = GrayMatterTheme.colors.neutral500
                )
            )
        },
        containerColor = GrayMatterTheme.colors.neutral800,
        confirmButton = {
            TextButton(
                onClick = {
                    if (newTopicName.isNotBlank()) {
                        onConfirm(newTopicName)
                    }
                }
            ) {
                Text("Rename", color = GrayMatterTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        }
    )
}

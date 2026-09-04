package com.example.graymatter.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.graymatter.android.ui.theme.GrayMatterTheme

enum class SortOption(val label: String) {
    CUSTOM("Custom Order"),
    DATE_MODIFIED("Date Modified"),
    ALPHABETICAL_ASC("Alphabetical (A-Z)"),
    ALPHABETICAL_DESC("Alphabetical (Z-A)")
}

enum class GroupOption(val label: String) {
    NONE("None"),
    DATE_MODIFIED("By Date")
}

@Composable
fun SortAndGroupDialog(
    onDismissRequest: () -> Unit,
    currentSortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    currentGroupOption: GroupOption,
    onGroupOptionSelected: (GroupOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Sort & Group",
                style = MaterialTheme.typography.titleMedium,
                color = GrayMatterTheme.colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Sort By",
                    style = MaterialTheme.typography.labelLarge,
                    color = GrayMatterTheme.colors.primary
                )
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOptionSelected(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentSortOption == option),
                            onClick = { onSortOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = GrayMatterTheme.colors.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayMatterTheme.colors.textPrimary
                        )
                    }
                }

                HorizontalDivider(color = GrayMatterTheme.colors.neutral800)

                Text(
                    text = "Group By",
                    style = MaterialTheme.typography.labelLarge,
                    color = GrayMatterTheme.colors.primary
                )
                GroupOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGroupOptionSelected(option) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentGroupOption == option),
                            onClick = { onGroupOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = GrayMatterTheme.colors.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrayMatterTheme.colors.textPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Done", color = GrayMatterTheme.colors.primary)
            }
        },
        containerColor = GrayMatterTheme.colors.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

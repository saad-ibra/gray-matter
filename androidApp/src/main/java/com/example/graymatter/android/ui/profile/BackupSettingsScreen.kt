package com.example.graymatter.android.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.backup.BackupFrequency
import com.example.graymatter.android.backup.BackupInfo
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.BackupUiState
import com.example.graymatter.android.ui.viewmodel.BackupViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: BackupViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<BackupInfo?>(null) }
    var exportTarget by remember { mutableStateOf<BackupInfo?>(null) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val restorePickerWithUri = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreDialog = true
        }
    }

    val exportSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null && exportTarget != null) {
            viewModel.exportBackup(exportTarget!!.file, uri)
            exportTarget = null
        }
    }

    // Snackbar for status messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = GrayMatterColors.BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Backup, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(24.dp))
                        Text("Backup", color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = GrayMatterColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GrayMatterColors.BackgroundDark)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Info Banner ──
            if (!state.isPasswordSet) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrayMatterColors.Gamboge.copy(alpha = 0.12f))
                            .border(1.dp, GrayMatterColors.Gamboge.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = GrayMatterColors.Gamboge, modifier = Modifier.size(20.dp))
                        Text(
                            "Set master password in Security → Data Encryption to start backups",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayMatterColors.Gamboge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Backup Toggle ──
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Automatic Backups", style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (state.isBackupEnabled) "Backups are scheduled" else "Backups are disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayMatterColors.Neutral500
                            )
                        }
                        Switch(
                            checked = state.isBackupEnabled,
                            onCheckedChange = { viewModel.setBackupEnabled(it) },
                            enabled = state.isPasswordSet,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GrayMatterColors.Primary,
                                checkedTrackColor = GrayMatterColors.Primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = GrayMatterColors.Neutral500,
                                uncheckedTrackColor = GrayMatterColors.Neutral800
                            )
                        )
                    }
                }
            }

            if (state.isBackupEnabled) {
                // ── Schedule Section ──
                item {
                    SectionHeader(icon = Icons.Default.Schedule, title = "BACKUP SCHEDULE")
                }
                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Frequency", style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BackupFrequency.entries.forEach { freq ->
                                        FilterChip(
                                            selected = state.frequency == freq,
                                            onClick = { viewModel.setFrequency(freq) },
                                            label = { Text(freq.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = GrayMatterColors.Primary.copy(alpha = 0.2f),
                                                selectedLabelColor = GrayMatterColors.Primary
                                            )
                                        )
                                    }
                                }
                            }

                            Divider(color = GrayMatterColors.Neutral800, thickness = 0.5.dp)

                            var showTimePicker by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Time of day", style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.SemiBold)

                                val timeStr = if (state.is24HourFormat) {
                                    String.format("%02d:%02d", state.backupTimeHour, state.backupTimeMinute)
                                } else {
                                    val hour = if (state.backupTimeHour % 12 == 0) 12 else state.backupTimeHour % 12
                                    val amPm = if (state.backupTimeHour < 12) "AM" else "PM"
                                    String.format("%02d:%02d %s", hour, state.backupTimeMinute, amPm)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GrayMatterColors.Neutral900)
                                        .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
                                        .clickable { showTimePicker = true }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        timeStr,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = GrayMatterColors.Primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (showTimePicker) {
                                TimePickerDialog(
                                    initialHour = state.backupTimeHour,
                                    initialMinute = state.backupTimeMinute,
                                    is24Hour = state.is24HourFormat,
                                    onFormatToggle = { viewModel.set24HourFormat(it) },
                                    onDismiss = { showTimePicker = false },
                                    onConfirm = { hour, minute ->
                                        viewModel.setBackupTime(hour, minute)
                                        showTimePicker = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Retention Section ──
            item {
                SettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Keep Backups", style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            
                            var backupCountStr by remember(state.maxBackups) { mutableStateOf(state.maxBackups.toString()) }
                            var errorMsg by remember { mutableStateOf<String?>(null) }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                OutlinedTextField(
                                    value = backupCountStr,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() }) {
                                            backupCountStr = newValue
                                            val num = newValue.toIntOrNull()
                                            if (num != null) {
                                                if (num > 30) {
                                                    errorMsg = "Max 30"
                                                } else if (num < 1) {
                                                    errorMsg = "Min 1"
                                                } else {
                                                    errorMsg = null
                                                    viewModel.setMaxBackups(num)
                                                }
                                            } else if (newValue.isEmpty()) {
                                                errorMsg = "Required"
                                            }
                                        }
                                    },
                                    isError = errorMsg != null,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp).height(50.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = GrayMatterColors.TextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GrayMatterColors.Primary,
                                        unfocusedBorderColor = GrayMatterColors.Neutral700,
                                        errorBorderColor = MaterialTheme.colorScheme.error,
                                        cursorColor = GrayMatterColors.Primary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                errorMsg?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }


            // ── Actions ──
            item {
                SectionHeader(icon = Icons.Default.Bolt, title = "ACTIONS")
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.triggerManualBackup() },
                        enabled = state.isPasswordSet && !state.isBackingUp,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrayMatterColors.Primary,
                            contentColor = GrayMatterColors.OnPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.isBackingUp) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GrayMatterColors.OnPrimary)
                        } else {
                            Icon(Icons.Default.Backup, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.isBackingUp) "Backing up..." else "Backup Now")
                    }
                    OutlinedButton(
                        onClick = {
                            restorePickerWithUri.launch(arrayOf("*/*"))
                        },
                        enabled = !state.isRestoring,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GrayMatterColors.Primary)
                    ) {
                        if (state.isRestoring) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GrayMatterColors.Primary)
                        } else {
                            Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.isRestoring) "Restoring..." else "Restore")
                    }
                }
            }

            // ── Local Backups List ──
            if (state.localBackups.isNotEmpty()) {
                item {
                    SectionHeader(icon = Icons.Default.Folder, title = "LOCAL BACKUPS (${state.localBackups.size})")
                }
                items(state.localBackups) { backup ->
                    BackupListItem(
                        backup = backup,
                        onExport = {
                            exportTarget = backup
                            exportSafLauncher.launch(backup.name)
                        },
                        onDelete = { showDeleteConfirm = backup }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    // ── Dialogs ──

    if (showRestoreDialog && pendingRestoreUri != null) {
        RestoreConfirmDialog(
            onDismiss = {
                showRestoreDialog = false
                pendingRestoreUri = null
            },
            onConfirm = { password ->
                pendingRestoreUri?.let { uri ->
                    viewModel.restoreFromBackup(uri, password)
                }
                showRestoreDialog = false
                pendingRestoreUri = null
            }
        )
    }

    showDeleteConfirm?.let { backup ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = GrayMatterColors.SurfaceDark,
            title = { Text("Delete Backup?", color = GrayMatterColors.TextPrimary) },
            text = { Text("This will permanently delete ${backup.name}.", color = GrayMatterColors.Neutral400) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBackup(backup.file)
                    showDeleteConfirm = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel", color = GrayMatterColors.Neutral500) }
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, null, tint = GrayMatterColors.Neutral500, modifier = Modifier.size(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold),
            color = GrayMatterColors.Neutral500
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun BackupListItem(
    backup: BackupInfo,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GrayMatterColors.SurfaceDark)
            .border(1.dp, GrayMatterColors.Neutral800, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                formatDate(backup.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = GrayMatterColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                formatSize(backup.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = GrayMatterColors.Neutral500
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Upload, "Export", tint = GrayMatterColors.Primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = GrayMatterColors.Neutral500, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun RestoreConfirmDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrayMatterColors.SurfaceDark,
        icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
        title = { Text("Restore Backup?", color = GrayMatterColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This will replace ALL current data with the backup. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayMatterColors.Neutral400
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Backup Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GrayMatterColors.Primary,
                        unfocusedBorderColor = GrayMatterColors.Neutral700,
                        focusedTextColor = GrayMatterColors.TextPrimary,
                        unfocusedTextColor = GrayMatterColors.TextPrimary,
                        cursorColor = GrayMatterColors.Primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (password.isNotEmpty()) onConfirm(password) },
                enabled = password.isNotEmpty()
            ) { Text("Restore", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = GrayMatterColors.Neutral500) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onFormatToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GrayMatterColors.SurfaceDark,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select Time",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrayMatterColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = is24Hour,
                            onClick = { onFormatToggle(true) },
                            label = { Text("24h") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GrayMatterColors.Primary.copy(alpha = 0.2f),
                                selectedLabelColor = GrayMatterColors.Primary
                            )
                        )
                        FilterChip(
                            selected = !is24Hour,
                            onClick = { onFormatToggle(false) },
                            label = { Text("12h") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GrayMatterColors.Primary.copy(alpha = 0.2f),
                                selectedLabelColor = GrayMatterColors.Primary
                            )
                        )
                    }
                }
                
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = GrayMatterColors.Neutral900,
                        clockDialSelectedContentColor = GrayMatterColors.OnPrimary,
                        clockDialUnselectedContentColor = GrayMatterColors.TextPrimary,
                        selectorColor = GrayMatterColors.Primary,
                        periodSelectorBorderColor = GrayMatterColors.Neutral700,
                        periodSelectorSelectedContainerColor = GrayMatterColors.Primary.copy(alpha = 0.2f),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = GrayMatterColors.Primary,
                        periodSelectorUnselectedContentColor = GrayMatterColors.Neutral500,
                        timeSelectorSelectedContainerColor = GrayMatterColors.Primary.copy(alpha = 0.2f),
                        timeSelectorUnselectedContainerColor = GrayMatterColors.Neutral900,
                        timeSelectorSelectedContentColor = GrayMatterColors.Primary,
                        timeSelectorUnselectedContentColor = GrayMatterColors.TextPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = GrayMatterColors.Neutral400)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(timePickerState.hour, timePickerState.minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

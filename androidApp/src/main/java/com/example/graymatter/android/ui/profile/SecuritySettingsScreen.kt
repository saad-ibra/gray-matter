package com.example.graymatter.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.graymatter.android.ui.theme.GrayMatterColors
import com.example.graymatter.android.ui.viewmodel.SecurityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: SecurityViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showPasswordDialog by remember { mutableStateOf(false) }

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
                        Icon(Icons.Outlined.Security, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(24.dp))
                        Text("Security", color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.Bold)
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
            // ── Device Protection ──
            item {
                SectionHeader(icon = Icons.Default.PhonelinkLock, title = "DEVICE PROTECTION")
            }
            item {
                SettingsCard {
                    SecurityToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "App Lock",
                        subtitle = "Require biometric or device credential to open the app",
                        checked = state.isAppLockEnabled,
                        onCheckedChange = { viewModel.setAppLockEnabled(it) }
                    )
                    
                    Divider(color = GrayMatterColors.Neutral800, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))
                    
                    SecurityToggleItem(
                        icon = Icons.Default.VisibilityOff,
                        title = "Screen Security",
                        subtitle = "Hide app content in recents and block screenshots",
                        checked = state.isScreenSecurityEnabled,
                        onCheckedChange = { viewModel.setScreenSecurityEnabled(it) }
                    )
                }
            }

            // ── Data Encryption ──
            item {
                SectionHeader(icon = Icons.Default.EnhancedEncryption, title = "DATA ENCRYPTION")
            }
            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Master Password", style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (state.isMasterPasswordSet) "Used to encrypt portable backups" else "Required for creating backups",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isMasterPasswordSet) GrayMatterColors.Primary else GrayMatterColors.Neutral500
                            )
                        }
                        FilledTonalButton(
                            onClick = { showPasswordDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = GrayMatterColors.Primary.copy(alpha = 0.15f),
                                contentColor = GrayMatterColors.Primary
                            )
                        ) {
                            Text(if (state.isMasterPasswordSet) "Change" else "Set")
                        }
                    }
                    
                    Divider(color = GrayMatterColors.Neutral800, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 16.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.VerifiedUser, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Vault Encryption", style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Hardware-backed security", style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.Neutral500)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (showPasswordDialog) {
        PasswordDialog(
            isPasswordSet = state.isMasterPasswordSet,
            onVerifyOldPassword = { oldPassword -> viewModel.verifyMasterPassword(oldPassword) },
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                viewModel.setMasterPassword(password)
                showPasswordDialog = false
            }
        )
    }
}

@Composable
private fun SecurityToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, null, tint = GrayMatterColors.Neutral400, modifier = Modifier.size(24.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = GrayMatterColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GrayMatterColors.Neutral500)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GrayMatterColors.OnPrimary,
                checkedTrackColor = GrayMatterColors.Primary,
                uncheckedThumbColor = GrayMatterColors.Neutral500,
                uncheckedTrackColor = GrayMatterColors.Neutral800
            )
        )
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
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
private fun PasswordDialog(
    isPasswordSet: Boolean,
    onVerifyOldPassword: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var oldPasswordError by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = GrayMatterColors.SurfaceDark,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(GrayMatterColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, null, tint = GrayMatterColors.Primary, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = if (isPasswordSet) "Change Password" else "Set Master Password",
                        style = MaterialTheme.typography.titleLarge,
                        color = GrayMatterColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This encrypts your backups. If forgotten, backups cannot be recovered.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayMatterColors.Neutral400,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Input Fields
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (isPasswordSet) {
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it; oldPasswordError = null },
                            label = { Text("Current Password") },
                            visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showOldPassword = !showOldPassword }) {
                                    Icon(
                                        if (showOldPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        null,
                                        tint = GrayMatterColors.Neutral500
                                    )
                                }
                            },
                            singleLine = true,
                            isError = oldPasswordError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GrayMatterColors.Primary,
                                unfocusedBorderColor = GrayMatterColors.Neutral700,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                focusedTextColor = GrayMatterColors.TextPrimary,
                                unfocusedTextColor = GrayMatterColors.TextPrimary,
                                cursorColor = GrayMatterColors.Primary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        oldPasswordError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text(if (isPasswordSet) "New Password" else "Password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = GrayMatterColors.Neutral500
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrayMatterColors.Primary,
                            unfocusedBorderColor = GrayMatterColors.Neutral700,
                            focusedTextColor = GrayMatterColors.TextPrimary,
                            unfocusedTextColor = GrayMatterColors.TextPrimary,
                            cursorColor = GrayMatterColors.Primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = null },
                        label = { Text("Confirm Password") },
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = GrayMatterColors.Neutral500
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GrayMatterColors.Primary,
                            unfocusedBorderColor = GrayMatterColors.Neutral700,
                            focusedTextColor = GrayMatterColors.TextPrimary,
                            unfocusedTextColor = GrayMatterColors.TextPrimary,
                            cursorColor = GrayMatterColors.Primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = GrayMatterColors.Neutral400)
                    }
                    Button(
                        onClick = {
                            if (isPasswordSet && !onVerifyOldPassword(oldPassword)) {
                                oldPasswordError = "Incorrect current password"
                                return@Button
                            }
                            
                            when {
                                password.length < 6 -> error = "Minimum 6 characters"
                                password != confirmPassword -> error = "Passwords don't match"
                                else -> onConfirm(password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GrayMatterColors.Primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

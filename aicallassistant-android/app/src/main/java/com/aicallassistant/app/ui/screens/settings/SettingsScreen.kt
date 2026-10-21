package com.aicallassistant.app.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.ui.components.UserProfileAvatar
import com.aicallassistant.app.ui.screens.home.AssistantStatusToggleCard
import com.aicallassistant.app.ui.screens.home.ReferralBannerCard
import com.aicallassistant.app.ui.theme.EqualDarkBg
import com.aicallassistant.app.ui.theme.EqualDarkBorder
import com.aicallassistant.app.ui.theme.EqualDarkCard
import com.aicallassistant.app.ui.theme.EqualDarkSurface
import com.aicallassistant.app.ui.theme.EqualGreen
import com.aicallassistant.app.ui.theme.EqualGreenDim
import com.aicallassistant.app.ui.theme.EqualGreenText
import com.aicallassistant.app.ui.theme.EqualTextPrimary
import com.aicallassistant.app.ui.theme.EqualTextSecondary
import com.aicallassistant.app.ui.theme.TealPrimary
import com.aicallassistant.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var backendUrlInput by remember(uiState.currentBackendUrl) { mutableStateOf(uiState.currentBackendUrl) }
    var showInstructionsDialog by remember { mutableStateOf(false) }
    var instructionInput by remember(uiState.customInstructions) { mutableStateOf(uiState.customInstructions) }

    var showVoiceDialog by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var addressInput by remember { mutableStateOf("") }
    var showBackendDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showHealthDialog by remember { mutableStateOf(false) }

    // Dialog: Instructions
    if (showInstructionsDialog) {
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = { Text("Your Instructions", fontWeight = FontWeight.Bold, color = EqualTextPrimary) },
            text = {
                Column {
                    Text(
                        "Tell Equal Assistant how to introduce itself, screen unknown calls, or answer questions:",
                        fontSize = 12.sp,
                        color = EqualTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = instructionInput,
                        onValueChange = { instructionInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EqualTextPrimary,
                            unfocusedTextColor = EqualTextPrimary,
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = EqualDarkBorder,
                            focusedContainerColor = EqualDarkSurface,
                            unfocusedContainerColor = EqualDarkSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateInstructions(instructionInput)
                        showInstructionsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EqualGreen, contentColor = Color(0xFF0F141C))
                ) {
                    Text("Save Instructions", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstructionsDialog = false }) {
                    Text("Cancel", color = EqualTextSecondary)
                }
            },
            containerColor = EqualDarkCard
        )
    }

    // Dialog: Voice & Language
    if (showVoiceDialog) {
        val voices = listOf(
            "Aoede (Warm & Professional)",
            "Puck (Casual & Friendly)",
            "Charon (Deep & Direct)",
            "Kore (Polite & Calm)",
            "Fenrir (Confident & Energetic)"
        )
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text("Voice & Language", fontWeight = FontWeight.Bold, color = EqualTextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select how Equal Assistant speaks aloud:", fontSize = 12.sp, color = EqualTextSecondary)
                    voices.forEach { v ->
                        val isSelected = uiState.selectedVoice == v
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateVoice(v)
                                    showVoiceDialog = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) TealPrimary.copy(alpha = 0.2f) else EqualDarkSurface
                        ) {
                            Text(
                                text = v,
                                color = if (isSelected) TealPrimary else EqualTextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Done", color = TealPrimary)
                }
            },
            containerColor = EqualDarkCard
        )
    }

    // Dialog: Addresses
    if (showAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Your Addresses", fontWeight = FontWeight.Bold, color = EqualTextPrimary) },
            text = {
                Column {
                    Text(
                        "Add locations your assistant can reference for deliveries or meeting directions:",
                        fontSize = 12.sp,
                        color = EqualTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    uiState.addresses.forEach { addr ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = EqualDarkSurface
                        ) {
                            Text(
                                text = "📍 $addr",
                                fontSize = 12.sp,
                                color = EqualTextPrimary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        placeholder = { Text("e.g. Vacation Home: Goa", color = EqualTextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EqualTextPrimary,
                            unfocusedTextColor = EqualTextPrimary,
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = EqualDarkBorder,
                            focusedContainerColor = EqualDarkSurface,
                            unfocusedContainerColor = EqualDarkSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addressInput.isNotBlank()) {
                            viewModel.addAddress(addressInput)
                            addressInput = ""
                        }
                        showAddressDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EqualGreen, contentColor = Color(0xFF0F141C))
                ) {
                    Text("Add Address", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("Close", color = EqualTextSecondary)
                }
            },
            containerColor = EqualDarkCard
        )
    }

    // Dialog: Backend URL Connection
    if (showBackendDialog) {
        AlertDialog(
            onDismissRequest = { showBackendDialog = false },
            title = { Text("Backend Server Configuration", fontWeight = FontWeight.Bold, color = EqualTextPrimary) },
            text = {
                Column {
                    Text(
                        "Set the IP or domain where the FastAPI voice server is hosted:",
                        fontSize = 12.sp,
                        color = EqualTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EqualTextPrimary,
                            unfocusedTextColor = EqualTextPrimary,
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = EqualDarkBorder,
                            focusedContainerColor = EqualDarkSurface,
                            unfocusedContainerColor = EqualDarkSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBackendUrl(backendUrlInput.trim())
                        showBackendDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color(0xFF0F141C))
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.resetBackendUrlToDefault()
                        backendUrlInput = uiState.currentBackendUrl
                        showBackendDialog = false
                    }
                ) {
                    Text("Reset Default", color = Color(0xFFF87171))
                }
            },
            containerColor = EqualDarkCard
        )
    }

    // Dialog: Health Check
    if (showHealthDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDialog = false },
            title = { Text("Assistant Health Check", fontWeight = FontWeight.Bold, color = EqualTextPrimary) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (uiState.isCheckingHealth) {
                        CircularProgressIndicator(color = TealPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Pinging assistant backend & Gemini Live...", color = EqualTextSecondary, fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = EqualGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.healthCheckStatus ?: "Ready to run connectivity check.",
                            fontSize = 13.sp,
                            color = EqualTextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.runHealthCheck() },
                    colors = ButtonDefaults.buttonColors(containerColor = EqualGreen, contentColor = Color(0xFF0F141C))
                ) {
                    Text("Run Ping Test", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHealthDialog = false }) {
                    Text("Close", color = EqualTextSecondary)
                }
            },
            containerColor = EqualDarkCard
        )
    }

    // Dialog: Sign Out
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold, color = EqualTextPrimary) },
            text = { Text("Are you sure you want to sign out of CallMate?", color = EqualTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut(onSignedOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = EqualTextSecondary)
                }
            },
            containerColor = EqualDarkCard
        )
    }

    Scaffold(
        containerColor = EqualDarkBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(EqualDarkBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Section (Matching Screenshot 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserProfileAvatar(size = 68.dp)

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.profile?.fullName ?: "Sayyad",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = EqualTextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit name",
                            tint = EqualTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "9398645687",
                        fontSize = 14.sp,
                        color = EqualTextSecondary
                    )

                    Text(
                        text = "@sayyadmazahar",
                        fontSize = 12.sp,
                        color = TealPrimary
                    )
                }
            }

            // AI Assistant is On Toggle (Matching Screenshot 1 & 2)
            AssistantStatusToggleCard(
                isOn = uiState.isAssistantOn,
                onToggle = { viewModel.toggleAssistant(it) }
            )

            // Section 1: TELL EQUAL (Matching Screenshot 1 & 2)
            SectionHeader(title = "TELL EQUAL")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EqualDarkCard)
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Outlined.Description,
                        title = "Your Instructions",
                        subtitle = "Tell Equal what to do",
                        hasNewBadge = true,
                        onClick = { showInstructionsDialog = true }
                    )
                    HorizontalDivider(color = EqualDarkBorder)
                    SettingsActionRow(
                        icon = Icons.Outlined.MedicalServices,
                        title = "Assistant Health Check",
                        subtitle = "Check Real-Time Assistant Connectivity",
                        onClick = {
                            viewModel.runHealthCheck()
                            showHealthDialog = true
                        }
                    )
                    HorizontalDivider(color = EqualDarkBorder)
                    SettingsActionRow(
                        icon = Icons.Outlined.NotificationsOff,
                        title = "Silent Mode",
                        subtitle = "Mute unwanted calls automatically",
                        trailingContent = {
                            Switch(
                                checked = uiState.silentMode,
                                onCheckedChange = { viewModel.toggleSilentMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EqualGreen,
                                    checkedTrackColor = Color(0xFF223114)
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = EqualDarkBorder)
                    SettingsActionRow(
                        icon = Icons.Default.Mic,
                        title = "Voice & Language",
                        subtitle = "Choose how Equal speaks.",
                        onClick = { showVoiceDialog = true }
                    )
                }
            }

            // Section 2: ABOUT YOU (Matching Screenshot 1)
            SectionHeader(title = "ABOUT YOU")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EqualDarkCard)
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.LocationOn,
                        title = "Your Addresses",
                        subtitle = "Add addresses your assistant should know",
                        hasNewBadge = true,
                        onClick = { showAddressDialog = true }
                    )
                    HorizontalDivider(color = EqualDarkBorder)
                    SettingsActionRow(
                        icon = Icons.Outlined.Dns,
                        title = "Backend Server URL",
                        subtitle = uiState.currentBackendUrl,
                        onClick = { showBackendDialog = true }
                    )
                }
            }

            // Section 3: SETTINGS & SUPPORT (Matching Screenshot 1)
            SectionHeader(title = "SETTINGS & SUPPORT")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = EqualDarkCard)
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.SmartToy,
                        title = "WhatsApp Updates",
                        subtitle = "Assistant and App Updates",
                        onClick = {}
                    )
                    HorizontalDivider(color = EqualDarkBorder)
                    SettingsActionRow(
                        icon = Icons.Outlined.Shield,
                        title = "Account Data",
                        subtitle = "Manage Your Account (${uiState.profile?.email ?: "sayyad@callmate.ai"})",
                        onClick = {}
                    )
                    HorizontalDivider(color = EqualDarkBorder)
                    SettingsActionRow(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Help Center",
                        subtitle = "Your What, Why and How",
                        onClick = {}
                    )
                    HorizontalDivider(color = EqualDarkBorder)
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Sign Out",
                        subtitle = "Log out from this device",
                        titleColor = Color(0xFFEF4444),
                        onClick = { showSignOutDialog = true }
                    )
                }
            }

            // Invite a Friend Banner
            ReferralBannerCard(
                onInviteClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Experience CallMate Equal AI for your phone calls! https://callmate.ai")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Invite a Friend"))
                }
            )

            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = EqualTextSecondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    hasNewBadge: Boolean = false,
    titleColor: Color = EqualTextPrimary,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor != EqualTextPrimary) titleColor else EqualTextPrimary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )

                if (hasNewBadge) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EqualGreen
                    ) {
                        Text(
                            text = "NEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F141C),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = EqualTextSecondary
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = EqualTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

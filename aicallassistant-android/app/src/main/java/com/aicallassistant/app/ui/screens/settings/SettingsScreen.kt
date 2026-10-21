package com.aicallassistant.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.font.FontStyle
import com.aicallassistant.app.ui.components.FemaleUserProfileAvatar
import com.aicallassistant.app.ui.components.UserGenderAvatar
import com.aicallassistant.app.ui.components.UserProfileAvatar
import com.aicallassistant.app.ui.screens.home.MockupAssistantCard
import androidx.compose.foundation.BorderStroke
import com.aicallassistant.app.ui.theme.CallMateActive
import com.aicallassistant.app.ui.theme.CallMateBadgeGold
import com.aicallassistant.app.ui.theme.CallMateBorder
import com.aicallassistant.app.ui.theme.CallMateCanvas
import com.aicallassistant.app.ui.theme.CallMateCard
import com.aicallassistant.app.ui.theme.CallMateCardElevated
import com.aicallassistant.app.ui.theme.CallMateInk
import com.aicallassistant.app.ui.theme.CallMatePrimary
import com.aicallassistant.app.ui.theme.CallMateSlate
import com.aicallassistant.app.ui.theme.CallMateSurface
import com.aicallassistant.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignedOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showPersonalDetails by remember { mutableStateOf(false) }

    if (showPersonalDetails) {
        PersonalDetailsScreenContent(
            initialName = uiState.fullName,
            initialGender = uiState.gender,
            onConfirm = { name, gender ->
                viewModel.updatePersonalDetails(name, gender)
                showPersonalDetails = false
            },
            onBack = { showPersonalDetails = false }
        )
        return
    }

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
            title = { Text("Your instructions", fontWeight = FontWeight.Bold, color = CallMateInk) },
            text = {
                Column {
                    Text(
                        "Tell CallMate AI how to introduce itself, screen unknown calls, or respond to callers:",
                        fontSize = 12.sp,
                        color = CallMateSlate
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = instructionInput,
                        onValueChange = { instructionInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CallMateInk,
                            unfocusedTextColor = CallMateInk,
                            focusedBorderColor = CallMatePrimary,
                            unfocusedBorderColor = CallMateBorder,
                            focusedContainerColor = CallMateCanvas,
                            unfocusedContainerColor = CallMateCanvas
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
                    colors = ButtonDefaults.buttonColors(containerColor = CallMateActive, contentColor = Color.Black)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstructionsDialog = false }) {
                    Text("Cancel", color = CallMateSlate)
                }
            },
            containerColor = CallMateCard
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
            title = { Text("Voice and language", fontWeight = FontWeight.Bold, color = CallMateInk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how CallMate AI speaks:", fontSize = 12.sp, color = CallMateSlate)
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
                            color = if (isSelected) CallMatePrimary.copy(alpha = 0.2f) else CallMateCanvas
                        ) {
                            Text(
                                text = v,
                                color = if (isSelected) CallMatePrimary else CallMateInk,
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
                    Text("Done", color = CallMatePrimary)
                }
            },
            containerColor = CallMateCard
        )
    }

    // Dialog: Addresses
    if (showAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Your addresses", fontWeight = FontWeight.Bold, color = CallMateInk) },
            text = {
                Column {
                    Text(
                        "Add places your assistant should know (e.g., home, office, deliveries):",
                        fontSize = 12.sp,
                        color = CallMateSlate
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    uiState.addresses.forEach { addr ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = CallMateCanvas
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📍 $addr",
                                    fontSize = 12.sp,
                                    color = CallMateInk,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.removeAddress(addr) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove address",
                                        tint = CallMateSlate,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        placeholder = { Text("e.g. Home: Hyderabad", color = CallMateSlate, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CallMateInk,
                            unfocusedTextColor = CallMateInk,
                            focusedBorderColor = CallMatePrimary,
                            unfocusedBorderColor = CallMateBorder,
                            focusedContainerColor = CallMateCanvas,
                            unfocusedContainerColor = CallMateCanvas
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
                    colors = ButtonDefaults.buttonColors(containerColor = CallMateActive, contentColor = Color.Black)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("Close", color = CallMateSlate)
                }
            },
            containerColor = CallMateCard
        )
    }

    // Dialog: Backend Server URL
    if (showBackendDialog) {
        AlertDialog(
            onDismissRequest = { showBackendDialog = false },
            title = { Text("Backend server URL", fontWeight = FontWeight.Bold, color = CallMateInk) },
            text = {
                Column {
                    Text(
                        "Configure the endpoint of your FastAPI CallMate voice backend:",
                        fontSize = 12.sp,
                        color = CallMateSlate
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CallMateInk,
                            unfocusedTextColor = CallMateInk,
                            focusedBorderColor = CallMatePrimary,
                            unfocusedBorderColor = CallMateBorder,
                            focusedContainerColor = CallMateCanvas,
                            unfocusedContainerColor = CallMateCanvas
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
                    colors = ButtonDefaults.buttonColors(containerColor = CallMatePrimary, contentColor = Color.Black)
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
            containerColor = CallMateCard
        )
    }

    // Dialog: Health Check
    if (showHealthDialog) {
        AlertDialog(
            onDismissRequest = { showHealthDialog = false },
            title = { Text("Assistant health check", fontWeight = FontWeight.Bold, color = CallMateInk) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (uiState.isCheckingHealth) {
                        CircularProgressIndicator(color = CallMatePrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Pinging CallMate backend & Gemini Live...", color = CallMateSlate, fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = CallMateActive,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.healthCheckStatus ?: "Ready to check real-time connectivity.",
                            fontSize = 13.sp,
                            color = CallMateInk,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.runHealthCheck() },
                    colors = ButtonDefaults.buttonColors(containerColor = CallMateActive, contentColor = Color.Black)
                ) {
                    Text("Check Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHealthDialog = false }) {
                    Text("Close", color = CallMateSlate)
                }
            },
            containerColor = CallMateCard
        )
    }

    // Dialog: Sign Out
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out", fontWeight = FontWeight.Bold, color = CallMateInk) },
            text = { Text("Are you sure you want to sign out of CallMate AI?", color = CallMateSlate) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut(onSignedOut)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = CallMateSlate)
                }
            },
            containerColor = CallMateCard
        )
    }

    Scaffold(
        containerColor = CallMateCanvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CallMateCanvas)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Title: "You" (Matching Mockup)
            Text(
                text = "You",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = CallMateInk,
                modifier = Modifier.padding(top = 4.dp)
            )

            // User Profile Card (Matching Mockup & Clickable to edit personal details)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPersonalDetails = true },
                shape = RoundedCornerShape(18.dp),
                color = CallMateCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserGenderAvatar(
                        gender = uiState.gender,
                        size = 52.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.fullName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallMateInk
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = uiState.profile?.email ?: "9398645687",
                            fontSize = 13.sp,
                            color = CallMateSlate
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = uiState.username,
                            fontSize = 13.sp,
                            color = CallMatePrimary
                        )
                    }

                    IconButton(onClick = { showPersonalDetails = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit personal details",
                            tint = CallMateSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Assistant is on Switch Card (Matching Mockup)
            MockupAssistantCard(
                isOn = uiState.isAssistantOn,
                onToggle = { viewModel.toggleAssistant(it) }
            )

            // Section: Assistant settings (Matching Mockup)
            SectionHeaderTitle(title = "Assistant settings")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CallMateCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder)
            ) {
                Column {
                    MockupSettingRow(
                        icon = Icons.Outlined.Smartphone,
                        title = "Your instructions",
                        subtitle = "Tell CallMate AI what to do",
                        hasNewBadge = true,
                        onClick = { showInstructionsDialog = true }
                    )
                    HorizontalDivider(color = CallMateBorder)
                    MockupSettingRow(
                        icon = Icons.Outlined.MonitorHeart,
                        title = "Assistant health check",
                        subtitle = "Check real-time connectivity",
                        onClick = {
                            viewModel.runHealthCheck()
                            showHealthDialog = true
                        }
                    )
                    HorizontalDivider(color = CallMateBorder)
                    MockupSettingRow(
                        icon = Icons.Outlined.NotificationsOff,
                        title = "Silent mode",
                        subtitle = "Mute unwanted calls automatically",
                        trailingContent = {
                            Switch(
                                checked = uiState.silentMode,
                                onCheckedChange = { viewModel.toggleSilentMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CallMateActive,
                                    uncheckedThumbColor = CallMateSlate,
                                    uncheckedTrackColor = Color(0xFF1E1E1E)
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = CallMateBorder)
                    MockupSettingRow(
                        icon = Icons.Default.Mic,
                        title = "Voice and language",
                        subtitle = "Choose how CallMate AI speaks",
                        onClick = { showVoiceDialog = true }
                    )
                }
            }

            // Section: About you (Matching Mockup)
            SectionHeaderTitle(title = "About you")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CallMateCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder)
            ) {
                Column {
                    MockupSettingRow(
                        icon = Icons.Default.Place,
                        title = "Your addresses",
                        subtitle = "Add places your assistant should know",
                        hasNewBadge = true,
                        onClick = { showAddressDialog = true }
                    )
                    HorizontalDivider(color = CallMateBorder)
                    MockupSettingRow(
                        icon = Icons.Outlined.Dns,
                        title = "Backend server URL",
                        subtitle = uiState.currentBackendUrl,
                        onClick = { showBackendDialog = true }
                    )
                }
            }

            // Section: Account & Support
            SectionHeaderTitle(title = "Support & Account")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = CallMateCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder)
            ) {
                Column {
                    MockupSettingRow(
                        icon = Icons.Outlined.Shield,
                        title = "Account data",
                        subtitle = uiState.profile?.email ?: "sayyad@callmate.ai",
                        onClick = {}
                    )
                    HorizontalDivider(color = CallMateBorder)
                    MockupSettingRow(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = "Help center",
                        subtitle = "Questions, setup and guides",
                        onClick = {}
                    )
                    HorizontalDivider(color = CallMateBorder)
                    MockupSettingRow(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Sign out",
                        subtitle = "Log out from CallMate",
                        titleColor = Color(0xFFEF4444),
                        onClick = { showSignOutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionHeaderTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = CallMateSlate,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
    )
}

@Composable
fun MockupSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    hasNewBadge: Boolean = false,
    titleColor: Color = CallMateInk,
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
        // Squircle dark container for icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF181818)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor != CallMateInk) titleColor else CallMateInk,
                modifier = Modifier.size(19.dp)
            )
        }

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
                        color = CallMateBadgeGold
                    ) {
                        Text(
                            text = "New",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = CallMateSlate
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CallMateSlate,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Personal Details editor screen matching Screenshot 2:
 * - What should we call you
 * - Full name input pill
 * - Italic: "Your AI will call you 'FirstName'"
 * - Male / Female selection with avatars
 * - Italic: "Used for voice pronouns."
 * - Neon Lime Confirm button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreenContent(
    initialName: String,
    initialGender: String,
    onConfirm: (name: String, gender: String) -> Unit,
    onBack: () -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    var selectedGender by remember {
        mutableStateOf(if (initialGender.equals("Female", ignoreCase = true)) "Female" else "Male")
    }

    val firstName = nameInput.trim().split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "there"

    Scaffold(
        containerColor = CallMateCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Personal Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = CallMateInk
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CallMateInk
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CallMateCanvas,
                    titleContentColor = CallMateInk
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CallMateCanvas)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Large Title
            Text(
                text = "What should we call you",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CallMateInk,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle
            Text(
                text = "Please enter your full name and\nselect your gender",
                fontSize = 15.sp,
                color = CallMateSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Full Name Input (Pill rounded outlined box)
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = CallMateInk
                ),
                shape = RoundedCornerShape(32.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CallMateInk,
                    unfocusedTextColor = CallMateInk,
                    focusedBorderColor = Color.White.copy(alpha = 0.8f),
                    unfocusedBorderColor = CallMateBorder.copy(alpha = 0.7f),
                    focusedContainerColor = CallMateSurface,
                    unfocusedContainerColor = CallMateSurface,
                    cursorColor = CallMatePrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Italic helper: "Your AI will call you 'Sayyad'"
            Text(
                text = "Your AI will call you '$firstName'",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = CallMateSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Male Option Card
            val isMaleSelected = selectedGender.equals("Male", ignoreCase = true)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedGender = "Male" },
                shape = RoundedCornerShape(16.dp),
                color = if (isMaleSelected) Color(0xFF283618).copy(alpha = 0.5f) else CallMateCard,
                border = BorderStroke(
                    1.dp,
                    if (isMaleSelected) Color(0xFFBAFF39).copy(alpha = 0.6f) else CallMateBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isMaleSelected,
                        onClick = { selectedGender = "Male" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFBAFF39),
                            unselectedColor = CallMateSlate
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Male",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CallMateInk,
                        modifier = Modifier.weight(1f)
                    )

                    UserProfileAvatar(size = 40.dp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Female Option Card
            val isFemaleSelected = selectedGender.equals("Female", ignoreCase = true)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedGender = "Female" },
                shape = RoundedCornerShape(16.dp),
                color = if (isFemaleSelected) Color(0xFF283618).copy(alpha = 0.5f) else CallMateCard,
                border = BorderStroke(
                    1.dp,
                    if (isFemaleSelected) Color(0xFFBAFF39).copy(alpha = 0.6f) else CallMateBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isFemaleSelected,
                        onClick = { selectedGender = "Female" },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFFBAFF39),
                            unselectedColor = CallMateSlate
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Female",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CallMateInk,
                        modifier = Modifier.weight(1f)
                    )

                    FemaleUserProfileAvatar(size = 40.dp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Italic helper: "Used for voice pronouns."
            Text(
                text = "Used for voice pronouns.",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = CallMateSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Confirm Button (Neon Lime matching Screenshot 2)
            Button(
                onClick = {
                    onConfirm(nameInput, selectedGender)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBAFF39),
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Confirm",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}


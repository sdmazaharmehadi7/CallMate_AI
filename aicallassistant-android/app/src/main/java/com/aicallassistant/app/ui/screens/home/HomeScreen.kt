package com.aicallassistant.app.ui.screens.home

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.R
import com.aicallassistant.app.core.util.DateUtils
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.ChatThreadItem
import com.aicallassistant.app.ui.components.CallerAvatar
import com.aicallassistant.app.ui.components.InstagramSkeletonChatList
import com.aicallassistant.app.ui.components.UserProfileAvatar
import com.aicallassistant.app.ui.components.UrgencyBadge
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
import com.aicallassistant.app.ui.viewmodel.ChatDateFilter
import com.aicallassistant.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSimulateCall: () -> Unit,
    onNavigateToCallDetail: (String) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = EqualDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // Brand Logo (6 green dots + equal ai)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                modifier = Modifier.padding(end = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EqualGreen))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EqualGreen))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EqualGreen))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EqualGreen))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EqualGreen))
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EqualGreen))
                                }
                            }
                            Text(
                                text = "equal ai",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EqualGreen
                            )
                        }
                    }
                },
                actions = {
                    // Profile avatar and username pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = uiState.userProfile?.fullName ?: "Sayyad",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EqualTextPrimary
                            )
                            Text(
                                text = "@sayyadmazahar",
                                fontSize = 10.sp,
                                color = EqualTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        UserProfileAvatar(size = 36.dp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EqualDarkBg,
                    titleContentColor = EqualTextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToSimulateCall,
                containerColor = EqualGreen,
                contentColor = Color(0xFF0F141C),
                icon = { Icon(Icons.Default.PhoneInTalk, contentDescription = "Simulate") },
                text = { Text("Simulate Call", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(28.dp)
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadDashboardData(isRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(EqualDarkBg)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp)
            ) {
                // 1. AI Assistant is On Toggle Card (Matching Screenshot 3)
                item {
                    AssistantStatusToggleCard(
                        isOn = uiState.isAssistantOn,
                        onToggle = { viewModel.toggleAssistant(it) }
                    )
                }

                // 2. Referral Invite Card (Matching Screenshot 3)
                item {
                    ReferralBannerCard(
                        onInviteClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Join me on CallMate Equal AI — Let your AI assistant answer and screen unknown calls with live voice! Download: https://callmate.ai"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Invite a Friend"))
                        }
                    )
                }

                // 3. Search and Date Filter Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Search Input
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = {
                                Text(
                                    "Search by caller name, number...",
                                    color = EqualTextSecondary,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TealPrimary
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = EqualTextSecondary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = EqualTextPrimary,
                                unfocusedTextColor = EqualTextPrimary,
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = EqualDarkBorder,
                                focusedContainerColor = EqualDarkCard,
                                unfocusedContainerColor = EqualDarkCard,
                                cursorColor = TealPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Date Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(ChatDateFilter.entries) { filter ->
                                val isSelected = uiState.selectedDateFilter == filter
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onDateFilterSelect(filter) },
                                    label = {
                                        Text(
                                            text = filter.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealPrimary,
                                        selectedLabelColor = Color(0xFF0F141C),
                                        containerColor = EqualDarkCard,
                                        labelColor = EqualTextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) TealPrimary else EqualDarkBorder
                                    )
                                )
                            }
                        }
                    }
                }

                // 4. Section Header "CHATS"
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHATS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EqualTextSecondary,
                            letterSpacing = 1.sp
                        )

                        if (uiState.filteredThreads.isNotEmpty()) {
                            Text(
                                text = "${uiState.filteredThreads.size} conversations",
                                fontSize = 11.sp,
                                color = TealPrimary
                            )
                        }
                    }
                }

                // 5. Chat List or Skeleton Shimmer Loading
                when {
                    uiState.isLoading && uiState.chatThreads.isEmpty() -> {
                        item {
                            InstagramSkeletonChatList(count = 4)
                        }
                    }

                    uiState.filteredThreads.isEmpty() -> {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = EqualDarkCard
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Inbox,
                                        contentDescription = null,
                                        tint = EqualGreen.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (uiState.searchQuery.isNotBlank()) "No matching chats found" else "No screened chats yet",
                                        fontWeight = FontWeight.Bold,
                                        color = EqualTextPrimary,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (uiState.searchQuery.isNotBlank()) {
                                            "Try adjusting your query or date filter."
                                        } else {
                                            "Tap 'Simulate Call' to test an incoming call. Your Equal Assistant will screen it and group all calls by contact here."
                                        },
                                        fontSize = 12.sp,
                                        color = EqualTextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Render WhatsApp-style chat rows grouped by contact
                        items(uiState.filteredThreads, key = { it.callerNumber }) { thread ->
                            ChatThreadRow(
                                thread = thread,
                                onClick = { onNavigateToCallDetail(thread.latestCall.id) },
                                onMuteToggle = { viewModel.toggleMuteCaller(thread.callerNumber) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Assistant Status Toggle Card matching reference screenshot:
 * - Glowing lime switch
 * - Robot phone avatar
 * - Subtitle: "Equal is managing your unknown calls!"
 */
@Composable
fun AssistantStatusToggleCard(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF131D19),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOn) EqualGreenDim.copy(alpha = 0.4f) else EqualDarkBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Robot icon badge with green glow
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isOn) EqualGreen.copy(alpha = 0.2f) else EqualDarkCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = if (isOn) EqualGreen else EqualTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isOn) "AI Assistant is On" else "AI Assistant is Off",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EqualTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOn) "Equal is managing your unknown calls!" else "Calls will not be automatically screened",
                        fontSize = 12.sp,
                        color = EqualTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isOn,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EqualGreen,
                    checkedTrackColor = Color(0xFF223114),
                    uncheckedThumbColor = Color(0xFF64748B),
                    uncheckedTrackColor = Color(0xFF1E293B)
                )
            )
        }
    }
}

/**
 * Referral / Invite Card matching Screenshot 3
 */
@Composable
fun ReferralBannerCard(
    onInviteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2433)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Star REFER badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF2C2F40)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "REFER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Invite a Friend",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Give them the same superpower you have",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onInviteClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C66DC),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Invite Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Clapping hands illustration graphic
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2D354E), Color(0xFF1E2235))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🙌", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Carousel dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4A5568)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4A5568)))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EqualGreen))
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4A5568)))
            }
        }
    }
}

/**
 * WhatsApp-style Chat thread row (matching Screenshot 3):
 * - Vintage red phone avatar or Equal assistant avatar
 * - Caller name (with verified checkmark for assistant)
 * - Timestamp on right
 * - Summary / latest dialogue snippet
 * - Action chip below: "Mute [Caller] 🔕 >"
 */
@Composable
fun ChatThreadRow(
    thread: ChatThreadItem,
    onClick: () -> Unit,
    onMuteToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EqualDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Caller Avatar
                CallerAvatar(
                    callerName = thread.callerName,
                    callerNumber = thread.callerNumber,
                    size = 46.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = thread.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = EqualTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Verified badge if Assistant
                            val isAssistant = (thread.callerName?.contains("equal", ignoreCase = true) == true) ||
                                    (thread.callerName?.contains("assistant", ignoreCase = true) == true)
                            if (isAssistant) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Assistant",
                                    tint = EqualGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (thread.calls.size > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = TealPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${thread.calls.size} calls",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = DateUtils.formatChatTimestamp(thread.latestTimestamp),
                            fontSize = 11.sp,
                            color = EqualTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = thread.latestSummary ?: "Call recorded without summary",
                        fontSize = 13.sp,
                        color = EqualTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Pill Button (Matching Screenshot 3)
            Row(modifier = Modifier.padding(start = 58.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EqualDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EqualDarkBorder),
                    modifier = Modifier.clickable(onClick = onMuteToggle)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (thread.isMuted) "Unmute ${thread.displayName.take(12)}" else "Mute ${thread.displayName.take(12)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (thread.isMuted) Color(0xFFFBBF24) else EqualTextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (thread.isMuted) "🔔 >" else "🔕 >",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legacy CallItemCard preserved for history and backward compatibility
 */
@Composable
fun CallItemCard(
    call: CallItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = EqualDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CallerAvatar(
                        callerName = call.callerName,
                        callerNumber = call.callerNumber,
                        size = 36.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = call.callerName?.ifBlank { null } ?: call.callerNumber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = EqualTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = call.callerNumber,
                            fontSize = 11.sp,
                            color = EqualTextSecondary
                        )
                    }
                }

                UrgencyBadge(urgency = call.urgency)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = call.summary ?: call.callerReason ?: "Call recorded without summary",
                fontSize = 12.sp,
                color = EqualTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = DateUtils.formatChatTimestamp(call.createdAt),
                    fontSize = 11.sp,
                    color = EqualTextSecondary
                )
                Text(
                    text = DateUtils.formatDuration(call.durationSeconds),
                    fontSize = 11.sp,
                    color = TealPrimary
                )
            }
        }
    }
}

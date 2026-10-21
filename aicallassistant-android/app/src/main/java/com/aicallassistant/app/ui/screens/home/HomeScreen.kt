package com.aicallassistant.app.ui.screens.home

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.core.config.AppConfig
import com.aicallassistant.app.core.util.DateUtils
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.ChatThreadItem
import com.aicallassistant.app.ui.components.CallerAvatar
import com.aicallassistant.app.ui.components.InstagramSkeletonChatList
import com.aicallassistant.app.ui.components.UrgencyBadge
import com.aicallassistant.app.ui.components.UserGenderAvatar
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

    Scaffold(
        containerColor = CallMateCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // White rounded squircle icon with black telephone
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "CallMate AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallMateInk
                        )
                    }
                },
                actions = {
                    // Profile avatar matching user's selected gender
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        UserGenderAvatar(
                            gender = AppConfig.getUserGender(),
                            size = 36.dp
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
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.loadDashboardData(isRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CallMateCanvas)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 95.dp)
            ) {
                // 1. Prominent White Simulate Call Button (Matching Mockup)
                item {
                    Button(
                        onClick = onNavigateToSimulateCall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulate call",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // 2. Assistant is on Card (Matching Mockup)
                item {
                    MockupAssistantCard(
                        isOn = uiState.isAssistantOn,
                        onToggle = { viewModel.toggleAssistant(it) }
                    )
                }

                // 3. Search Bar (Matching Mockup)
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = {
                            Text(
                                "Search by name or number",
                                color = CallMateSlate,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = CallMateSlate
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = CallMateSlate
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CallMateInk,
                            unfocusedTextColor = CallMateInk,
                            focusedBorderColor = CallMatePrimary,
                            unfocusedBorderColor = CallMateBorder,
                            focusedContainerColor = CallMateCard,
                            unfocusedContainerColor = CallMateCard,
                            cursorColor = CallMatePrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 4. Filter Chips: All, Today, Yesterday, This week (Matching Mockup)
                item {
                    val filterOptions = listOf(
                        ChatDateFilter.ALL to "All",
                        ChatDateFilter.TODAY to "Today",
                        ChatDateFilter.YESTERDAY to "Yesterday",
                        ChatDateFilter.THIS_WEEK to "This week"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterOptions) { (filter, label) ->
                            val isSelected = uiState.selectedDateFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onDateFilterSelect(filter) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = Color.Black,
                                    containerColor = CallMateCard,
                                    labelColor = CallMateInk
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) Color.White else CallMateBorder
                                )
                            )
                        }
                    }
                }

                // 5. Section Header "Screened calls" (Matching Mockup)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Screened calls",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallMateInk
                        )

                        if (uiState.filteredThreads.isNotEmpty()) {
                            Text(
                                text = "${uiState.filteredThreads.size}",
                                fontSize = 12.sp,
                                color = CallMateSlate
                            )
                        }
                    }
                }

                // 6. Empty State or Filtered Threads
                when {
                    uiState.isLoading && uiState.chatThreads.isEmpty() -> {
                        item {
                            InstagramSkeletonChatList(count = 4)
                        }
                    }

                    uiState.filteredThreads.isEmpty() -> {
                        item {
                            // Empty State Box exactly matching Mockup
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = CallMateCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Circular dark container with blue phone
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF181818)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Call,
                                            contentDescription = null,
                                            tint = CallMatePrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "No screened calls yet",
                                        fontWeight = FontWeight.Bold,
                                        color = CallMateInk,
                                        fontSize = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Tap Simulate call to test an incoming call. CallMate AI will screen it and group calls by contact here.",
                                        fontSize = 12.sp,
                                        color = CallMateSlate,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // Contact Threads List
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
 * Assistant Switch Card matching mockup:
 * - Rounded dark card
 * - Robot squircle icon in Primary Blue (#3291FF)
 * - "Assistant is on" / "Screening unknown calls"
 * - Active Green switch (#0CCE6B)
 */
@Composable
fun MockupAssistantCard(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CallMateCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder)
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
                // Blue squircle robot badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = CallMatePrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isOn) "Assistant is on" else "Assistant is off",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallMateInk
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOn) "Screening unknown calls" else "Calls will not be screened",
                        fontSize = 12.sp,
                        color = CallMateSlate
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isOn,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CallMateActive,
                    uncheckedThumbColor = CallMateSlate,
                    uncheckedTrackColor = Color(0xFF1E1E1E)
                )
            )
        }
    }
}

/**
 * WhatsApp-style Chat thread row
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
        colors = CardDefaults.cardColors(containerColor = CallMateCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                CallerAvatar(
                    callerName = thread.callerName,
                    callerNumber = thread.callerNumber,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = thread.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallMateInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = DateUtils.formatChatTimestamp(thread.latestTimestamp),
                            fontSize = 11.sp,
                            color = CallMateSlate
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = thread.latestSummary ?: "Call recorded without summary",
                        fontSize = 12.sp,
                        color = CallMateSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CallMateSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder),
                    modifier = Modifier.clickable(onClick = onMuteToggle)
                ) {
                    Text(
                        text = if (thread.isMuted) "Unmute caller 🔔" else "Mute caller 🔕",
                        fontSize = 11.sp,
                        color = if (thread.isMuted) CallMateBadgeGold else CallMateInk,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (thread.calls.size > 1) {
                    Text(
                        text = "${thread.calls.size} calls",
                        fontSize = 11.sp,
                        color = CallMatePrimary
                    )
                }
            }
        }
    }
}

/**
 * Backward-compatible CallItemCard for history/other views
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
        colors = CardDefaults.cardColors(containerColor = CallMateCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CallMateBorder),
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
                            color = CallMateInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = call.callerNumber,
                            fontSize = 11.sp,
                            color = CallMateSlate
                        )
                    }
                }

                UrgencyBadge(urgency = call.urgency)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = call.summary ?: call.callerReason ?: "Call recorded without summary",
                fontSize = 12.sp,
                color = CallMateSlate,
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
                    color = CallMateSlate
                )
                Text(
                    text = DateUtils.formatDuration(call.durationSeconds),
                    fontSize = 11.sp,
                    color = CallMatePrimary
                )
            }
        }
    }
}

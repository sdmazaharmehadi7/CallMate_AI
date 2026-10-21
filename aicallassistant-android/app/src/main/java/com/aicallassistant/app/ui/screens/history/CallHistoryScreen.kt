package com.aicallassistant.app.ui.screens.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.core.util.DateUtils
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.ui.components.CallerAvatar
import com.aicallassistant.app.ui.components.InstagramSkeletonCallList
import com.aicallassistant.app.ui.components.UrgencyBadge
import com.aicallassistant.app.ui.theme.EqualDarkBg
import com.aicallassistant.app.ui.theme.EqualDarkBorder
import com.aicallassistant.app.ui.theme.EqualDarkCard
import com.aicallassistant.app.ui.theme.EqualDarkSurface
import com.aicallassistant.app.ui.theme.EqualGreen
import com.aicallassistant.app.ui.theme.EqualGreenText
import com.aicallassistant.app.ui.theme.EqualTextPrimary
import com.aicallassistant.app.ui.theme.EqualTextSecondary
import com.aicallassistant.app.ui.theme.TealPrimary
import com.aicallassistant.app.ui.viewmodel.CallHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    viewModel: CallHistoryViewModel,
    onNavigateToCallDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = EqualDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = EqualGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Call History",
                            color = EqualTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCalls(reset = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh calls",
                            tint = TealPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EqualDarkBg,
                    titleContentColor = EqualTextPrimary
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.loadCalls(isRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(EqualDarkBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Search Input
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = {
                        Text(
                            "Search caller, number, summary...",
                            color = EqualTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TealPrimary)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = EqualTextSecondary)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                // Filter Chips Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedStatus == null && !uiState.importantOnly,
                            onClick = {
                                viewModel.onStatusFilterChange(null)
                                if (uiState.importantOnly) viewModel.onImportantFilterToggle()
                            },
                            label = { Text("All Calls", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealPrimary,
                                selectedLabelColor = Color(0xFF0F141C),
                                containerColor = EqualDarkCard,
                                labelColor = EqualTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedStatus == null && !uiState.importantOnly,
                                borderColor = if (uiState.selectedStatus == null && !uiState.importantOnly) TealPrimary else EqualDarkBorder
                            )
                        )
                    }

                    item {
                        FilterChip(
                            selected = uiState.importantOnly,
                            onClick = { viewModel.onImportantFilterToggle() },
                            label = { Text("Starred ★", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFBBF24),
                                selectedLabelColor = Color(0xFF0F141C),
                                containerColor = EqualDarkCard,
                                labelColor = EqualTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.importantOnly,
                                borderColor = if (uiState.importantOnly) Color(0xFFFBBF24) else EqualDarkBorder
                            )
                        )
                    }

                    listOf("completed", "active", "failed").forEach { status ->
                        item {
                            val isSelected = uiState.selectedStatus == status
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onStatusFilterChange(if (isSelected) null else status) },
                                label = { Text(status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
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

                // Group calls by date!
                val groupedCalls = uiState.calls.groupBy { call ->
                    DateUtils.getRelativeDateHeader(call.createdAt)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    when {
                        uiState.isLoading && uiState.calls.isEmpty() -> {
                            item {
                                InstagramSkeletonCallList(count = 5)
                            }
                        }

                        uiState.calls.isEmpty() -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.Inbox,
                                            contentDescription = null,
                                            tint = EqualGreen.copy(alpha = 0.5f),
                                            modifier = Modifier.size(54.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No calls found",
                                            fontWeight = FontWeight.Bold,
                                            color = EqualTextPrimary,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Screened and simulated calls will appear here grouped by date.",
                                            color = EqualTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            // Render sectioned by date
                            groupedCalls.forEach { (dateHeader, callsForDate) ->
                                item(key = "header_$dateHeader") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = EqualDarkSurface
                                        ) {
                                            Text(
                                                text = dateHeader,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EqualGreenText,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }

                                        Text(
                                            text = "${callsForDate.size} calls",
                                            fontSize = 11.sp,
                                            color = EqualTextSecondary
                                        )
                                    }
                                }

                                items(callsForDate, key = { it.id }) { call ->
                                    CallHistoryCard(
                                        call = call,
                                        onClick = { onNavigateToCallDetail(call.id) },
                                        onToggleImportant = { viewModel.toggleImportant(call) }
                                    )
                                }
                            }

                            if (uiState.isPaginating) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = TealPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryCard(
    call: CallItem,
    onClick: () -> Unit,
    onToggleImportant: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EqualDarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, com.aicallassistant.app.ui.theme.CallMateBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallerAvatar(
                    callerName = call.callerName,
                    callerNumber = call.callerNumber,
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = call.callerName?.ifBlank { null } ?: call.callerNumber,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EqualTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = DateUtils.formatTimeOnly(call.createdAt),
                            fontSize = 11.sp,
                            color = EqualTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = call.callerNumber,
                        fontSize = 11.sp,
                        color = TealPrimary
                    )
                }

                IconButton(
                    onClick = onToggleImportant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (call.isImportant) Color(0xFFFBBF24) else EqualDarkBorder,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = call.summary ?: call.callerReason ?: "Call recorded without summary",
                fontSize = 12.sp,
                color = EqualTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UrgencyBadge(urgency = call.urgency)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EqualDarkSurface
                    ) {
                        Text(
                            text = DateUtils.formatDuration(call.durationSeconds),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = EqualTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.clickable(onClick = onClick)
                ) {
                    Text(
                        text = "View Chat >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

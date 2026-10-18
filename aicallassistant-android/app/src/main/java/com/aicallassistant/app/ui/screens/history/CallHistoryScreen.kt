package com.aicallassistant.app.ui.screens.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.R
import com.aicallassistant.app.ui.screens.home.CallItemCard
import com.aicallassistant.app.ui.theme.CallMateBackgroundGradient
import com.aicallassistant.app.ui.theme.CallMateCyan
import com.aicallassistant.app.ui.theme.CallMateNavyBg
import com.aicallassistant.app.ui.theme.CallMateNavyElevated
import com.aicallassistant.app.ui.theme.CallMateNavySurface
import com.aicallassistant.app.ui.viewmodel.CallHistoryViewModel

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material.icons.filled.Refresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    viewModel: CallHistoryViewModel,
    onNavigateToCallDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = CallMateNavyBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.callmate_logo),
                            contentDescription = "CallMate",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Call History",
                            color = Color.White,
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
                            tint = CallMateCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CallMateNavyBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CallMateBackgroundGradient)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search caller, number, or summary...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CallMateCyan) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFF94A3B8))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CallMateCyan,
                    unfocusedBorderColor = Color(0xFF334155),
                    cursorColor = CallMateCyan
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val selected = uiState.selectedStatus == null && !uiState.importantOnly
                    FilterChip(
                        selected = selected,
                        onClick = {
                            viewModel.onStatusFilterChange(null)
                            if (uiState.importantOnly) viewModel.onImportantFilterToggle()
                        },
                        label = { Text("All", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CallMateCyan,
                            selectedLabelColor = CallMateNavyBg,
                            containerColor = CallMateNavyElevated,
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
                item {
                    val selected = uiState.selectedStatus == "completed"
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (uiState.selectedStatus == "completed") null else "completed"
                            viewModel.onStatusFilterChange(next)
                        },
                        label = { Text("Completed", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CallMateCyan,
                            selectedLabelColor = CallMateNavyBg,
                            containerColor = CallMateNavyElevated,
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
                item {
                    val selected = uiState.selectedStatus == "active"
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = if (uiState.selectedStatus == "active") null else "active"
                            viewModel.onStatusFilterChange(next)
                        },
                        label = { Text("Active", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CallMateCyan,
                            selectedLabelColor = CallMateNavyBg,
                            containerColor = CallMateNavyElevated,
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
                item {
                    val selected = uiState.importantOnly
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onImportantFilterToggle() },
                        label = { Text("Starred", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CallMateCyan,
                            selectedLabelColor = CallMateNavyBg,
                            containerColor = CallMateNavyElevated,
                            labelColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calls List Area with Pull to Refresh
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.loadCalls(reset = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.calls.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CallMateCyan)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Loading calls...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    }
                    uiState.errorMessage != null && uiState.calls.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Error Loading Calls",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.errorMessage ?: "Check backend connection",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    uiState.calls.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Inbox,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No calls found",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.searchQuery.isNotBlank()) "No records matching '${uiState.searchQuery}'." else "No screened calls recorded yet.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.calls, key = { it.id }) { call ->
                                CallItemCard(
                                    call = call,
                                    onClick = { onNavigateToCallDetail(call.id) }
                                )
                            }

                            if (uiState.calls.size < uiState.totalCount) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isPaginating) {
                                            CircularProgressIndicator(color = CallMateCyan)
                                        } else {
                                            viewModel.loadCalls(reset = false)
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
}

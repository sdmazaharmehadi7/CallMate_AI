package com.aicallassistant.app.ui.screens.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aicallassistant.app.ui.components.EmptyStateView
import com.aicallassistant.app.ui.components.ErrorStateView
import com.aicallassistant.app.ui.components.LoadingIndicatorView
import com.aicallassistant.app.ui.screens.home.CallItemCard
import com.aicallassistant.app.ui.viewmodel.CallHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    viewModel: CallHistoryViewModel,
    onNavigateToCallDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Call History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search caller, number, or summary...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedStatus == null && !uiState.importantOnly,
                        onClick = {
                            viewModel.onStatusFilterChange(null)
                            if (uiState.importantOnly) viewModel.onImportantFilterToggle()
                        },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedStatus == "completed",
                        onClick = {
                            val next = if (uiState.selectedStatus == "completed") null else "completed"
                            viewModel.onStatusFilterChange(next)
                        },
                        label = { Text("Completed") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.selectedStatus == "active",
                        onClick = {
                            val next = if (uiState.selectedStatus == "active") null else "active"
                            viewModel.onStatusFilterChange(next)
                        },
                        label = { Text("Active") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.importantOnly,
                        onClick = { viewModel.onImportantFilterToggle() },
                        label = { Text("Starred") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calls List Area
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        LoadingIndicatorView(message = "Loading call records...")
                    }
                    uiState.errorMessage != null && uiState.calls.isEmpty() -> {
                        ErrorStateView(
                            message = uiState.errorMessage ?: "Failed to load calls",
                            onRetry = { viewModel.loadCalls(reset = true) }
                        )
                    }
                    uiState.calls.isEmpty() -> {
                        EmptyStateView(
                            title = "No calls found",
                            message = if (uiState.searchQuery.isNotBlank()) "No records matching '${uiState.searchQuery}'." else "No call history available."
                        )
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
                                            CircularProgressIndicator()
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

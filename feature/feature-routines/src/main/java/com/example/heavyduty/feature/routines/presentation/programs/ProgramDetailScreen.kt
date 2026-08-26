package com.example.heavyduty.feature.routines.presentation.programs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    onNavigateBack: () -> Unit,
    onRoutineClick: (Long) -> Unit, // Navigate to routine editor
    modifier: Modifier = Modifier,
    viewModel: ProgramDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val program = uiState.program

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(program?.name ?: "Program Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading || program == null) {
            HdLoadingIndicator(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(program.weeks) { week ->
                    HdCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Week ${week.weekNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (week.days.isEmpty()) {
                                Text(
                                    text = "No routines assigned to this week yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            week.days.forEach { routine ->
                                Button(
                                    onClick = { onRoutineClick(routine.id) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(routine.name)
                                }
                            }
                            Button(
                                onClick = { /* TODO: Add new routine for this week */ },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                Text("Add Training Day")
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = { viewModel.addWeek() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Week")
                    }
                }
            }
        }
    }
}

package com.example.heavyduty.feature.stats.presentation.series

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.ui.components.HdCardsSkeleton
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdErrorState
import com.example.heavyduty.core.ui.components.HdHorizontalBarChart
import com.example.heavyduty.core.ui.components.HdPeriodSelector
import com.example.heavyduty.core.ui.nameResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesPorMusculoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesPorMusculoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_series_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            HdCardsSkeleton(modifier = Modifier.padding(innerPadding).padding(16.dp), heights = listOf(48.dp, 64.dp, 280.dp))
            return@Scaffold
        }
        if (uiState.loadFailed) {
            HdErrorState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            HdPeriodSelector(
                selected = uiState.selectedPeriod,
                onSelect = { viewModel.onEvent(SeriesPorMusculoUiEvent.OnPeriodChange(it)) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.stats_series_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.data.isEmpty()) {
                HdEmptyState(
                    icon = Icons.Default.Timeline,
                    title = stringResource(R.string.stats_series_empty_title),
                    description = stringResource(R.string.stats_series_empty_desc)
                )
            } else {
                val data = uiState.data.map { (muscle, value) -> stringResource(muscle.nameResId) to value }
                HdHorizontalBarChart(
                    data = data,
                    valueFormatter = { value -> value.formatDecimal() }
                )
            }
        }
    }
}

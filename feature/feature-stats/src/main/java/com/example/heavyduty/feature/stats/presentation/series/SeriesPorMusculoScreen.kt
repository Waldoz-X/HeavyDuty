package com.example.heavyduty.feature.stats.presentation.series

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.heavyduty.core.ui.components.HdBarChart
import com.example.heavyduty.core.ui.components.HdEmptyState
import com.example.heavyduty.core.ui.components.HdLoadingIndicator
import com.example.heavyduty.core.ui.nameResId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesPorMusculoScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesPorMusculoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuento de series") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        when {
            uiState.isLoading -> HdLoadingIndicator(modifier = Modifier.padding(innerPadding), message = "Cargando...")
            uiState.data.isEmpty() -> HdEmptyState(
                icon = Icons.Default.Timeline,
                title = "Sin series esta semana",
                description = "Completa entrenamientos para ver el recuento por grupo muscular.",
                modifier = Modifier.padding(innerPadding),
            )
            else -> {
                val data = uiState.data.map { (muscle, value) -> stringResource(muscle.nameResId) to value }
                HdBarChart(
                    data = data,
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp),
                    valueFormatter = { value -> "%.0f".format(value) },
                )
            }
        }
    }
}

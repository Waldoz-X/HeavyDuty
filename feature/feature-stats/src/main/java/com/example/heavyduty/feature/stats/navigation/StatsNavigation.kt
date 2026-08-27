package com.example.heavyduty.feature.stats.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.heavyduty.feature.stats.presentation.clasificacion.EjerciciosClasificacionScreen
import com.example.heavyduty.feature.stats.presentation.distribucioncorporal.DistribucionCorporalScreen
import com.example.heavyduty.feature.stats.presentation.distribucionmuscular.DistribucionMuscularScreen
import com.example.heavyduty.feature.stats.presentation.informemensual.InformeMensualScreen
import com.example.heavyduty.feature.stats.presentation.main.EstadisticasScreen
import com.example.heavyduty.feature.stats.presentation.principales.EjerciciosPrincipalesScreen
import com.example.heavyduty.feature.stats.presentation.series.SeriesPorMusculoScreen

/**
 * Registers the "Estadísticas" hub and its six advanced-stat sub-screens in the NavHost.
 */
fun NavGraphBuilder.statsGraph(
    onNavigateBack: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToDistribucionRadar: () -> Unit,
    onNavigateToDistribucionCuerpo: () -> Unit,
    onNavigateToPrincipales: () -> Unit,
    onNavigateToClasificacion: () -> Unit,
    onNavigateToInformeMensual: () -> Unit,
) {
    composable(route = "stats_graph") {
        EstadisticasScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSeries = onNavigateToSeries,
            onNavigateToDistribucionRadar = onNavigateToDistribucionRadar,
            onNavigateToDistribucionCuerpo = onNavigateToDistribucionCuerpo,
            onNavigateToPrincipales = onNavigateToPrincipales,
            onNavigateToClasificacion = onNavigateToClasificacion,
            onNavigateToInformeMensual = onNavigateToInformeMensual,
        )
    }
    composable(route = "stats/series") {
        SeriesPorMusculoScreen(onNavigateBack = onNavigateBack)
    }
    composable(route = "stats/distribucion_radar") {
        DistribucionMuscularScreen(onNavigateBack = onNavigateBack)
    }
    composable(route = "stats/distribucion_cuerpo") {
        DistribucionCorporalScreen(onNavigateBack = onNavigateBack)
    }
    composable(route = "stats/principales") {
        EjerciciosPrincipalesScreen(onNavigateBack = onNavigateBack)
    }
    composable(route = "stats/clasificacion") {
        EjerciciosClasificacionScreen(onNavigateBack = onNavigateBack)
    }
    composable(route = "stats/informe_mensual") {
        InformeMensualScreen(onNavigateBack = onNavigateBack)
    }
}

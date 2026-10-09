package com.example.heavyduty.feature.stats.presentation.informemensual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.core.common.flow.whileSubscribed
import com.example.heavyduty.feature.stats.domain.usecase.GetMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InformeMensualViewModel @Inject constructor(
    private val getMonthlyReport: GetMonthlyReportUseCase,
) : ViewModel() {

    private val _currentMillis = MutableStateFlow(System.currentTimeMillis())
    private val _uiState = MutableStateFlow(InformeMensualUiState())

    /** Loads only while the screen is watching — see [whileSubscribed]. */
    val uiState: StateFlow<InformeMensualUiState> = _uiState.whileSubscribed(viewModelScope) {
        launch {
            // Switching months drops the previous month's query (flatMapLatest).
            _currentMillis
                .flatMapLatest { millis -> getMonthlyReport(millis).map { report -> millis to report } }
                .collect { (millis, report) ->
                    _uiState.update {
                        it.copy(
                            report = report,
                            isLoading = false,
                            hasNextMonth = canGoForward(millis),
                        )
                    }
                }
        }
    }

    init {
        showMonth(_currentMillis.value)
    }

    private fun showMonth(millis: Long) {
        _currentMillis.value = millis
        _uiState.update {
            it.copy(
                isLoading = true,
                currentReferenceMillis = millis,
                hasNextMonth = canGoForward(millis),
            )
        }
    }

    fun selectPreviousMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMillis.value
            add(Calendar.MONTH, -1)
        }
        showMonth(cal.timeInMillis)
    }

    fun selectNextMonth() {
        if (!canGoForward(_currentMillis.value)) return
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMillis.value
            add(Calendar.MONTH, 1)
        }
        showMonth(cal.timeInMillis)
    }

    private fun canGoForward(millis: Long): Boolean {
        val currentMonthCal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
        }
        val targetMonthCal = Calendar.getInstance().apply {
            timeInMillis = millis
        }
        val currentKey = currentMonthCal.get(Calendar.YEAR) * 12 + currentMonthCal.get(Calendar.MONTH)
        val targetKey = targetMonthCal.get(Calendar.YEAR) * 12 + targetMonthCal.get(Calendar.MONTH)
        return targetKey < currentKey
    }
}

package com.example.heavyduty.feature.stats.presentation.informemensual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.heavyduty.feature.stats.domain.usecase.GetMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class InformeMensualViewModel @Inject constructor(
    private val getMonthlyReport: GetMonthlyReportUseCase,
) : ViewModel() {

    private val _currentMillis = MutableStateFlow(System.currentTimeMillis())
    private val _uiState = MutableStateFlow(InformeMensualUiState())
    val uiState: StateFlow<InformeMensualUiState> = _uiState.asStateFlow()

    private var reportJob: Job? = null

    init {
        loadReportForMillis(_currentMillis.value)
    }

    private fun loadReportForMillis(millis: Long) {
        reportJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                currentReferenceMillis = millis,
                hasNextMonth = canGoForward(millis),
            )
        }
        reportJob = viewModelScope.launch {
            getMonthlyReport(millis).collect { report ->
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

    fun selectPreviousMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMillis.value
            add(Calendar.MONTH, -1)
        }
        _currentMillis.value = cal.timeInMillis
        loadReportForMillis(cal.timeInMillis)
    }

    fun selectNextMonth() {
        if (!canGoForward(_currentMillis.value)) return
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMillis.value
            add(Calendar.MONTH, 1)
        }
        _currentMillis.value = cal.timeInMillis
        loadReportForMillis(cal.timeInMillis)
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

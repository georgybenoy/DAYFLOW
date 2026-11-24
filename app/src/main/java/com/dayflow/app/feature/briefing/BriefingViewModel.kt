package com.dayflow.app.feature.briefing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayflow.app.core.time.TimeProvider
import com.dayflow.app.domain.briefing.BriefingContent
import com.dayflow.app.domain.usecase.GetBriefingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BriefingUiState(
    val date: LocalDate,
    val briefing: BriefingContent? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BriefingViewModel @Inject constructor(
    private val getBriefingUseCase: GetBriefingUseCase,
    private val timeProvider: TimeProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val targetDate: LocalDate = savedStateHandle.get<String>("dateString")?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            timeProvider.nowLocalDate()
        }
    } ?: timeProvider.nowLocalDate()

    private val _uiState = MutableStateFlow(BriefingUiState(date = targetDate))
    val uiState: StateFlow<BriefingUiState> = _uiState.asStateFlow()

    init {
        loadBriefing()
    }

    fun loadBriefing() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val content = getBriefingUseCase(targetDate)
                _uiState.update { it.copy(briefing = content, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}

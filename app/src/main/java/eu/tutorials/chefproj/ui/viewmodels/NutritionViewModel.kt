package eu.tutorials.chefproj.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.DailyNutritionSummary
import eu.tutorials.chefproj.Data.api.MealPlan
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NutritionUiState(
    val todaySummary: DailyNutritionSummary? = null,
    val mealPlans: List<MealPlan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NutritionViewModel(
    private val repository: NutriBotRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init { loadAll() }

    private fun loadAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            loadMealPlans()
            loadTodayNutrition()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadTodayNutrition() {
        viewModelScope.launch {
            repository.getTodayNutrition(userId).collect { result ->
                result.fold(
                    onSuccess = { summary ->
                        _uiState.update { it.copy(todaySummary = summary, isLoading = false) }
                    },
                    onFailure = { computeTodaySummaryLocally() }
                )
            }
        }
    }

    fun loadMealPlans(days: Int = 14) {
        viewModelScope.launch {
            repository.getMealPlans(userId, days).collect { result ->
                result.fold(
                    onSuccess = { meals ->
                        _uiState.update { it.copy(mealPlans = meals, isLoading = false) }
                        if (_uiState.value.todaySummary == null) computeTodaySummaryLocally()
                    },
                    onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun computeTodaySummaryLocally() {
        val today = LocalDate.now().toString()
        val meals = _uiState.value.mealPlans.filter { it.planDate == today }
        if (meals.isEmpty()) return
        _uiState.update {
            it.copy(todaySummary = DailyNutritionSummary(
                calories = meals.sumOf { m -> m.calories },
                proteinG = meals.sumOf { m -> m.proteinG.toDouble() }.toFloat(),
                carbsG   = meals.sumOf { m -> m.carbsG.toDouble()   }.toFloat(),
                fatG     = meals.sumOf { m -> m.fatG.toDouble()     }.toFloat(),
            ))
        }
    }

    fun getProgressPercentage(): Float =
        ((_uiState.value.todaySummary?.calories ?: 0) / 1500f).coerceAtMost(1f)

    fun clearError() { _uiState.update { it.copy(error = null) } }
}

class NutritionViewModelFactory(private val userId: String) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NutritionViewModel(NutriBotRepository(), userId) as T
    }
}

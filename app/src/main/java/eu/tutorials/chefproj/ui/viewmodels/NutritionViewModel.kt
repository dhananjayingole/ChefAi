package eu.tutorials.chefproj.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.DailyNutritionSummary
import eu.tutorials.chefproj.Data.api.MealPlan
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NutritionUiState(
    val todaySummary: DailyNutritionSummary? = null,
    val mealPlans: List<MealPlan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class NutritionViewModel(
    private val repository: NutriBotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    /** Load both today nutrition and all meal plans in parallel. */
    private fun loadAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Load meal plans first (7 days)
            loadMealPlans()
            // Then try to get server-side nutrition summary
            loadTodayNutrition()
        }
    }

    fun loadTodayNutrition() {
        viewModelScope.launch {
            repository.getTodayNutrition().collect { result ->
                result.fold(
                    onSuccess = { summary ->
                        _uiState.update { it.copy(todaySummary = summary, isLoading = false, error = null) }
                    },
                    onFailure = {
                        // Server summary failed — compute locally from saved meal plans
                        computeTodaySummaryLocally()
                    }
                )
            }
        }
    }

    fun loadMealPlans(days: Int = 14) {
        viewModelScope.launch {
            repository.getMealPlans(days).collect { result ->
                result.fold(
                    onSuccess = { meals ->
                        _uiState.update { it.copy(mealPlans = meals, isLoading = false, error = null) }
                        // If we don't have a server summary yet, compute from local meals
                        if (_uiState.value.todaySummary == null) {
                            computeTodaySummaryLocally()
                        }
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(isLoading = false, error = err.message) }
                    }
                )
            }
        }
    }

    /**
     * Falls back to aggregating today's meals from the meal plan list.
     * This guarantees the Nutrition tab always reflects saved meals.
     */
    private fun computeTodaySummaryLocally() {
        val today = LocalDate.now().toString()
        val todayMeals = _uiState.value.mealPlans.filter { it.planDate == today }

        if (todayMeals.isEmpty()) return

        val summary = DailyNutritionSummary(
            calories = todayMeals.sumOf { it.calories },
            proteinG = todayMeals.sumOf { it.proteinG.toDouble() }.toFloat(),
            carbsG   = todayMeals.sumOf { it.carbsG.toDouble() }.toFloat(),
            fatG     = todayMeals.sumOf { it.fatG.toDouble() }.toFloat()
        )
        _uiState.update { it.copy(todaySummary = summary) }
    }

    /** Progress as fraction of 1500 kcal daily target. */
    fun getProgressPercentage(): Float {
        val cal = _uiState.value.todaySummary?.calories ?: 0
        return (cal / 1500f).coerceAtMost(1f)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

class NutritionViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NutritionViewModel(
            repository = eu.tutorials.chefproj.Data.repository.NutriBotRepository()
        ) as T
    }
}
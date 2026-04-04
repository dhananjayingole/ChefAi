package eu.tutorials.chefproj.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.BudgetData
import eu.tutorials.chefproj.Data.api.EcoData
import eu.tutorials.chefproj.Data.api.HealthAdviceResponse
import eu.tutorials.chefproj.Data.api.MealPlanSaveRequest
import eu.tutorials.chefproj.Data.api.NutritionData
import eu.tutorials.chefproj.Data.api.RecipeGenerationRequest
import eu.tutorials.chefproj.Data.api.RecipeRatingRequest
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeUiState(
    val recipe: String? = null,
    val ingredients: List<Map<String, Any>> = emptyList(),
    val nutrition: NutritionData? = null,
    val budget: BudgetData? = null,
    val ecoScore: EcoData? = null,
    val isLoading: Boolean = false,
    val isRating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val currentStep: Int = 0,
    val cookingSteps: List<CookingStepUi> = emptyList(),
    val isCookingMode: Boolean = false,
    val shoppingList: String? = null,
    val healthAdvice: HealthAdviceResponse? = null,
    val weeklyPlan: String? = null,
    val isGeneratingShoppingList: Boolean = false,
    val isGettingHealthAdvice: Boolean = false,
    val isGeneratingWeeklyPlan: Boolean = false
)

data class CookingStepUi(
    val stepNumber: Int,
    val instruction: String,
    val timerSeconds: Int,
    val isCompleted: Boolean = false
)

class RecipeViewModel(
    private val repository: NutriBotRepository,
    private val userId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeUiState())
    val uiState: StateFlow<RecipeUiState> = _uiState.asStateFlow()

    fun generateRecipe(
        query: String,
        dietaryRestrictions: List<String> = emptyList(),
        healthConditions: List<String> = emptyList(),
        calorieLimit: Int = 500,
        budgetLimit: Float = 500f,
        servings: Int = 2,
        cuisinePreference: String = "Indian"
    ) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val request = RecipeGenerationRequest(
                query = query,
                userId = userId,
                dietaryRestrictions = dietaryRestrictions,
                healthConditions = healthConditions,
                calorieLimit = calorieLimit,
                budgetLimit = budgetLimit,
                servings = servings,
                cuisinePreference = cuisinePreference
            )
            repository.generateRecipe(request).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        _uiState.update { it.copy(
                            recipe = response.recipe,
                            ingredients = response.ingredients,
                            nutrition = response.nutrition,
                            budget = response.budget,
                            ecoScore = response.ecoScore,
                            isLoading = false,
                            error = null
                        )}
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                    }
                )
            }
        }
    }

    fun rateRecipe(recipeName: String, rating: Int, feedback: String? = null) {
        _uiState.update { it.copy(isRating = true) }
        viewModelScope.launch {
            val result = repository.rateRecipe(
                userId = userId ?: "",
                request = RecipeRatingRequest(
                    recipeName = recipeName,
                    rating = rating,
                    feedback = feedback
                )
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isRating = false, successMessage = "Thanks for rating $recipeName!") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isRating = false, error = error.message) }
                }
            )
        }
    }

    /** Save the current recipe to the meal plan calendar */
    fun saveToMealPlan(
        mealType: String,
        planDate: String,
        recipeName: String,
        nutrition: NutritionData? = null
    ) {
        viewModelScope.launch {
            val ns = nutrition?.perServing
            val request = MealPlanSaveRequest(
                planDate   = planDate,
                mealType   = mealType,
                recipeName = recipeName,
                calories   = ns?.calories?.toInt() ?: 0,
                proteinG   = ns?.proteinG ?: 0f,
                carbsG     = ns?.carbsG ?: 0f,
                fatG       = ns?.fatG ?: 0f,
                notes      = "Saved from Recipes"
            )
            val result = repository.saveMealPlan(userId = userId ?: "", request = request)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "✅ Saved as $mealType for $planDate!") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = "Could not save: ${error.message}") }
                }
            )
        }
    }

    fun generateShoppingList(recipeText: String) {
        _uiState.update { it.copy(isGeneratingShoppingList = true) }
        viewModelScope.launch {
            // Fix: Pass the recipe text as query parameter
            repository.generateShoppingList(
                query = recipeText,
                userId = userId
            ).collect { result ->
                result.fold(
                    onSuccess = { list ->
                        _uiState.update { it.copy(shoppingList = list, isGeneratingShoppingList = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, isGeneratingShoppingList = false) }
                    }
                )
            }
        }
    }

    fun getHealthAdvice(query: String) {
        _uiState.update { it.copy(isGettingHealthAdvice = true) }
        viewModelScope.launch {
            repository.getHealthAdvice(
                query = query,
                userId = userId
            ).collect { result ->
                result.fold(
                    onSuccess = { advice ->
                        _uiState.update { it.copy(healthAdvice = advice, isGettingHealthAdvice = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, isGettingHealthAdvice = false) }
                    }
                )
            }
        }
    }

    fun generateWeeklyPlan(query: String) {
        _uiState.update { it.copy(isGeneratingWeeklyPlan = true) }
        viewModelScope.launch {
            repository.generateWeeklyPlan(
                query = query,
                userId = userId
            ).collect { result ->
                result.fold(
                    onSuccess = { plan ->
                        _uiState.update { it.copy(weeklyPlan = plan, isGeneratingWeeklyPlan = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, isGeneratingWeeklyPlan = false) }
                    }
                )
            }
        }
    }

    fun parseCookingSteps(recipeText: String) {
        viewModelScope.launch {
            try {
                val steps = mutableListOf<CookingStepUi>()
                val stepPattern = Regex("(\\d+)\\.\\s*(.*?)(?=\\n\\d+\\.|\\n\\n|$)", RegexOption.DOT_MATCHES_ALL)
                stepPattern.findAll(recipeText).forEachIndexed { index, match ->
                    val instruction = match.groupValues[2].trim()
                    steps.add(CookingStepUi(
                        stepNumber = index + 1,
                        instruction = instruction,
                        timerSeconds = extractTimer(instruction)
                    ))
                }
                if (steps.isNotEmpty()) {
                    _uiState.update { it.copy(cookingSteps = steps, currentStep = 0, isCookingMode = true) }
                } else {
                    // Fallback: Use the API-based parser
                    parseAndStartCookingSteps(recipeText)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to parse recipe steps: ${e.message}") }
            }
        }
    }

    fun parseAndStartCookingSteps(recipeText: String) {
        viewModelScope.launch {
            repository.parseRecipeSteps(recipeText).collect { result ->
                result.fold(
                    onSuccess = { steps ->
                        val uiSteps = steps.mapIndexed { index, step ->
                            CookingStepUi(
                                stepNumber = step.step,
                                instruction = step.instruction,
                                timerSeconds = step.timerSeconds,
                                isCompleted = step.completed
                            )
                        }
                        _uiState.update { it.copy(cookingSteps = uiSteps, currentStep = 0, isCookingMode = true) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = "Failed to parse steps: ${error.message}") }
                    }
                )
            }
        }
    }

    private fun extractTimer(instruction: String): Int {
        // Check for minutes
        val minMatch = Regex("(\\d+)\\s*(?:min|minute|minutes)").find(instruction.lowercase())
        if (minMatch != null) {
            return minMatch.groupValues[1].toIntOrNull()?.times(60) ?: 0
        }

        // Check for seconds
        val secMatch = Regex("(\\d+)\\s*(?:sec|second|seconds)").find(instruction.lowercase())
        if (secMatch != null) {
            return secMatch.groupValues[1].toIntOrNull() ?: 0
        }

        return 0
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < state.cookingSteps.size - 1) {
                state.copy(currentStep = state.currentStep + 1)
            } else {
                // Complete cooking mode when reaching the last step
                state.copy(isCookingMode = false, successMessage = "🎉 Cooking complete! Enjoy your meal!")
            }
        }
    }

    fun previousStep() {
        _uiState.update { state ->
            if (state.currentStep > 0) state.copy(currentStep = state.currentStep - 1)
            else state
        }
    }

    fun exitCookingMode() {
        _uiState.update { it.copy(isCookingMode = false, cookingSteps = emptyList(), currentStep = 0) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

class RecipeViewModelFactory(private val userId: String) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RecipeViewModel(
            repository = NutriBotRepository(),
            userId = userId
        ) as T
    }
}
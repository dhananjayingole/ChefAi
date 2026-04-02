package eu.tutorials.chefproj.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.BudgetData
import eu.tutorials.chefproj.Data.api.EcoData
import eu.tutorials.chefproj.Data.api.HealthAdviceResponse
import eu.tutorials.chefproj.Data.api.MealPlanSaveRequest
import eu.tutorials.chefproj.Data.api.NutritionData
import eu.tutorials.chefproj.Data.api.RecipeGenerationRequest
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
                eu.tutorials.chefproj.Data.api.RecipeRatingRequest(
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
            val result = repository.saveMealPlan(request)
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
            repository.generateShoppingList("Generate shopping list for this recipe", userId).collect { result ->
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
            repository.getHealthAdvice(query, userId).collect { result ->
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
            repository.generateWeeklyPlan(query, userId).collect { result ->
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
                _uiState.update { it.copy(cookingSteps = steps, currentStep = 0, isCookingMode = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to parse recipe steps") }
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
                                stepNumber = index + 1,
                                instruction = step.instruction,
                                timerSeconds = step.timerSeconds,
                                isCompleted = false
                            )
                        }
                        _uiState.update { it.copy(cookingSteps = uiSteps, currentStep = 0, isCookingMode = true) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                    }
                )
            }
        }
    }

    private fun extractTimer(instruction: String): Int {
        val match = Regex("(\\d+)\\s*(?:min|minute|minutes)").find(instruction.lowercase())
        return match?.groupValues?.get(1)?.toIntOrNull()?.times(60) ?: 0
    }

    fun nextStep() {
        _uiState.update { state ->
            if (state.currentStep < state.cookingSteps.size - 1)
                state.copy(currentStep = state.currentStep + 1)
            else
                state.copy(isCookingMode = false)
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

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSuccess() { _uiState.update { it.copy(successMessage = null) } }
}
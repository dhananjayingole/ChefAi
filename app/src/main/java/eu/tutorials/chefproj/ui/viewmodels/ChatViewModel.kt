package eu.tutorials.chefproj.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.ChatRequest
import eu.tutorials.chefproj.Data.api.MealPlanSaveRequest
import eu.tutorials.chefproj.Data.api.NutritionData
import eu.tutorials.chefproj.Data.api.BudgetData
import eu.tutorials.chefproj.Data.api.EcoData
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sessionId: String? = null,
    val lastGeneratedRecipe: LastRecipeState? = null,
    val saveSuccessMessage: String? = null
)

/** Cached data of the most recently generated recipe so "save this" always works. */
data class LastRecipeState(
    val recipeName: String,
    val recipeContent: String,
    val calories: Int = 0,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val content: String,
    val intent: String? = null,
    val nutrition: NutritionData? = null,
    val budget: BudgetData? = null,
    val eco: EcoData? = null,
    val timestamp: Long = System.currentTimeMillis(),
    /** True for AI recipe messages — shows save buttons */
    val hasRecipe: Boolean = false,
    val recipeName: String? = null,
    val recipeCalories: Int = 0,
    val recipeProtein: Float = 0f,
    val recipeCarbs: Float = 0f,
    val recipeFat: Float = 0f
)

class ChatViewModel(
    private val repository: NutriBotRepository,
    private val userId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // Intercept save-meal requests client-side (no network round-trip needed)
        val saveMealType = detectSaveMealRequest(query)
        if (saveMealType != null) {
            handleSaveMealRequest(query, saveMealType)
            return
        }

        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            isUser = true,
            content = query
        )
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true, error = null) }

        viewModelScope.launch {
            val request = ChatRequest(
                query = query,
                sessionId = _uiState.value.sessionId,
                userId = userId
            )

            repository.chat(request).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        val isRecipeIntent = response.intent in listOf(
                            "recipe_generation", "generate_recipe",
                            "smart_recommendation", "modify_recipe"
                        )

                        val calories = response.nutrition?.perServing?.calories?.toInt() ?: 0
                        val protein  = response.nutrition?.perServing?.proteinG ?: 0f
                        val carbs    = response.nutrition?.perServing?.carbsG   ?: 0f
                        val fat      = response.nutrition?.perServing?.fatG     ?: 0f
                        val rName    = if (isRecipeIntent) extractRecipeName(response.response) else null

                        val aiMessage = ChatMessage(
                            id = (System.currentTimeMillis() + 1).toString(),
                            isUser = false,
                            content = response.response,
                            intent = response.intent,
                            nutrition = response.nutrition,
                            budget = response.budget,
                            eco = response.eco,
                            hasRecipe = isRecipeIntent,
                            recipeName = rName,
                            recipeCalories = calories,
                            recipeProtein = protein,
                            recipeCarbs = carbs,
                            recipeFat = fat
                        )

                        // Always cache the latest recipe
                        val newLastRecipe = if (isRecipeIntent && rName != null) {
                            LastRecipeState(
                                recipeName    = rName,
                                recipeContent = response.response,
                                calories      = calories,
                                proteinG      = protein,
                                carbsG        = carbs,
                                fatG          = fat
                            )
                        } else _uiState.value.lastGeneratedRecipe

                        _uiState.update { s ->
                            s.copy(
                                messages             = s.messages + aiMessage,
                                isLoading            = false,
                                sessionId            = response.sessionId,
                                lastGeneratedRecipe  = newLastRecipe
                            )
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
     * Called when the user taps a save button directly on a recipe bubble.
     * Uses data already available in the message — no guessing needed.
     */
    fun saveRecipeAsMeal(
        mealType: String,
        recipeName: String,
        calories: Int,
        proteinG: Float,
        carbsG: Float,
        fatG: Float
    ) {
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            isUser = true,
            content = "Save \"$recipeName\" as today's ${mealType.replaceFirstChar { it.uppercase() }}"
        )
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true) }

        viewModelScope.launch {
            val result = repository.saveMealPlan(
                MealPlanSaveRequest(
                    planDate  = LocalDate.now().toString(),
                    mealType  = mealType,
                    recipeName = recipeName,
                    calories  = calories,
                    proteinG  = proteinG,
                    carbsG    = carbsG,
                    fatG      = fatG,
                    notes     = "Saved from Chef AI"
                )
            )
            result.fold(
                onSuccess = {
                    val ok = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        isUser = false,
                        content = "✅ **$recipeName** saved as today's ${mealType.replaceFirstChar { it.uppercase() }}!\n\n" +
                                "📊 $calories kcal  •  ${proteinG.toInt()}g protein  •  ${carbsG.toInt()}g carbs  •  ${fatG.toInt()}g fat\n\n" +
                                "Head to the **Nutrition** tab to see your daily totals! 🎯",
                        intent = "save_meal"
                    )
                    _uiState.update { s -> s.copy(messages = s.messages + ok, isLoading = false, saveSuccessMessage = "$recipeName saved!") }
                },
                onFailure = { err ->
                    val errMsg = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        isUser = false,
                        content = "❌ Couldn't save meal: ${err.message}\n\nMake sure the server is running and try again.",
                        intent = "error"
                    )
                    _uiState.update { s -> s.copy(messages = s.messages + errMsg, isLoading = false, error = err.message) }
                }
            )
        }
    }

    // ── Text-based save ("save this for lunch") ───────────────────────────

    private fun handleSaveMealRequest(query: String, mealType: String) {
        val last = _uiState.value.lastGeneratedRecipe
        val userMsg = ChatMessage(id = System.currentTimeMillis().toString(), isUser = true, content = query)

        if (last == null) {
            val noRecipe = ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                isUser = false,
                content = "❌ No recipe to save yet!\n\nAsk me to generate a recipe first — for example:\n*\"Make me palak paneer\"* — then tap **Save as Lunch** or say *\"save this for lunch\"*.",
                intent = "error"
            )
            _uiState.update { s -> s.copy(messages = s.messages + userMsg + noRecipe) }
            return
        }

        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true) }

        viewModelScope.launch {
            val result = repository.saveMealPlan(
                MealPlanSaveRequest(
                    planDate   = LocalDate.now().toString(),
                    mealType   = mealType,
                    recipeName = last.recipeName,
                    calories   = last.calories,
                    proteinG   = last.proteinG,
                    carbsG     = last.carbsG,
                    fatG       = last.fatG,
                    notes      = "Saved from Chef AI"
                )
            )
            result.fold(
                onSuccess = {
                    val ok = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        isUser = false,
                        content = "✅ **${last.recipeName}** saved as today's ${mealType.replaceFirstChar { it.uppercase() }}!\n\n" +
                                "📊 ${last.calories} kcal  •  ${last.proteinG.toInt()}g protein\n\n" +
                                "Check the **Nutrition** tab to see your progress! 🎯",
                        intent = "save_meal"
                    )
                    _uiState.update { s -> s.copy(messages = s.messages + ok, isLoading = false) }
                },
                onFailure = { err ->
                    val errMsg = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        isUser = false,
                        content = "❌ Failed to save: ${err.message}",
                        intent = "error"
                    )
                    _uiState.update { s -> s.copy(messages = s.messages + errMsg, isLoading = false, error = err.message) }
                }
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun detectSaveMealRequest(query: String): String? {
        val q = query.lowercase()
        if (!listOf("save", "log", "track", "add to meal").any { it in q }) return null
        return when {
            "breakfast" in q -> "breakfast"
            "lunch"     in q -> "lunch"
            "dinner"    in q -> "dinner"
            "snack"     in q -> "snack"
            else             -> "meal"
        }
    }

    private fun extractRecipeName(recipe: String): String {
        val header = Regex("""##?\s*[🍽️🍛🥗🍲🥘]?\s*(.+?)(?=\n|$)""").find(recipe)
        if (header != null) {
            val name = header.groupValues[1].replace(Regex("""[*_#🍽️🍛🥗🍲🥘]"""), "").trim()
            if (name.isNotBlank() && name.length < 60) return name
        }
        val first = recipe.lines().firstOrNull { it.isNotBlank() }
            ?.replace(Regex("""[*_#🍽️🍛🥗🍲🥘]"""), "")?.trim()
        if (!first.isNullOrBlank() && first.length < 60) return first
        return "Generated Recipe"
    }

    fun clearError()       { _uiState.update { it.copy(error = null) } }
    fun clearSaveSuccess() { _uiState.update { it.copy(saveSuccessMessage = null) } }
    fun clearChat()        { _uiState.update { ChatUiState() } }
}
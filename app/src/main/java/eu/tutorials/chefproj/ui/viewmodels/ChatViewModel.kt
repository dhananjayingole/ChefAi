package eu.tutorials.chefproj.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.ChatRequest
import eu.tutorials.chefproj.Data.api.MealPlanSaveRequest
import eu.tutorials.chefproj.Data.api.NutritionData
import eu.tutorials.chefproj.Data.api.BudgetData
import eu.tutorials.chefproj.Data.api.EcoData
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val hasRecipe: Boolean = false,
    val recipeName: String? = null,
    val recipeCalories: Int = 0,
    val recipeProtein: Float = 0f,
    val recipeCarbs: Float = 0f,
    val recipeFat: Float = 0f
)

class ChatViewModel(
    private val repository: NutriBotRepository,
    private val userId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun sendMessage(query: String) {
        if (query.isBlank()) return

        val saveMealType = detectSaveMealRequest(query)
        if (saveMealType != null) { handleSaveMealRequest(query, saveMealType); return }

        val userMsg = ChatMessage(id = System.currentTimeMillis().toString(), isUser = true, content = query)
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true, error = null) }

        viewModelScope.launch {
            val request = ChatRequest(
                query     = query,
                sessionId = _uiState.value.sessionId,
                userId    = userId
            )
            repository.chat(request).collect { result ->
                result.fold(
                    onSuccess = { response ->
                        val isRecipe = response.intent in listOf(
                            "recipe_generation", "generate_recipe", "smart_recommendation", "modify_recipe"
                        )
                        val cal   = response.nutrition?.perServing?.calories?.toInt() ?: 0
                        val prot  = response.nutrition?.perServing?.proteinG ?: 0f
                        val carbs = response.nutrition?.perServing?.carbsG   ?: 0f
                        val fat   = response.nutrition?.perServing?.fatG     ?: 0f
                        val rName = if (isRecipe) extractRecipeName(response.response) else null

                        val aiMsg = ChatMessage(
                            id             = (System.currentTimeMillis() + 1).toString(),
                            isUser         = false,
                            content        = response.response,
                            intent         = response.intent,
                            nutrition      = if (isRecipe) response.nutrition else null,
                            budget         = if (isRecipe) response.budget    else null,
                            eco            = if (isRecipe) response.eco       else null,
                            hasRecipe      = isRecipe,
                            recipeName     = rName,
                            recipeCalories = cal,
                            recipeProtein  = prot,
                            recipeCarbs    = carbs,
                            recipeFat      = fat
                        )
                        val newLast = if (isRecipe && rName != null)
                            LastRecipeState(rName, response.response, cal, prot, carbs, fat)
                        else _uiState.value.lastGeneratedRecipe

                        _uiState.update { s ->
                            s.copy(messages = s.messages + aiMsg, isLoading = false,
                                sessionId = response.sessionId, lastGeneratedRecipe = newLast)
                        }
                    },
                    onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
                )
            }
        }
    }

    /** Called from the inline "Save as Dinner" buttons on a recipe bubble. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveRecipeAsMeal(
        mealType: String, recipeName: String,
        calories: Int, proteinG: Float, carbsG: Float, fatG: Float
    ) {
        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(), isUser = true,
            content = "Save \"$recipeName\" as today's ${mealType.replaceFirstChar { it.uppercase() }}"
        )
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true) }

        viewModelScope.launch {
            repository.saveMealPlan(
                userId = userId,
                request = MealPlanSaveRequest(
                    planDate   = LocalDate.now().toString(),
                    mealType   = mealType,
                    recipeName = recipeName,
                    calories   = calories,
                    proteinG   = proteinG,
                    carbsG     = carbsG,
                    fatG       = fatG,
                    notes      = "Saved from Chef AI"
                )
            ).fold(
                onSuccess = {
                    val ok = ChatMessage(
                        id      = (System.currentTimeMillis() + 1).toString(),
                        isUser  = false,
                        intent  = "save_meal",
                        content = "✅ **$recipeName** saved as today's ${mealType.replaceFirstChar { it.uppercase() }}!\n\n" +
                                "📊 $calories kcal  •  ${proteinG.toInt()}g protein  •  ${carbsG.toInt()}g carbs\n\n" +
                                "Check the **Nutrition** tab to see your progress! 🎯"
                    )
                    _uiState.update { s ->
                        s.copy(messages = s.messages + ok, isLoading = false,
                            saveSuccessMessage = "$recipeName saved!")
                    }
                },
                onFailure = { err ->
                    val errMsg = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(), isUser = false,
                        intent = "error",
                        content = "❌ Couldn't save meal: ${err.message}"
                    )
                    _uiState.update { s -> s.copy(messages = s.messages + errMsg, isLoading = false, error = err.message) }
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleSaveMealRequest(query: String, mealType: String) {
        val last = _uiState.value.lastGeneratedRecipe
        val userMsg = ChatMessage(id = System.currentTimeMillis().toString(), isUser = true, content = query)
        if (last == null) {
            val noRecipe = ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(), isUser = false,
                content = "❌ No recipe to save yet! Ask me to generate one first."
            )
            _uiState.update { s -> s.copy(messages = s.messages + userMsg + noRecipe) }
            return
        }
        _uiState.update { it.copy(messages = it.messages + userMsg, isLoading = true) }
        viewModelScope.launch {
            repository.saveMealPlan(
                userId = userId,
                request = MealPlanSaveRequest(
                    planDate = LocalDate.now().toString(), mealType = mealType,
                    recipeName = last.recipeName, calories = last.calories,
                    proteinG = last.proteinG, carbsG = last.carbsG, fatG = last.fatG,
                    notes = "Saved from Chef AI"
                )
            ).fold(
                onSuccess = {
                    val ok = ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(), isUser = false,
                        intent = "save_meal",
                        content = "✅ **${last.recipeName}** saved as today's ${mealType.replaceFirstChar { it.uppercase() }}!\n\nCheck the Nutrition tab 🎯"
                    )
                    _uiState.update { s -> s.copy(messages = s.messages + ok, isLoading = false) }
                },
                onFailure = { err ->
                    _uiState.update { s -> s.copy(isLoading = false, error = err.message) }
                }
            )
        }
    }

    private fun detectSaveMealRequest(query: String): String? {
        val q = query.lowercase()
        if (!listOf("save", "log", "track").any { it in q }) return null
        return when { "breakfast" in q -> "breakfast"; "lunch" in q -> "lunch"
            "dinner" in q -> "dinner"; "snack" in q -> "snack"; else -> null }
    }

    private fun extractRecipeName(recipe: String): String {
        val header = Regex("""##?\s*[🍽️🍛🥗🍲🥘]?\s*(.+?)(?=\n|$)""").find(recipe)
        if (header != null) {
            val name = header.groupValues[1].replace(Regex("""[*_#🍽️🍛🥗🍲🥘]"""), "").trim()
            if (name.isNotBlank() && name.length < 60) return name
        }
        return "Generated Recipe"
    }

    fun clearError()       { _uiState.update { it.copy(error = null) } }
    fun clearSaveSuccess() { _uiState.update { it.copy(saveSuccessMessage = null) } }
    fun clearChat()        { _uiState.update { ChatUiState() } }
}

class ChatViewModelFactory(private val userId: String) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(NutriBotRepository(), userId) as T
    }
}

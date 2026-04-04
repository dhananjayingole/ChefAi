package eu.tutorials.chefproj.Data.repository

import eu.tutorials.chefproj.Data.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * NutriBotRepository v2
 * Every method that reads or writes user data accepts a userId parameter
 * and forwards it to the backend so each user's data stays isolated.
 */
class NutriBotRepository {

    private val apiService = ApiClient.apiService

    // ── Chat ──────────────────────────────────────────────────────────────────

    fun chat(request: ChatRequest): Flow<Result<ChatResponse>> = flow {
        try {
            val response = apiService.chat(request)
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val d = response.body()?.data as? Map<String, Any?>
                val chatResponse = ChatResponse(
                    response        = d?.get("response")   as? String ?: "",
                    intent          = d?.get("intent")     as? String ?: "general",
                    sessionId       = d?.get("session_id") as? String ?: "",
                    nutrition       = (d?.get("nutrition") as? Map<String, Any?>)?.let { parseNutritionData(it) },
                    budget          = (d?.get("budget")    as? Map<String, Any?>)?.let { parseBudgetData(it) },
                    eco             = (d?.get("eco")       as? Map<String, Any?>)?.let { parseEcoData(it) },
                    generatedRecipe = d?.get("generated_recipe") as? String
                )
                emit(Result.success(chatResponse))
            } else {
                emit(Result.failure(Exception(response.body()?.error ?: "Unknown error")))
            }
        } catch (e: IOException) {
            emit(Result.failure(Exception("Network error — is the server running?")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    suspend fun getProfile(userId: String): Result<UserProfile> = runCatching {
        val r = apiService.getProfile(userId)
        check(r.isSuccessful && r.body()?.success == true) {
            r.body()?.error ?: "Failed to get profile"
        }
        @Suppress("UNCHECKED_CAST")
        val m = r.body()?.data as? Map<String, Any?>
        UserProfile(
            userId              = userId,
            dietType            = m?.get("diet_type")    as? String,
            fitnessGoal         = m?.get("fitness_goal") as? String,
            cuisinePreferences  = m?.get("cuisine_preferences") as? List<String>,
            allergies           = m?.get("allergies")    as? List<String>,
            healthConditions    = m?.get("health_conditions") as? List<String>,
            calorieGoal         = (m?.get("calorie_goal") as? Double)?.toInt(),
            budgetPreference    = m?.get("budget_preference") as? Map<String, Any>,
            skillLevel          = m?.get("skill_level")  as? String
        )
    }

    suspend fun updateProfile(userId: String, profile: UserProfile): Result<UserProfile> = runCatching {
        val r = apiService.updateProfile(userId, profile)
        check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Failed" }
        profile
    }

    suspend fun resetProfile(userId: String): Result<Boolean> = runCatching {
        val r = apiService.resetProfile(userId)
        check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Failed" }
        true
    }

    // ── Pantry ────────────────────────────────────────────────────────────────

    fun getPantry(userId: String): Flow<Result<List<GroceryItem>>> = flow {
        try {
            val r = apiService.getPantry(userId)
            if (r.isSuccessful && r.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val items = (r.body()?.data as? Map<String, Any?>)
                    ?.get("items") as? List<Map<String, Any?>>
                emit(Result.success(items?.mapNotNull { parseGroceryItem(it) } ?: emptyList()))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    suspend fun addGrocery(userId: String, item: GroceryItemAddRequest): Result<Boolean> = runCatching {
        val r = apiService.addGrocery(userId, item)
        check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Failed" }
        true
    }

    suspend fun removeGrocery(userId: String, itemName: String): Result<Boolean> = runCatching {
        val r = apiService.removeGrocery(userId, GroceryItemRemoveRequest(itemName))
        check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Not found" }
        true
    }

    suspend fun clearPantry(userId: String): Result<Boolean> = runCatching {
        val r = apiService.clearPantry(userId)
        check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Failed" }
        true
    }

    fun getExpiringItems(userId: String, days: Int = 3): Flow<Result<List<GroceryItem>>> = flow {
        try {
            val r = apiService.getExpiringItems(userId, days)
            if (r.isSuccessful && r.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val items = (r.body()?.data as? Map<String, Any?>)
                    ?.get("items") as? List<Map<String, Any?>>
                emit(Result.success(items?.mapNotNull { parseGroceryItem(it) } ?: emptyList()))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ── Recipes ───────────────────────────────────────────────────────────────

    fun generateRecipe(request: RecipeGenerationRequest): Flow<Result<RecipeGenerationResponse>> = flow {
        try {
            val r = apiService.generateRecipe(request)
            if (r.isSuccessful && r.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val m = r.body()?.data as? Map<String, Any?>
                emit(Result.success(RecipeGenerationResponse(
                    recipe      = m?.get("recipe") as? String ?: "",
                    ingredients = (m?.get("ingredients") as? List<Map<String, Any>>) ?: emptyList(),
                    nutrition   = (m?.get("nutrition") as? Map<String, Any?>)?.let { parseNutritionData(it) },
                    budget      = (m?.get("budget")    as? Map<String, Any?>)?.let { parseBudgetData(it) },
                    ecoScore    = (m?.get("eco_score") as? Map<String, Any?>)?.let { parseEcoData(it) }
                )))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    suspend fun rateRecipe(userId: String, request: RecipeRatingRequest): Result<Boolean> = runCatching {
        val r = apiService.rateRecipe(userId, request)
        check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Failed" }
        true
    }

    // ── Meal Plans ────────────────────────────────────────────────────────────

    fun getMealPlans(userId: String, days: Int = 7): Flow<Result<List<MealPlan>>> = flow {
        try {
            val r = apiService.getMealPlans(userId, days)
            if (r.isSuccessful && r.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val meals = (r.body()?.data as? Map<String, Any?>)
                    ?.get("meals") as? List<Map<String, Any?>>
                emit(Result.success(meals?.mapNotNull { parseMealPlan(it) } ?: emptyList()))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    suspend fun saveMealPlan(userId: String?, request: MealPlanSaveRequest): Result<Boolean> = runCatching {
        val r = apiService.saveMealPlan(userId, request)
        check(r.isSuccessful && r.body()?.success == true) {
            r.body()?.error ?: r.body()?.message ?: "Failed to save meal (HTTP ${r.code()})"
        }
        true
    }

    fun generateWeeklyPlan(query: String, userId: String?): Flow<Result<String>> = flow {
        try {
            val r = apiService.generateWeeklyPlan(WeeklyPlanRequest(query, userId))
            if (r.isSuccessful && r.body()?.success == true) {
                val plan = (r.body()?.data as? Map<*, *>)?.get("plan") as? String ?: ""
                emit(Result.success(plan))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ── Nutrition ─────────────────────────────────────────────────────────────

    fun getTodayNutrition(userId: String): Flow<Result<DailyNutritionSummary>> = flow {
        try {
            val r = apiService.getTodayNutrition(userId)
            if (r.isSuccessful && r.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val s = (r.body()?.data as? Map<String, Any?>)?.get("summary") as? Map<String, Any?>
                emit(Result.success(DailyNutritionSummary(
                    calories = (s?.get("calories") as? Double)?.toInt() ?: 0,
                    proteinG = (s?.get("protein_g") as? Double)?.toFloat() ?: 0f,
                    carbsG   = (s?.get("carbs_g")   as? Double)?.toFloat() ?: 0f,
                    fatG     = (s?.get("fat_g")      as? Double)?.toFloat() ?: 0f,
                )))
            } else emit(Result.failure(Exception("No data")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ── Budget ────────────────────────────────────────────────────────────────

    suspend fun getCheapestProtein(userId: String, dietType: String = "vegetarian"): Result<Map<String, Any>> =
        runCatching {
            val r = apiService.getCheapestProtein(userId, dietType)
            check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Failed" }
            @Suppress("UNCHECKED_CAST")
            (r.body()?.data as? Map<String, Any>) ?: emptyMap()
        }

    // ── Feedback ──────────────────────────────────────────────────────────────

    suspend fun getFeedbackStats(userId: String): Result<FeedbackStats> = runCatching {
        val r = apiService.getFeedbackStats(userId)
        check(r.isSuccessful && r.body()?.success == true) { r.body()?.error ?: "Failed" }
        @Suppress("UNCHECKED_CAST")
        val m = r.body()?.data as? Map<String, Any?>
        FeedbackStats(
            totalRated       = (m?.get("total_rated") as? Double)?.toInt() ?: 0,
            avgRating        = (m?.get("avg_rating")  as? Double)?.toFloat() ?: 0f,
            topCuisines      = emptyList(),
            likedIngredients = emptyList()
        )
    }

    // ── Shopping / cooking / health ───────────────────────────────────────────

    fun generateShoppingList(query: String, userId: String?): Flow<Result<String>> = flow {
        try {
            val r = apiService.generateShoppingList(ShoppingListRequest(query, userId))
            if (r.isSuccessful && r.body()?.success == true) {
                emit(Result.success((r.body()?.data as? Map<*, *>)?.get("shopping_list") as? String ?: ""))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun parseRecipeSteps(recipeText: String): Flow<Result<List<CookingStep>>> = flow {
        try {
            val r = apiService.parseRecipeSteps(ParseStepsRequest(recipeText))
            if (r.isSuccessful && r.body()?.success == true) {
                val steps = (r.body()?.data as? Map<*, *>)?.get("steps") as? List<Map<String, Any>> ?: emptyList()
                emit(Result.success(steps.mapNotNull { step ->
                    try {
                        CookingStep(
                            step         = (step["step"] as? Double)?.toInt() ?: 0,
                            instruction  = step["instruction"] as? String ?: "",
                            timerSeconds = (step["timer_seconds"] as? Double)?.toInt() ?: 0,
                            timerDisplay = step["timer_display"] as? String,
                            completed    = step["completed"] as? Boolean ?: false
                        )
                    } catch (e: Exception) { null }
                }))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun getHealthAdvice(query: String, userId: String?): Flow<Result<HealthAdviceResponse>> = flow {
        try {
            val r = apiService.getHealthAdvice(HealthAdviceRequest(query, userId))
            if (r.isSuccessful && r.body()?.success == true) {
                val m = r.body()?.data as? Map<*, *>
                emit(Result.success(HealthAdviceResponse(
                    advice          = m?.get("advice")          as? String ?: "",
                    recommendations = m?.get("recommendations") as? String
                )))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ── Private parsers ───────────────────────────────────────────────────────

    private fun parseGroceryItem(item: Map<String, Any?>): GroceryItem? {
        return try {
            GroceryItem(
                itemName     = item["item_name"] as? String ?: return null,
                quantity     = (item["quantity"] as? Double)?.toFloat() ?: 0f,
                unit         = item["unit"] as? String ?: "pieces",
                category     = item["category"] as? String,
                isPerishable = item["is_perishable"] as? Boolean ?: false,
                expiryDate   = item["expiry_date"] as? String
            )
        } catch (e: Exception) { null }
    }

    private fun parseMealPlan(m: Map<String, Any?>): MealPlan? = try {
        MealPlan(
            id         = (m["id"] as? Double)?.toInt() ?: 0,
            planDate   = m["plan_date"]   as? String ?: "",
            mealType   = m["meal_type"]   as? String ?: "",
            recipeName = m["recipe_name"] as? String ?: "",
            calories   = (m["calories"]  as? Double)?.toInt() ?: 0,
            proteinG   = (m["protein_g"] as? Double)?.toFloat() ?: 0f,
            carbsG     = (m["carbs_g"]   as? Double)?.toFloat() ?: 0f,
            fatG       = (m["fat_g"]     as? Double)?.toFloat() ?: 0f,
            notes      = m["notes"] as? String
        )
    } catch (e: Exception) { null }

    private fun parseNutritionData(m: Map<String, Any?>): NutritionData? = try {
        val ps = m["per_serving"] as? Map<String, Any?>
        NutritionData(
            perServing = ps?.let {
                PerServing(
                    calories = (it["calories"]  as? Double)?.toFloat() ?: 0f,
                    proteinG = (it["protein_g"] as? Double)?.toFloat() ?: 0f,
                    carbsG   = (it["carbs_g"]   as? Double)?.toFloat() ?: 0f,
                    fatG     = (it["fat_g"]     as? Double)?.toFloat() ?: 0f,
                    fiberG   = (it["fiber_g"]   as? Double)?.toFloat() ?: 0f,
                    sodiumMg = (it["sodium_mg"] as? Double)?.toFloat() ?: 0f,
                )
            },
            accuracyPct      = (m["accuracy_pct"]      as? Double)?.toFloat() ?: 0f,
            usdaMatched      = (m["usda_matched"]       as? Double)?.toInt()   ?: 0,
            totalIngredients = (m["total_ingredients"]  as? Double)?.toInt()   ?: 0,
        )
    } catch (e: Exception) { null }

    private fun parseBudgetData(m: Map<String, Any?>): BudgetData? = try {
        BudgetData(
            totalCost    = (m["total_cost"]   as? Double)?.toFloat() ?: 0f,
            perServing   = (m["per_serving"]  as? Double)?.toFloat() ?: 0f,
            budgetLimit  = (m["budget_limit"] as? Double)?.toFloat() ?: 500f,
            withinBudget =  m["within_budget"] as? Boolean ?: true,
            status       =  m["status"]   as? String ?: "",
            currency     =  m["currency"] as? String ?: "₹",
        )
    } catch (e: Exception) { null }

    private fun parseEcoData(m: Map<String, Any?>): EcoData? = try {
        EcoData(
            score      = (m["score"]        as? Double)?.toFloat() ?: 0f,
            grade      =  m["grade"]        as? String ?: "C",
            co2Kg      = (m["co2_kg"]       as? Double)?.toFloat() ?: 0f,
            co2SavedKg = (m["co2_saved_kg"] as? Double)?.toFloat() ?: 0f,
            tip        =  m["tip"]          as? String ?: "",
        )
    } catch (e: Exception) { null }
}

data class RecipeGenerationResponse(
    val recipe: String,
    val ingredients: List<Map<String, Any>>,
    val nutrition: NutritionData?,
    val budget: BudgetData?,
    val ecoScore: EcoData?,
)
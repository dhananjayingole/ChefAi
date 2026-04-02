package eu.tutorials.chefproj.Data.repository

import eu.tutorials.chefproj.Data.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class NutriBotRepository {

    private val apiService = ApiClient.apiService

    // ── Chat ──────────────────────────────────────────────────────────────

    fun chat(request: ChatRequest): Flow<Result<ChatResponse>> = flow {
        try {
            val response = apiService.chat(request)
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val dataMap = response.body()?.data as? Map<String, Any?>
                val nutritionMap = dataMap?.get("nutrition") as? Map<String, Any?>
                val budgetMap    = dataMap?.get("budget")    as? Map<String, Any?>
                val ecoMap       = dataMap?.get("eco")       as? Map<String, Any?>

                val chatResponse = ChatResponse(
                    response  = dataMap?.get("response") as? String ?: "",
                    intent    = dataMap?.get("intent")   as? String ?: "general",
                    sessionId = dataMap?.get("session_id") as? String ?: "",
                    nutrition = nutritionMap?.let { parseNutritionData(it) },
                    budget    = budgetMap?.let    { parseBudgetData(it) },
                    eco       = ecoMap?.let       { parseEcoData(it) },
                    generatedRecipe = dataMap?.get("generated_recipe") as? String
                )
                emit(Result.success(chatResponse))
            } else {
                emit(Result.failure(Exception(response.body()?.error ?: "Unknown error")))
            }
        } catch (e: IOException) {
            emit(Result.failure(Exception("Network error: check server connection")))
        } catch (e: HttpException) {
            emit(Result.failure(Exception("Server error: ${e.code()}")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ── Meal Plans ────────────────────────────────────────────────────────

    /**
     * Save a meal plan entry to the server.
     * This is the key method that makes "save recipe" work.
     */
    suspend fun saveMealPlan(request: MealPlanSaveRequest): Result<Boolean> {
        return try {
            val response = apiService.saveMealPlan(request)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                val err = response.body()?.error
                    ?: response.body()?.message
                    ?: "Failed to save meal (HTTP ${response.code()})"
                Result.failure(Exception(err))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: make sure the server is running"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getMealPlans(days: Int = 7): Flow<Result<List<MealPlan>>> = flow {
        try {
            val response = apiService.getMealPlans(days)
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val dataMap = response.body()?.data as? Map<String, Any?>
                val meals   = dataMap?.get("meals") as? List<Map<String, Any?>>

                val mealPlans = meals?.mapNotNull { meal ->
                    try {
                        MealPlan(
                            id         = (meal["id"] as? Double)?.toInt() ?: 0,
                            planDate   = meal["plan_date"]   as? String ?: "",
                            mealType   = meal["meal_type"]   as? String ?: "",
                            recipeName = meal["recipe_name"] as? String ?: "",
                            calories   = (meal["calories"]   as? Double)?.toInt() ?: 0,
                            proteinG   = (meal["protein_g"]  as? Double)?.toFloat() ?: 0f,
                            carbsG     = (meal["carbs_g"]    as? Double)?.toFloat() ?: 0f,
                            fatG       = (meal["fat_g"]      as? Double)?.toFloat() ?: 0f,
                            notes      = meal["notes"] as? String
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()

                emit(Result.success(mealPlans))
            } else {
                emit(Result.failure(Exception(response.body()?.error ?: "Failed to get meal plans")))
            }
        } catch (e: IOException) {
            emit(Result.failure(Exception("Network error: ${e.message}")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // ── Profile ───────────────────────────────────────────────────────────

    suspend fun getProfile(userId: String): Result<UserProfile> {
        return try {
            val response = apiService.getProfile(userId)
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val m = response.body()?.data as? Map<String, Any?>
                Result.success(UserProfile(
                    userId             = userId,
                    dietType           = m?.get("diet_type") as? String,
                    fitnessGoal        = m?.get("fitness_goal") as? String,
                    cuisinePreferences = m?.get("cuisine_preferences") as? List<String>,
                    allergies          = m?.get("allergies") as? List<String>,
                    healthConditions   = m?.get("health_conditions") as? List<String>,
                    calorieGoal        = (m?.get("calorie_goal") as? Double)?.toInt(),
                    budgetPreference   = m?.get("budget_preference") as? Map<String, Any>,
                    skillLevel         = m?.get("skill_level") as? String
                ))
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to get profile"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateProfile(userId: String, profile: UserProfile): Result<UserProfile> {
        return try {
            val response = apiService.updateProfile(userId, profile)
            if (response.isSuccessful && response.body()?.success == true) Result.success(profile)
            else Result.failure(Exception(response.body()?.error ?: "Failed to update profile"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun resetProfile(userId: String): Result<Boolean> {
        return try {
            val response = apiService.resetProfile(userId)
            if (response.isSuccessful && response.body()?.success == true) Result.success(true)
            else Result.failure(Exception(response.body()?.error ?: "Failed to reset profile"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Pantry ────────────────────────────────────────────────────────────

    fun getPantry(): Flow<Result<List<GroceryItem>>> = flow {
        try {
            val response = apiService.getPantry()
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val items = (response.body()?.data as? Map<String, Any?>)?.get("items") as? List<Map<String, Any?>>
                val groceries = items?.mapNotNull { item ->
                    try {
                        GroceryItem(
                            itemName    = item["item_name"] as? String ?: "",
                            quantity    = (item["quantity"] as? Double)?.toFloat() ?: 0f,
                            unit        = item["unit"] as? String ?: "pieces",
                            category    = item["category"] as? String,
                            isPerishable= item["is_perishable"] as? Boolean ?: false,
                            expiryDate  = item["expiry_date"] as? String
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                emit(Result.success(groceries))
            } else emit(Result.failure(Exception(response.body()?.error ?: "Failed to get pantry")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    suspend fun addGrocery(item: GroceryItemAddRequest): Result<Boolean> {
        return try {
            val r = apiService.addGrocery(item)
            if (r.isSuccessful && r.body()?.success == true) Result.success(true)
            else Result.failure(Exception(r.body()?.error ?: "Failed to add item"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun removeGrocery(itemName: String): Result<Boolean> {
        return try {
            val r = apiService.removeGrocery(GroceryItemRemoveRequest(itemName))
            if (r.isSuccessful && r.body()?.success == true) Result.success(true)
            else Result.failure(Exception(r.body()?.error ?: "Failed to remove item"))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun clearPantry(): Result<Boolean> {
        return try {
            val r = apiService.clearPantry()
            if (r.isSuccessful && r.body()?.success == true) Result.success(true)
            else Result.failure(Exception(r.body()?.error ?: "Failed to clear pantry"))
        } catch (e: Exception) { Result.failure(e) }
    }

    fun getExpiringItems(days: Int = 3): Flow<Result<List<GroceryItem>>> = flow {
        try {
            val response = apiService.getExpiringItems(days)
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val items = (response.body()?.data as? Map<String, Any?>)?.get("items") as? List<Map<String, Any?>>
                val groceries = items?.mapNotNull { item ->
                    try {
                        GroceryItem(
                            itemName    = item["item_name"] as? String ?: "",
                            quantity    = (item["quantity"] as? Double)?.toFloat() ?: 0f,
                            unit        = item["unit"] as? String ?: "pieces",
                            category    = item["category"] as? String,
                            isPerishable= item["is_perishable"] as? Boolean ?: false,
                            expiryDate  = item["expiry_date"] as? String
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                emit(Result.success(groceries))
            } else emit(Result.failure(Exception(response.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ── Recipes ───────────────────────────────────────────────────────────

    fun generateRecipe(request: RecipeGenerationRequest): Flow<Result<RecipeGenerationResponse>> = flow {
        try {
            val response = apiService.generateRecipe(request)
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val m = response.body()?.data as? Map<String, Any?>
                emit(Result.success(RecipeGenerationResponse(
                    recipe      = m?.get("recipe") as? String ?: "",
                    ingredients = (m?.get("ingredients") as? List<Map<String, Any>>) ?: emptyList(),
                    nutrition   = (m?.get("nutrition") as? Map<String, Any?>)?.let { parseNutritionData(it) },
                    budget      = (m?.get("budget") as? Map<String, Any?>)?.let { parseBudgetData(it) },
                    ecoScore    = (m?.get("eco_score") as? Map<String, Any?>)?.let { parseEcoData(it) }
                )))
            } else emit(Result.failure(Exception(response.body()?.error ?: "Failed to generate recipe")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    suspend fun rateRecipe(request: RecipeRatingRequest): Result<Boolean> {
        return try {
            val r = apiService.rateRecipe(request)
            if (r.isSuccessful && r.body()?.success == true) Result.success(true)
            else Result.failure(Exception(r.body()?.error ?: "Failed to rate"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Nutrition ─────────────────────────────────────────────────────────

    fun getTodayNutrition(): Flow<Result<DailyNutritionSummary>> = flow {
        try {
            val response = apiService.getTodayNutrition()
            if (response.isSuccessful && response.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val summary = (response.body()?.data as? Map<String, Any?>)?.get("summary") as? Map<String, Any?>
                emit(Result.success(DailyNutritionSummary(
                    calories = (summary?.get("calories") as? Double)?.toInt() ?: 0,
                    proteinG = (summary?.get("protein_g") as? Double)?.toFloat() ?: 0f,
                    carbsG   = (summary?.get("carbs_g")   as? Double)?.toFloat() ?: 0f,
                    fatG     = (summary?.get("fat_g")     as? Double)?.toFloat() ?: 0f
                )))
            } else emit(Result.failure(Exception("No nutrition data")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ── Budget ────────────────────────────────────────────────────────────

    suspend fun getCheapestProtein(dietType: String = "vegetarian"): Result<Map<String, Any>> {
        return try {
            val r = apiService.getCheapestProtein(dietType)
            if (r.isSuccessful && r.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                Result.success((r.body()?.data as? Map<String, Any>) ?: emptyMap())
            } else Result.failure(Exception(r.body()?.error ?: "Failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Feedback ──────────────────────────────────────────────────────────

    suspend fun getFeedbackStats(): Result<FeedbackStats> {
        return try {
            val r = apiService.getFeedbackStats()
            if (r.isSuccessful && r.body()?.success == true) {
                @Suppress("UNCHECKED_CAST")
                val m = r.body()?.data as? Map<String, Any?>
                Result.success(FeedbackStats(
                    totalRated        = (m?.get("total_rated") as? Double)?.toInt() ?: 0,
                    avgRating         = (m?.get("avg_rating")  as? Double)?.toFloat() ?: 0f,
                    topCuisines       = emptyList(),
                    likedIngredients  = emptyList()
                ))
            } else Result.failure(Exception(r.body()?.error ?: "Failed"))
        } catch (e: Exception) { Result.failure(e) }
    }

    // ── Shopping / Eco / Health / Weekly ─────────────────────────────────

    fun generateShoppingList(query: String, userId: String? = null): Flow<Result<String>> = flow {
        try {
            val r = apiService.generateShoppingList(ShoppingListRequest(query, userId))
            if (r.isSuccessful && r.body()?.success == true) {
                val list = (r.body()?.data as? Map<*, *>)?.get("shopping_list") as? String ?: ""
                emit(Result.success(list))
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

    fun getHealthAdvice(query: String, userId: String? = null): Flow<Result<HealthAdviceResponse>> = flow {
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

    fun calculateEcoScore(ingredients: List<Map<String, Any>>): Flow<Result<EcoScoreResponse>> = flow {
        try {
            val r = apiService.calculateEcoScore(EcoScoreRequest(ingredients))
            if (r.isSuccessful && r.body()?.success == true) {
                val m = r.body()?.data as? Map<*, *>
                emit(Result.success(EcoScoreResponse(
                    score      = (m?.get("score")        as? Double)?.toFloat() ?: 0f,
                    grade      =  m?.get("grade")        as? String ?: "C",
                    co2Kg      = (m?.get("co2_kg")       as? Double)?.toFloat() ?: 0f,
                    co2SavedKg = (m?.get("co2_saved_kg") as? Double)?.toFloat() ?: 0f,
                    tip        =  m?.get("tip")          as? String ?: ""
                )))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun generateWeeklyPlan(query: String, userId: String? = null): Flow<Result<String>> = flow {
        try {
            val r = apiService.generateWeeklyPlan(WeeklyPlanRequest(query, userId))
            if (r.isSuccessful && r.body()?.success == true) {
                val plan = (r.body()?.data as? Map<*, *>)?.get("plan") as? String ?: ""
                emit(Result.success(plan))
            } else emit(Result.failure(Exception(r.body()?.error ?: "Failed")))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // ── Parsers ───────────────────────────────────────────────────────────

    private fun parseNutritionData(m: Map<String, Any?>): NutritionData? {
        return try {
            val ps = m["per_serving"] as? Map<String, Any?>
            NutritionData(
                perServing = ps?.let {
                    PerServing(
                        calories  = (it["calories"]   as? Double)?.toFloat() ?: 0f,
                        proteinG  = (it["protein_g"]  as? Double)?.toFloat() ?: 0f,
                        carbsG    = (it["carbs_g"]    as? Double)?.toFloat() ?: 0f,
                        fatG      = (it["fat_g"]      as? Double)?.toFloat() ?: 0f,
                        fiberG    = (it["fiber_g"]    as? Double)?.toFloat() ?: 0f,
                        sodiumMg  = (it["sodium_mg"]  as? Double)?.toFloat() ?: 0f
                    )
                },
                accuracyPct = (m["accuracy_pct"] as? Double)?.toFloat() ?: 0f,
                usdaMatched = (m["usda_matched"] as? Double)?.toInt() ?: 0,
                totalIngredients = (m["total_ingredients"] as? Double)?.toInt() ?: 0
            )
        } catch (e: Exception) { null }
    }

    private fun parseBudgetData(m: Map<String, Any?>): BudgetData? {
        return try {
            BudgetData(
                totalCost    = (m["total_cost"]   as? Double)?.toFloat() ?: 0f,
                perServing   = (m["per_serving"]  as? Double)?.toFloat() ?: 0f,
                budgetLimit  = (m["budget_limit"] as? Double)?.toFloat() ?: 500f,
                withinBudget =  m["within_budget"] as? Boolean ?: true,
                status       =  m["status"]  as? String ?: "",
                currency     =  m["currency"] as? String ?: "₹"
            )
        } catch (e: Exception) { null }
    }

    private fun parseEcoData(m: Map<String, Any?>): EcoData? {
        return try {
            EcoData(
                score      = (m["score"]        as? Double)?.toFloat() ?: 0f,
                grade      =  m["grade"]        as? String ?: "C",
                co2Kg      = (m["co2_kg"]       as? Double)?.toFloat() ?: 0f,
                co2SavedKg = (m["co2_saved_kg"] as? Double)?.toFloat() ?: 0f,
                tip        =  m["tip"]          as? String ?: ""
            )
        } catch (e: Exception) { null }
    }
}

data class RecipeGenerationResponse(
    val recipe: String,
    val ingredients: List<Map<String, Any>>,
    val nutrition: NutritionData?,
    val budget: BudgetData?,
    val ecoScore: EcoData?
)
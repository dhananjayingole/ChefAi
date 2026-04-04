package eu.tutorials.chefproj.Data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── System ────────────────────────────────────────────────────────────────
    @GET("/")
    suspend fun root(): Response<APIResponse>

    @GET("/health")
    suspend fun healthCheck(): Response<Map<String, String>>

    // ── Chat ──────────────────────────────────────────────────────────────────
    @POST("/chat")
    suspend fun chat(@Body request: ChatRequest): Response<APIResponse>

    // ── Profile ───────────────────────────────────────────────────────────────
    @GET("/profile/{user_id}")
    suspend fun getProfile(@Path("user_id") userId: String): Response<APIResponse>

    @PUT("/profile/{user_id}")
    suspend fun updateProfile(
        @Path("user_id") userId: String,
        @Body profile: UserProfile
    ): Response<APIResponse>

    @DELETE("/profile/{user_id}")
    suspend fun resetProfile(@Path("user_id") userId: String): Response<APIResponse>

    // ── Pantry  (user_id as query param so GET/DELETE can carry it) ───────────
    @GET("/pantry")
    suspend fun getPantry(@Query("user_id") userId: String): Response<APIResponse>

    @POST("/pantry")
    suspend fun addGrocery(
        @Query("user_id") userId: String,
        @Body item: GroceryItemAddRequest
    ): Response<APIResponse>

    @DELETE("/pantry")
    suspend fun removeGrocery(
        @Query("user_id") userId: String,
        @Body item: GroceryItemRemoveRequest
    ): Response<APIResponse>

    @DELETE("/pantry/all")
    suspend fun clearPantry(@Query("user_id") userId: String): Response<APIResponse>

    @GET("/pantry/expiring")
    suspend fun getExpiringItems(
        @Query("user_id") userId: String,
        @Query("days") days: Int = 3
    ): Response<APIResponse>

    // ── Recipes ───────────────────────────────────────────────────────────────
    @POST("/recipe/generate")
    suspend fun generateRecipe(@Body request: RecipeGenerationRequest): Response<APIResponse>

    @POST("/recipe/rate")
    suspend fun rateRecipe(
        @Query("user_id") userId: String,
        @Body request: RecipeRatingRequest
    ): Response<APIResponse>

    // ── Meal Plans ────────────────────────────────────────────────────────────
    @GET("/mealplan")
    suspend fun getMealPlans(
        @Query("user_id") userId: String,
        @Query("days") days: Int = 7
    ): Response<APIResponse>

    @GET("/mealplan/today")
    suspend fun getTodayMealPlans(@Query("user_id") userId: String): Response<APIResponse>

    @POST("/mealplan")
    suspend fun saveMealPlan(
        @Query("user_id") userId: String?,
        @Body request: MealPlanSaveRequest
    ): Response<APIResponse>

    @POST("/mealplan/week")
    suspend fun generateWeeklyPlan(@Body request: WeeklyPlanRequest): Response<APIResponse>

    // ── Nutrition ─────────────────────────────────────────────────────────────
    @GET("/nutrition/today")
    suspend fun getTodayNutrition(@Query("user_id") userId: String): Response<APIResponse>

    @GET("/nutrition/week")
    suspend fun getWeeklyNutrition(@Query("user_id") userId: String): Response<APIResponse>

    // ── Budget ────────────────────────────────────────────────────────────────
    @GET("/budget/cheapest-protein")
    suspend fun getCheapestProtein(
        @Query("user_id") userId: String,
        @Query("diet_type") dietType: String = "vegetarian"
    ): Response<APIResponse>

    @GET("/budget/prices")
    suspend fun getAllPrices(@Query("user_id") userId: String): Response<APIResponse>

    @GET("/budget/price/{ingredient}")
    suspend fun getIngredientPrice(
        @Path("ingredient") ingredient: String,
        @Query("user_id") userId: String,
        @Query("quantity_kg") quantityKg: Float = 1f
    ): Response<APIResponse>

    // ── Vision ────────────────────────────────────────────────────────────────
    @Multipart
    @POST("/vision/analyze")
    suspend fun analyzeImage(
        @Part file: MultipartBody.Part,
        @Part("context") context: okhttp3.RequestBody,
        @Part("user_id") userId: okhttp3.RequestBody
    ): Response<APIResponse>

    // ── Voice ─────────────────────────────────────────────────────────────────
    @Multipart
    @POST("/voice/transcribe")
    suspend fun transcribeAudio(@Part file: MultipartBody.Part): Response<APIResponse>

    // ── Shopping ──────────────────────────────────────────────────────────────
    @POST("/shopping/generate")
    suspend fun generateShoppingList(@Body request: ShoppingListRequest): Response<APIResponse>

    // ── Cooking ───────────────────────────────────────────────────────────────
    @POST("/cooking/parse")
    suspend fun parseRecipeSteps(@Body request: ParseStepsRequest): Response<APIResponse>

    // ── Eco ───────────────────────────────────────────────────────────────────
    @POST("/eco/calculate")
    suspend fun calculateEcoScore(
        @Query("user_id") userId: String,
        @Body request: EcoScoreRequest
    ): Response<APIResponse>

    // ── Health ────────────────────────────────────────────────────────────────
    @POST("/health/advice")
    suspend fun getHealthAdvice(@Body request: HealthAdviceRequest): Response<APIResponse>

    // ── Feedback ──────────────────────────────────────────────────────────────
    @GET("/feedback/stats")
    suspend fun getFeedbackStats(@Query("user_id") userId: String): Response<APIResponse>

    @GET("/feedback/top-cuisines")
    suspend fun getTopCuisines(
        @Query("user_id") userId: String,
        @Query("min_ratings") minRatings: Int = 1
    ): Response<APIResponse>

    @GET("/feedback/liked-ingredients")
    suspend fun getLikedIngredients(
        @Query("user_id") userId: String,
        @Query("min_likes") minLikes: Int = 2
    ): Response<APIResponse>
}
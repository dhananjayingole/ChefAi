package eu.tutorials.chefproj.Data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // System
    @GET("/")
    suspend fun root(): Response<APIResponse>

    @GET("/health")
    suspend fun healthCheck(): Response<Map<String, String>>

    // Chat
    @POST("/chat")
    suspend fun chat(@Body request: ChatRequest): Response<APIResponse>

    // Profile
    @GET("/profile/{user_id}")
    suspend fun getProfile(@Path("user_id") userId: String): Response<APIResponse>

    @PUT("/profile/{user_id}")
    suspend fun updateProfile(
        @Path("user_id") userId: String,
        @Body profile: UserProfile
    ): Response<APIResponse>

    @DELETE("/profile/{user_id}")
    suspend fun resetProfile(@Path("user_id") userId: String): Response<APIResponse>

    // Pantry
    @GET("/pantry")
    suspend fun getPantry(): Response<APIResponse>

    @POST("/pantry")
    suspend fun addGrocery(@Body item: GroceryItemAddRequest): Response<APIResponse>

    @DELETE("/pantry")
    suspend fun removeGrocery(@Body item: GroceryItemRemoveRequest): Response<APIResponse>

    @DELETE("/pantry/all")
    suspend fun clearPantry(): Response<APIResponse>

    @GET("/pantry/expiring")
    suspend fun getExpiringItems(@Query("days") days: Int = 3): Response<APIResponse>

    // Recipes
    @POST("/recipe/generate")
    suspend fun generateRecipe(@Body request: RecipeGenerationRequest): Response<APIResponse>

    @POST("/recipe/rate")
    suspend fun rateRecipe(@Body request: RecipeRatingRequest): Response<APIResponse>

    // Meal Plans
    @GET("/mealplan")
    suspend fun getMealPlans(@Query("days") days: Int = 7): Response<APIResponse>

    @GET("/mealplan/today")
    suspend fun getTodayMealPlans(): Response<APIResponse>

    @POST("/mealplan")
    suspend fun saveMealPlan(@Body request: MealPlanSaveRequest): Response<APIResponse>

    @POST("/mealplan/week")
    suspend fun generateWeeklyPlan(@Body request: WeeklyPlanRequest): Response<APIResponse>

    // Nutrition
    @GET("/nutrition/today")
    suspend fun getTodayNutrition(): Response<APIResponse>

    @GET("/nutrition/week")
    suspend fun getWeeklyNutrition(): Response<APIResponse>

    // Budget
    @GET("/budget/cheapest-protein")
    suspend fun getCheapestProtein(@Query("diet_type") dietType: String = "vegetarian"): Response<APIResponse>

    @GET("/budget/prices")
    suspend fun getAllPrices(): Response<APIResponse>

    @GET("/budget/price/{ingredient}")
    suspend fun getIngredientPrice(
        @Path("ingredient") ingredient: String,
        @Query("quantity_kg") quantityKg: Float = 1f
    ): Response<APIResponse>

    @POST("/budget/price")
    suspend fun updateIngredientPrice(
        @Query("ingredient") ingredient: String,
        @Query("price") price: Float,
        @Query("source") source: String = "android"
    ): Response<APIResponse>

    // Vision
    @Multipart
    @POST("/vision/analyze")
    suspend fun analyzeImage(
        @Part("file") file: MultipartBody.Part,
        @Part("context") context: String = "fridge"
    ): Response<APIResponse>

    // Voice
    @Multipart
    @POST("/voice/transcribe")
    suspend fun transcribeAudio(
        @Part("file") file: MultipartBody.Part
    ): Response<APIResponse>

    // Shopping
    @POST("/shopping/generate")
    suspend fun generateShoppingList(@Body request: ShoppingListRequest): Response<APIResponse>

    // Cooking
    @POST("/cooking/parse")
    suspend fun parseRecipeSteps(@Body request: ParseStepsRequest): Response<APIResponse>

    // Eco
    @POST("/eco/calculate")
    suspend fun calculateEcoScore(@Body request: EcoScoreRequest): Response<APIResponse>

    // Health
    @POST("/health/advice")
    suspend fun getHealthAdvice(@Body request: HealthAdviceRequest): Response<APIResponse>

    // Feedback
    @GET("/feedback/stats")
    suspend fun getFeedbackStats(): Response<APIResponse>

    @GET("/feedback/top-cuisines")
    suspend fun getTopCuisines(@Query("min_ratings") minRatings: Int = 1): Response<APIResponse>

    @GET("/feedback/liked-ingredients")
    suspend fun getLikedIngredients(@Query("min_likes") minLikes: Int = 2): Response<APIResponse>
}
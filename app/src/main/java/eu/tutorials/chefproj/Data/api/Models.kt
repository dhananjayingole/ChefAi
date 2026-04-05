package eu.tutorials.chefproj.Data.api

import com.google.gson.annotations.SerializedName

// Base Response
data class APIResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null,
    val error: String? = null
)

// Chat Models
data class ChatRequest(
    val query: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("dietary_restrictions") val dietaryRestrictions: List<String>? = null,
    @SerializedName("health_conditions") val healthConditions: List<String>? = null,
    @SerializedName("calorie_limit") val calorieLimit: Int = 500,
    @SerializedName("budget_limit") val budgetLimit: Float = 500f,
    val servings: Int = 2,
    @SerializedName("cuisine_preference") val cuisinePreference: String = "Indian"
)

data class ChatResponse(
    val response: String,
    val intent: String,
    @SerializedName("session_id") val sessionId: String,
    val nutrition: NutritionData? = null,
    val budget: BudgetData? = null,
    val eco: EcoData? = null,
    @SerializedName("generated_recipe") val generatedRecipe: String? = null
)

// Nutrition Models
data class NutritionData(
    @SerializedName("per_serving") val perServing: PerServing? = null,
    val total: TotalNutrition? = null,
    @SerializedName("usda_matched") val usdaMatched: Int = 0,
    @SerializedName("total_ingredients") val totalIngredients: Int = 0,
    @SerializedName("accuracy_pct") val accuracyPct: Float = 0f
)

data class PerServing(
    val calories: Float = 0f,
    @SerializedName("protein_g") val proteinG: Float = 0f,
    @SerializedName("carbs_g") val carbsG: Float = 0f,
    @SerializedName("fat_g") val fatG: Float = 0f,
    @SerializedName("fiber_g") val fiberG: Float = 0f,
    @SerializedName("sodium_mg") val sodiumMg: Float = 0f
)

data class TotalNutrition(
    val calories: Float = 0f,
    @SerializedName("protein_g") val proteinG: Float = 0f,
    @SerializedName("carbs_g") val carbsG: Float = 0f,
    @SerializedName("fat_g") val fatG: Float = 0f
)

// Budget Models
data class BudgetData(
    @SerializedName("total_cost") val totalCost: Float = 0f,
    @SerializedName("per_serving") val perServing: Float = 0f,
    @SerializedName("budget_limit") val budgetLimit: Float = 0f,
    @SerializedName("within_budget") val withinBudget: Boolean = false,
    val status: String = "",
    val currency: String = "₹"
)

// Eco Models
data class EcoData(
    val score: Float = 0f,
    val grade: String = "C",
    @SerializedName("co2_kg") val co2Kg: Float = 0f,
    @SerializedName("co2_saved_kg") val co2SavedKg: Float = 0f,
    val tip: String = ""
)

// Pantry Models
data class GroceryItem(
    @SerializedName("item_name") val itemName: String,
    val quantity: Float = 1f,
    val unit: String = "pieces",
    val category: String? = null,
    @SerializedName("is_perishable") val isPerishable: Boolean = false,
    @SerializedName("expiry_date") val expiryDate: String? = null
)

data class GroceryItemAddRequest(
    @SerializedName("item_name") val itemName: String,
    val quantity: Float = 1f,
    val unit: String = "pieces",
    val category: String? = null,
    @SerializedName("is_perishable") val isPerishable: Boolean = false
)

data class GroceryItemRemoveRequest(
    @SerializedName("item_name") val itemName: String
)

// Recipe Models
data class RecipeGenerationRequest(
    val query: String,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("dietary_restrictions") val dietaryRestrictions: List<String>? = null,
    @SerializedName("health_conditions") val healthConditions: List<String>? = null,
    @SerializedName("calorie_limit") val calorieLimit: Int = 500,
    @SerializedName("budget_limit") val budgetLimit: Float = 500f,
    val servings: Int = 2,
    @SerializedName("cuisine_preference") val cuisinePreference: String = "Indian"
)

data class RecipeRatingRequest(
    @SerializedName("recipe_name") val recipeName: String,
    val rating: Int,
    val feedback: String? = null,
    val cuisine: String? = null
)

// Meal Plan Models
data class MealPlanSaveRequest(
    @SerializedName("plan_date") val planDate: String,
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("recipe_name") val recipeName: String,
    val calories: Int = 0,
    @SerializedName("protein_g") val proteinG: Float = 0f,
    @SerializedName("carbs_g") val carbsG: Float = 0f,
    @SerializedName("fat_g") val fatG: Float = 0f,
    val notes: String? = null
)

data class MealPlan(
    val id: Int,
    @SerializedName("plan_date") val planDate: String,
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("recipe_name") val recipeName: String,
    val calories: Int = 0,
    @SerializedName("protein_g") val proteinG: Float = 0f,
    @SerializedName("carbs_g") val carbsG: Float = 0f,
    @SerializedName("fat_g") val fatG: Float = 0f,
    val notes: String? = null
)

// User Profile Models
data class UserProfile(
    @SerializedName("user_id") val userId: String?,
    @SerializedName("diet_type") val dietType: String? = null,
    @SerializedName("fitness_goal") val fitnessGoal: String? = null,
    @SerializedName("cuisine_preferences") val cuisinePreferences: List<String>? = null,
    val allergies: List<String>? = null,
    @SerializedName("health_conditions") val healthConditions: List<String>? = null,
    @SerializedName("calorie_goal") val calorieGoal: Int? = null,
    @SerializedName("budget_preference") val budgetPreference: Map<String, Any>? = null,
    @SerializedName("skill_level") val skillLevel: String? = null
)

// Cooking Mode Models
data class CookingStep(
    val step: Int,
    val instruction: String,
    @SerializedName("timer_seconds") val timerSeconds: Int = 0,
    @SerializedName("timer_display") val timerDisplay: String? = null,
    val completed: Boolean = false
)

// Daily Nutrition Models
data class DailyNutritionSummary(
    val calories: Int = 0,
    @SerializedName("protein_g") val proteinG: Float = 0f,
    @SerializedName("carbs_g") val carbsG: Float = 0f,
    @SerializedName("fat_g") val fatG: Float = 0f
)

// Feedback Models
data class FeedbackStats(
    @SerializedName("total_rated") val totalRated: Int = 0,
    @SerializedName("avg_rating") val avgRating: Float = 0f,
    @SerializedName("top_cuisines") val topCuisines: List<TopCuisine> = emptyList(),
    @SerializedName("liked_ingredients") val likedIngredients: List<String> = emptyList()
)

data class TopCuisine(
    val cuisine: String,
    @SerializedName("avg_rating") val avgRating: Float = 0f,
    val count: Int = 0
)

// Health Advice Models
data class HealthAdviceRequest(
    val query: String,
    @SerializedName("user_id") val userId: String? = null
)

// Add to Models.kt - Additional Models

// Shopping List Models
data class ShoppingListRequest(
    val query: String,
    @SerializedName("user_id") val userId: String? = null
)

data class ShoppingListResponse(
    @SerializedName("shopping_list") val shoppingList: String
)

// Cooking Step Models
data class ParseStepsRequest(
    @SerializedName("recipe_text") val recipeText: String
)

data class ParseStepsResponse(
    val steps: List<CookingStep>,
    @SerializedName("total_steps") val totalSteps: Int
)

// Health Advice Models
data class HealthAdviceResponse(
    val advice: String,
    val recommendations: String? = null
)

// Eco Score Models
data class EcoScoreRequest(
    val ingredients: List<Map<String, Any>>
)

data class EcoScoreResponse(
    val score: Float,
    val grade: String,
    @SerializedName("co2_kg") val co2Kg: Float,
    @SerializedName("co2_saved_kg") val co2SavedKg: Float,
    val tip: String
)

// Weekly Plan Models
data class WeeklyPlanRequest(
    val query: String,
    @SerializedName("user_id") val userId: String? = null
)

data class WeeklyPlanResponse(
    val plan: String
)

// ── Fridge Scan Models ────────────────────────────────────────────────────────

data class FridgeScanResult(
    @SerializedName("total_detected") val totalDetected: Int = 0,
    @SerializedName("allowed_items") val allowedItems: List<FridgeDetectedItem> = emptyList(),
    @SerializedName("blocked_items") val blockedItems: List<FridgeBlockedItem> = emptyList(),
    @SerializedName("scene_description") val sceneDescription: String = "",
    @SerializedName("suggested_recipes") val suggestedRecipes: List<String> = emptyList(),
    val confidence: Float = 0f,
    val summary: String = ""
)

data class FridgeDetectedItem(
    val name: String,
    val quantity: Float = 1f,
    val unit: String = "pieces",
    val freshness: String = "good",            // fresh / good / use-soon / expiring
    @SerializedName("expiry_risk") val expiryRisk: Float = 0f,  // 0.0 – 1.0
    val category: String = "other"
)

data class FridgeBlockedItem(
    val name: String,
    val quantity: Float = 1f,
    val unit: String = "pieces",
    val category: String = "other",
    @SerializedName("blocked_reason") val blockedReason: String = "",
    @SerializedName("restriction_type") val restrictionType: String = "" // diet | health | allergy
)

// Wrapper for the full API response
data class FridgeScanResponse(
    val success: Boolean,
    val data: FridgeScanResult? = null,
    val error: String? = null
)
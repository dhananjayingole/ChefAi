package eu.tutorials.chefproj.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.tutorials.chefproj.Data.api.DailyNutritionSummary
import eu.tutorials.chefproj.Data.api.MealPlan
import eu.tutorials.chefproj.ui.components.LoadingIndicator
import eu.tutorials.chefproj.ui.theme.*
import eu.tutorials.chefproj.ui.viewmodels.NutritionViewModel
import eu.tutorials.chefproj.ui.viewmodels.NutritionViewModelFactory
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = viewModel(factory = NutritionViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Tab state: 0 = Today, 1 = Meal Calendar
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it, "Dismiss", duration = SnackbarDuration.Short)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Page header ───────────────────────────────────────────
            NutritionPageHeader(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onRefresh = {
                    viewModel.loadTodayNutrition()
                    viewModel.loadMealPlans()
                }
            )

            if (uiState.isLoading) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    message = "Loading your nutrition data..."
                )
            } else {
                when (selectedTab) {
                    0 -> TodayTab(uiState.todaySummary, viewModel.getProgressPercentage(), uiState.mealPlans)
                    1 -> MealCalendarTab(uiState.mealPlans)
                }
            }
        }
    }
}

// ── Page header with tabs ─────────────────────────────────────────────────

@Composable
private fun NutritionPageHeader(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CalorieColor.copy(alpha = 0.15f), MaterialTheme.colorScheme.background)
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Nutrition", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Track your daily intake", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("📊 Today", "📅 Meal Calendar").forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) ChefOrange else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(index) }
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Today tab ─────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TodayTab(
    summary: DailyNutritionSummary?,
    progress: Float,
    todayMeals: List<MealPlan>
) {
    val today = java.time.LocalDate.now().toString()
    val meals = todayMeals.filter { it.planDate == today }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            NutritionHeroSection(summary = summary, progress = progress)
        }

        if (summary != null) {
            item {
                MacroBreakdownSection(
                    calories = summary.calories,
                    protein  = summary.proteinG.toInt(),
                    carbs    = summary.carbsG.toInt(),
                    fat      = summary.fatG.toInt()
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's Meals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(20.dp), color = ChefOrange.copy(alpha = 0.1f)) {
                    Text(
                        text = "${meals.size} meals",
                        fontSize = 12.sp,
                        color = ChefOrange,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (meals.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍽️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No meals logged today", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ask Chef AI for a recipe, then tap a save button\nto log it here!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(meals) { meal ->
                MealPlanCard(meal = meal, modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp))
            }
        }
    }
}

// ── Meal Calendar tab ─────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MealCalendarTab(allMeals: List<MealPlan>) {
    if (allMeals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📅", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No meals logged yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generate recipes in Chef AI and tap the\n\"Save\" buttons to build your calendar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Group meals by date descending
    val grouped = allMeals
        .groupBy { it.planDate }
        .entries
        .sortedByDescending { it.key }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp, ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grouped.forEach { (date, meals) ->
            item(key = "header_$date") {
                DayHeader(date = date, meals = meals)
            }
            items(meals, key = { "meal_${it.id}" }) { meal ->
                MealPlanCard(meal = meal, modifier = Modifier.padding(vertical = 4.dp))
            }
            item(key = "spacer_$date") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayHeader(date: String, meals: List<MealPlan>) {
    val totalCal = meals.sumOf { it.calories }

    val displayDate = try {
        val ld = java.time.LocalDate.parse(date)
        val today = java.time.LocalDate.now()
        when {
            ld == today               -> "Today  •  $date"
            ld == today.minusDays(1)  -> "Yesterday  •  $date"
            else                      -> ld.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() } + "  •  $date"
        }
    } catch (e: Exception) { date }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayDate,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(shape = RoundedCornerShape(20.dp), color = CalorieColor.copy(alpha = 0.12f)) {
            Text(
                text = "$totalCal kcal total",
                fontSize = 11.sp,
                color = CalorieColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ── Nutrition hero ────────────────────────────────────────────────────────

@Composable
private fun NutritionHeroSection(summary: DailyNutritionSummary?, progress: Float) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(colors = listOf(CalorieColor.copy(alpha = 0.08f), MaterialTheme.colorScheme.background)))
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DailyStatRow(label = "Calories", value = "${summary?.calories ?: 0} kcal", color = CalorieColor)
                    DailyStatRow(label = "Protein",  value = "${summary?.proteinG?.toInt() ?: 0}g",  color = ProteinColor)
                    DailyStatRow(label = "Carbs",    value = "${summary?.carbsG?.toInt() ?: 0}g",    color = CarbColor)
                    DailyStatRow(label = "Fat",      value = "${summary?.fatG?.toInt() ?: 0}g",      color = FatColor)
                }

                val animProg by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(1200, easing = EaseOutCubic),
                    label = "calorieProgress"
                )
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
                        val sw = 10.dp.toPx()
                        drawArc(
                            color = CalorieColor.copy(alpha = 0.12f), startAngle = -90f,
                            sweepAngle = 360f, useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(CalorieColor, Color(0xFFFF8C42), CalorieColor)),
                            startAngle = -90f, sweepAngle = 360f * animProg.coerceIn(0f, 1f),
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(progress * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CalorieColor)
                        Text("of goal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStatRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun MacroBreakdownSection(calories: Int, protein: Int, carbs: Int, fat: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MacroMiniCard(value = "${protein}g", label = "Protein", color = ProteinColor, modifier = Modifier.weight(1f))
        MacroMiniCard(value = "${carbs}g",   label = "Carbs",   color = CarbColor,    modifier = Modifier.weight(1f))
        MacroMiniCard(value = "${fat}g",     label = "Fat",     color = FatColor,     modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MacroMiniCard(value: String, label: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

// MealPlanCard stays the same — copied here for completeness
@Composable
fun MealPlanCard(meal: MealPlan, modifier: Modifier = Modifier) {
    val mealEmoji = when (meal.mealType) {
        "breakfast" -> "🌅"; "lunch" -> "☀️"; "dinner" -> "🌙"; "snack" -> "🍎"; else -> "🍴"
    }
    val mealColor = when (meal.mealType) {
        "breakfast" -> Color(0xFFF59E0B); "lunch" -> Color(0xFF10B981); "dinner" -> Color(0xFF6366F1); else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(mealColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Text(mealEmoji, fontSize = 24.sp) }

            Column(modifier = Modifier.weight(1f)) {
                Text(meal.recipeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientPill("${meal.calories} kcal", CalorieColor)
                    NutrientPill("${meal.proteinG.toInt()}g protein", ProteinColor)
                    NutrientPill("${meal.carbsG.toInt()}g carbs", CarbColor)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = RoundedCornerShape(8.dp), color = mealColor.copy(alpha = 0.1f)) {
                    Text(
                        text = meal.mealType.replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = mealColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = meal.planDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun NutrientPill(text: String, color: Color) {
    Text(text = text, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
}

@Composable
fun NutritionMetricCompact(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = MaterialTheme.typography.headlineSmall.fontSize, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
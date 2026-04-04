package eu.tutorials.chefproj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.tutorials.chefproj.ui.components.*
import eu.tutorials.chefproj.ui.theme.*
import eu.tutorials.chefproj.ui.viewmodels.RecipeViewModel
import eu.tutorials.chefproj.ui.viewmodels.CookingStepUi
import eu.tutorials.chefproj.ui.viewmodels.RecipeViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(
    userId: String,
    viewModel: RecipeViewModel = viewModel(factory = RecipeViewModelFactory(userId))
) {
    val uiState by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showSaveMealDialog by remember { mutableStateOf(false) }
    var ratingValue by remember { mutableStateOf(0) }
    var ratingFeedback by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it, "Dismiss") }
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearSuccess()
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ChefOrange.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
            ) {
                Column {
                    Text(
                        text = "Recipes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI-generated, tailored to you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    RecipeSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onGenerate = {
                            if (query.isNotBlank()) viewModel.generateRecipe(query)
                        },
                        isLoading = uiState.isLoading
                    )
                }
            }

            if (uiState.isLoading) {
                LoadingIndicator(
                    modifier = Modifier.fillMaxSize(),
                    message = "Creating your perfect recipe..."
                )
            } else if (uiState.recipe == null) {
                RecipeEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        RecipeContentCard(recipeText = uiState.recipe ?: "")
                    }

                    if (uiState.nutrition != null) {
                        item { NutritionCard(nutrition = uiState.nutrition) }
                    }
                    if (uiState.budget != null) {
                        item { BudgetCard(budget = uiState.budget) }
                    }
                    if (uiState.ecoScore != null) {
                        item { EcoCard(eco = uiState.ecoScore) }
                    }

                    item {
                        RecipeActionButtons(
                            onStartCooking = {
                                uiState.recipe?.let { viewModel.parseCookingSteps(it) }
                            },
                            onRate = { showRatingDialog = true },
                            onSaveMeal = { showSaveMealDialog = true }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }

        if (showRatingDialog && uiState.recipe != null) {
            RatingDialog(
                ratingValue = ratingValue,
                ratingFeedback = ratingFeedback,
                onRatingChange = { ratingValue = it },
                onFeedbackChange = { ratingFeedback = it },
                onSubmit = {
                    val name = extractRecipeName(uiState.recipe ?: "")
                    viewModel.rateRecipe(name, ratingValue, ratingFeedback.takeIf { it.isNotBlank() })
                    showRatingDialog = false
                    ratingValue = 0
                    ratingFeedback = ""
                },
                onDismiss = { showRatingDialog = false }
            )
        }

        if (showSaveMealDialog && uiState.recipe != null) {
            SaveMealDialog(
                recipeName = extractRecipeName(uiState.recipe ?: ""),
                nutrition = uiState.nutrition,
                onSave = { mealType, planDate ->
                    viewModel.saveToMealPlan(
                        mealType = mealType,
                        planDate = planDate,
                        recipeName = extractRecipeName(uiState.recipe ?: ""),
                        nutrition = uiState.nutrition
                    )
                    showSaveMealDialog = false
                },
                onDismiss = { showSaveMealDialog = false }
            )
        }

        if (uiState.isCookingMode && uiState.cookingSteps.isNotEmpty()) {
            CookingModeScreen(
                steps = uiState.cookingSteps,
                currentStep = uiState.currentStep,
                onNext = { viewModel.nextStep() },
                onPrevious = { viewModel.previousStep() },
                onExit = { viewModel.exitCookingMode() }
            )
        }
    }
}

// Keep all the existing composable functions (SaveMealDialog, RecipeSearchBar, etc.)
// They remain unchanged except for fixing imports

// ── Save Meal Dialog ─────────────────────────────────────────────────────────

@Composable
private fun SaveMealDialog(
    recipeName: String,
    nutrition: eu.tutorials.chefproj.Data.api.NutritionData?,
    onSave: (mealType: String, planDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMealType by remember { mutableStateOf("lunch") }
    var selectedDate by remember {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        mutableStateOf(today)
    }

    val mealTypes = listOf(
        Triple("breakfast", "🍳", "Breakfast"),
        Triple("lunch",     "🍱", "Lunch"),
        Triple("dinner",    "🍽️", "Dinner"),
        Triple("snack",     "🥪", "Snack")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📅", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save to Meal Plan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = recipeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Meal type",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealTypes.forEach { (key, emoji, label) ->
                        val selected = selectedMealType == key
                        Surface(
                            onClick = { selectedMealType = key },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) ChefOrange.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selected)
                                androidx.compose.foundation.BorderStroke(1.5.dp, ChefOrange)
                            else null
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(emoji, fontSize = 20.sp)
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) ChefOrange
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick date shortcuts
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val tomorrow = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }.time)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Today" to today, "Tomorrow" to tomorrow).forEach { (label, date) ->
                        val sel = selectedDate == date
                        FilterChip(
                            selected = sel,
                            onClick = { selectedDate = date },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChefOrange.copy(alpha = 0.15f),
                                selectedLabelColor = ChefOrange
                            )
                        )
                    }
                }

                // Nutrition preview if available
                nutrition?.perServing?.let { ns ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            NutrientPreviewPill("${ns.calories.toInt()} kcal", CalorieColor)
                            NutrientPreviewPill("${ns.proteinG.toInt()}g protein", ProteinColor)
                            NutrientPreviewPill("${ns.carbsG.toInt()}g carbs", CarbColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedMealType, selectedDate) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChefOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save to Meal Plan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun NutrientPreviewPill(text: String, color: Color) {
    Text(text = text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
}

// ── Search bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onGenerate: () -> Unit,
    isLoading: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("What would you like to cook?", fontSize = 14.sp) },
            enabled = !isLoading,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                Text("🔍", fontSize = 18.sp, modifier = Modifier.padding(start = 4.dp))
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ChefOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Button(
            onClick = onGenerate,
            enabled = !isLoading && query.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChefOrange),
            modifier = Modifier.height(56.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Text("Generate", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun RecipeEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🍳", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ready to cook?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Type what you'd like to make and our AI chef\nwill create a personalised recipe for you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        listOf("Pasta Carbonara", "Chicken Curry", "Avocado Toast").forEach { suggestion ->
            AssistChip(
                onClick = {},
                label = { Text(suggestion, fontSize = 13.sp) },
                modifier = Modifier.padding(vertical = 3.dp),
                leadingIcon = { Text("✨", fontSize = 14.sp) },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ── Recipe card ───────────────────────────────────────────────────────────────

@Composable
private fun RecipeContentCard(recipeText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChefOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("📖", fontSize = 18.sp) }
                Text(
                    text = "Your Recipe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = recipeText,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Action buttons (now 3) ────────────────────────────────────────────────────

@Composable
private fun RecipeActionButtons(
    onStartCooking: () -> Unit,
    onRate: () -> Unit,
    onSaveMeal: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Top row: Cook + Rate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartCooking,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChefOrange)
            ) {
                Text("🍳  Start Cooking", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onRate,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(Gold, ChefOrange))
                )
            ) {
                Text("⭐  Rate Recipe", fontWeight = FontWeight.Bold)
            }
        }
        // Full-width save button
        Button(
            onClick = onSaveMeal,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HerbGreen)
        ) {
            Text("📅  Save to Meal Plan", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Rating dialog ─────────────────────────────────────────────────────────────

@Composable
private fun RatingDialog(
    ratingValue: Int,
    ratingFeedback: String,
    onRatingChange: (Int) -> Unit,
    onFeedbackChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌟", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rate this Recipe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "How was your cooking experience?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        IconButton(
                            onClick = { onRatingChange(index + 1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (index < ratingValue) "⭐" else "☆", fontSize = 28.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ratingFeedback,
                    onValueChange = onFeedbackChange,
                    label = { Text("Share your thoughts (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = ratingValue > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChefOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Rating", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    )
}

// ── Cooking mode ─────────────────────────────────────────────────────────────

@Composable
fun CookingModeScreen(
    steps: List<CookingStepUi>,
    currentStep: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExit: () -> Unit
) {
    val current = steps.getOrNull(currentStep) ?: return
    val progress = (currentStep + 1).toFloat() / steps.size

    AlertDialog(
        onDismissRequest = onExit,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ChefOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "${currentStep + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Step ${currentStep + 1} of ${steps.size}", style = MaterialTheme.typography.titleMedium)
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ChefOrange.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            color = ChefOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ChefOrange,
                    trackColor = ChefOrange.copy(alpha = 0.15f)
                )
            }
        },
        text = {
            Column {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = current.instruction,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                if (current.timerSeconds > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TimerButton(seconds = current.timerSeconds)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("← Back") }
                }
                Button(
                    onClick = if (currentStep < steps.size - 1) onNext else onExit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChefOrange)
                ) {
                    Text(
                        text = if (currentStep < steps.size - 1) "Next →" else "Complete 🎉",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("Exit Cooking Mode", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun TimerButton(seconds: Int) {
    var timeLeft by remember { mutableIntStateOf(seconds) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
        if (timeLeft == 0) isRunning = false
    }

    val timerBg = if (isRunning) ChefOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val timeColor = if (timeLeft == 0) HerbGreen else if (isRunning) ChefOrange else MaterialTheme.colorScheme.onSurface

    Surface(shape = RoundedCornerShape(16.dp), color = timerBg) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⏱️  Timer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (timeLeft > 0) "%02d:%02d".format(timeLeft / 60, timeLeft % 60) else "Done! 🎉",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = timeColor
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isRunning && timeLeft > 0) {
                    Button(
                        onClick = { isRunning = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChefOrange)
                    ) { Text("Start Timer") }
                } else if (isRunning) {
                    Button(
                        onClick = { isRunning = false },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Pause") }
                    OutlinedButton(
                        onClick = { isRunning = false; timeLeft = seconds },
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Reset") }
                }
            }
        }
    }
}

private fun extractRecipeName(recipe: String): String {
    val pattern = Regex("##\\s*[🍽️]*\\s*(.+?)(?=\\n|\$)")
    return pattern.find(recipe)?.groupValues?.get(1)?.trim() ?: "Recipe"
}


package eu.tutorials.chefproj.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.tutorials.chefproj.ui.components.LoadingIndicator
import eu.tutorials.chefproj.ui.theme.*
import eu.tutorials.chefproj.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String = "default_user",
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(userId))
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var dietType by remember { mutableStateOf("") }
    var fitnessGoal by remember { mutableStateOf("") }
    var calorieGoal by remember { mutableStateOf("500") }
    var skillLevel by remember { mutableStateOf("") }

    LaunchedEffect(uiState.profile) {
        uiState.profile?.let {
            dietType = it.dietType ?: ""
            fitnessGoal = it.fitnessGoal ?: ""
            calorieGoal = it.calorieGoal.toString()
            skillLevel = it.skillLevel ?: ""
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it, "Dismiss", duration = SnackbarDuration.Short) }
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.fillMaxSize().padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Hero / avatar section
                item {
                    ProfileHeroSection(
                        profile = uiState.profile,
                        isEditing = isEditing,
                        onEdit = { isEditing = true },
                        onCancelEdit = { isEditing = false },
                        onReset = { viewModel.resetProfile() }
                    )
                }

                // Preferences card
                item {
                    PreferencesCard(
                        profile = uiState.profile,
                        isEditing = isEditing,
                        dietType = dietType,
                        fitnessGoal = fitnessGoal,
                        calorieGoal = calorieGoal,
                        skillLevel = skillLevel,
                        onDietTypeChange = { dietType = it },
                        onFitnessGoalChange = { fitnessGoal = it },
                        onCalorieGoalChange = { calorieGoal = it },
                        onSkillLevelChange = { skillLevel = it },
                        onSave = {
                            val updated = uiState.profile?.copy(
                                dietType = dietType.takeIf { it.isNotBlank() },
                                fitnessGoal = fitnessGoal.takeIf { it.isNotBlank() },
                                calorieGoal = calorieGoal.toIntOrNull() ?: 500,
                                skillLevel = skillLevel.takeIf { it.isNotBlank() }
                            )
                            updated?.let { viewModel.updateProfile(it) }
                            isEditing = false
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Stats card
                if (uiState.feedbackStats != null) {
                    item {
                        FeedbackStatsCard(
                            stats = uiState.feedbackStats!!,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Best value protein card
                if (uiState.cheapestProtein != null) {
                    item {
                        BestValueProteinCard(
                            protein = uiState.cheapestProtein!!,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroSection(
    profile: eu.tutorials.chefproj.Data.api.UserProfile?,
    isEditing: Boolean,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ChefOrange.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ChefOrange, ChefOrangeLight)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("👤", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "My Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            profile?.dietType?.let {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HerbGreen.copy(alpha = 0.12f),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = it.replaceFirstChar { c -> c.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HerbGreen,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isEditing) {
                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChefOrange)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Profile", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }
                } else {
                    OutlinedButton(
                        onClick = onCancelEdit,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferencesCard(
    profile: eu.tutorials.chefproj.Data.api.UserProfile?,
    isEditing: Boolean,
    dietType: String,
    fitnessGoal: String,
    calorieGoal: String,
    skillLevel: String,
    onDietTypeChange: (String) -> Unit,
    onFitnessGoalChange: (String) -> Unit,
    onCalorieGoalChange: (String) -> Unit,
    onSkillLevelChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChefOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("⚙️", fontSize = 18.sp) }
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isEditing) {
                // Edit fields
                listOf(
                    Triple("Diet Type", dietType, onDietTypeChange) to "vegetarian, vegan, non-vegetarian...",
                    Triple("Fitness Goal", fitnessGoal, onFitnessGoalChange) to "weight_loss, muscle_gain...",
                    Triple("Calorie Goal / meal", calorieGoal, onCalorieGoalChange) to "e.g. 500",
                    Triple("Skill Level", skillLevel, onSkillLevelChange) to "beginner, intermediate, advanced"
                ).forEach { (fieldData, hint) ->
                    val (label, value, onChange) = fieldData
                    OutlinedTextField(
                        value = value,
                        onValueChange = onChange,
                        label = { Text(label) },
                        placeholder = { Text(hint, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ChefOrange)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChefOrange)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                // Display mode
                val fields = listOf(
                    "🥗  Diet" to (profile?.dietType?.replaceFirstChar { it.uppercase() } ?: "Not set"),
                    "🎯  Goal" to (profile?.fitnessGoal?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Not set"),
                    "🔥  Calories/meal" to (profile?.calorieGoal?.let { "$it kcal" } ?: "Not set"),
                    "👨‍🍳  Skill Level" to (profile?.skillLevel?.replaceFirstChar { it.uppercase() } ?: "Not set")
                )

                fields.forEach { (label, value) ->
                    ProfileDetailRow(label = label, value = value)
                    if (label != fields.last().first) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                profile?.allergies?.let { allergies ->
                    if (allergies.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                        ProfileDetailRow(
                            label = "⚠️  Allergies",
                            value = allergies.joinToString(", "),
                            valueColor = ErrorRed
                        )
                    }
                }
                profile?.cuisinePreferences?.let { prefs ->
                    if (prefs.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
                        ProfileDetailRow(label = "🍜  Cuisines", value = prefs.joinToString(", "))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun FeedbackStatsCard(
    stats: eu.tutorials.chefproj.Data.api.FeedbackStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text("⭐", fontSize = 18.sp) }
                Text(
                    text = "Your Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBlock(
                    value = stats.totalRated.toString(),
                    label = "Recipes Rated",
                    color = ChefOrange
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                StatBlock(
                    value = "%.1f".format(stats.avgRating),
                    label = "Avg Rating",
                    suffix = "/5",
                    color = Gold
                )
            }

            if (stats.topCuisines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Top Cuisines",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                stats.topCuisines.take(3).forEach { cuisine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cuisine.cuisine,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⭐", fontSize = 12.sp)
                            Text(
                                text = "${cuisine.avgRating}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Gold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String, color: Color, suffix: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value$suffix",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BestValueProteinCard(
    protein: Map<String, Any>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HerbGreen.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(HerbGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("💰", fontSize = 28.sp)
            }
            Column {
                Text(
                    text = "Best Value Protein",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = HerbGreen
                )
                Text(
                    text = "${protein["name"]}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${protein["price_per_kg"]}/kg • ${protein["protein_per_100g"]}g protein/100g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String, isWarning: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatMetric(value: String, label: String, suffix: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value$suffix",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

class ProfileViewModelFactory(private val userId: String) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(
            repository = eu.tutorials.chefproj.Data.repository.NutriBotRepository(),
            userId = userId
        ) as T
    }
}
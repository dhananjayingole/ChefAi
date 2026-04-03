package eu.tutorials.chefproj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
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
import eu.tutorials.chefproj.ui.components.*
import eu.tutorials.chefproj.ui.theme.*
import eu.tutorials.chefproj.ui.viewmodels.ChatMessage
import eu.tutorials.chefproj.ui.viewmodels.ChatViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val QUICK_SUGGESTIONS = listOf(
    "🍛 Quick dinner ideas",
    "🥗 High protein meals",
    "💰 Budget-friendly recipes",
    "🌱 Vegan options today",
    "🏋️ Muscle building meals"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String? = null,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(userId))
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isVoiceProcessing by remember { mutableStateOf(false) }
    var isVoiceListening by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Show save success snackbar
    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
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
            ChatHeader(
                messageCount = uiState.messages.size,
                onClear = { viewModel.clearChat() }
            )

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty() && !uiState.isLoading) {
                    ChatWelcomeState(
                        onSuggestionClick = { suggestion ->
                            viewModel.sendMessage(suggestion.drop(3))
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            Column {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
                                ) {
                                    if (message.isUser) {
                                        UserChatBubble(message = message.content)
                                    } else {
                                        AiMessageBlock(
                                            message = message,
                                            onSaveMeal = { mealType ->
                                                viewModel.saveRecipeAsMeal(
                                                    mealType    = mealType,
                                                    recipeName  = message.recipeName ?: "Recipe",
                                                    calories    = message.recipeCalories,
                                                    proteinG    = message.recipeProtein,
                                                    carbsG      = message.recipeCarbs,
                                                    fatG        = message.recipeFat
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Show voice typing indicator when listening
                        if (isVoiceListening) {
                            item {
                                VoiceTypingIndicator()
                            }
                        }

                        // Show regular typing indicator when AI is thinking
                        if (uiState.isLoading && !isVoiceListening) {
                            item { TypingIndicator() }
                        }
                    }
                }
            }

            // Error banner
            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { error ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.clearError() },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) { Text("Dismiss", fontSize = 12.sp) }
                        }
                    }
                }
            }

            // Chat input bar with voice and text input
            ChatInputBarWithVoice(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                onVoiceResult = { voiceText ->
                    scope.launch {
                        isVoiceProcessing = true
                        // Add the voice text to input field
                        inputText = voiceText
                        // Show confirmation
                        snackbarHostState.showSnackbar("🎤 Voice input: \"$voiceText\"")
                        delay(500)
                        isVoiceProcessing = false
                        // Optional: Auto-send after voice input (uncomment if desired)
                        // if (inputText.isNotBlank()) {
                        //     viewModel.sendMessage(inputText)
                        //     inputText = ""
                        // }
                    }
                },
                onVoiceListeningState = { listening ->
                    isVoiceListening = listening
                },
                enabled = !uiState.isLoading && !isVoiceProcessing
            )
        }
    }
}

// Composable with both voice and text input
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBarWithVoice(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceListeningState: (Boolean) -> Unit,
    enabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Text input field
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (enabled) "Ask your chef anything or tap the mic..." else "Processing...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                enabled = enabled,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChefOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Voice input button
            VoiceInputButton(
                onVoiceResult = onVoiceResult,
                onListeningState = onVoiceListeningState,
                enabled = enabled,
                modifier = Modifier.size(52.dp)
            )

            // Send button
            val canSend = enabled && text.isNotBlank()
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend)
                            Brush.linearGradient(listOf(ChefOrange, ChefOrangeLight))
                        else
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ── AI message block with recipe save actions ─────────────────────────────

@Composable
private fun AiMessageBlock(
    message: ChatMessage,
    onSaveMeal: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AIChatBubble(
            message = message.content,
            intent = message.intent ?: "general"
        )

        // Show nutrition / budget / eco cards
        message.nutrition?.let { NutritionCard(nutrition = it, modifier = Modifier.padding(start = 40.dp)) }
        message.budget?.let   { BudgetCard(budget = it,        modifier = Modifier.padding(start = 40.dp)) }
        message.eco?.let      { EcoCard(eco = it,               modifier = Modifier.padding(start = 40.dp)) }

        // Save-meal action row — only for recipe messages
        if (message.hasRecipe) {
            SaveMealRow(
                recipeName = message.recipeName ?: "Recipe",
                onSave     = onSaveMeal
            )
        }
    }
}

@Composable
private fun SaveMealRow(
    recipeName: String,
    onSave: (String) -> Unit
) {
    val mealTypes = listOf(
        "breakfast" to "🌅",
        "lunch"     to "☀️",
        "dinner"    to "🌙",
        "snack"     to "🍎"
    )

    Surface(
        modifier = Modifier
            .padding(start = 40.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "Save to meal plan:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                mealTypes.forEach { (type, emoji) ->
                    OutlinedButton(
                        onClick = { onSave(type) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(ChefOrange.copy(alpha = 0.5f), ChefOrange.copy(alpha = 0.5f)))
                        )
                    ) {
                        Text(
                            text = "$emoji ${type.replaceFirstChar { it.uppercase() }}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChefOrange
                        )
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(messageCount: Int, onClear: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(colors = listOf(ChefOrange, ChefOrangeLight))),
                    contentAlignment = Alignment.Center
                ) { Text("👨‍🍳", fontSize = 22.sp) }
                Column {
                    Text(
                        text = "Chef AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(HerbGreen))
                        Text(
                            text = "Online • Your personal chef",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (messageCount > 0) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Welcome state ─────────────────────────────────────────────────────────

@Composable
private fun ChatWelcomeState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(100.dp).clip(CircleShape)
                .background(Brush.radialGradient(colors = listOf(ChefOrange.copy(alpha = 0.2f), ChefOrange.copy(alpha = 0.05f)))),
            contentAlignment = Alignment.Center
        ) { Text("👨‍🍳", fontSize = 52.sp) }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Hello, Chef!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ask me anything about recipes, nutrition,\nmeal plans, or grocery budgets.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Tip card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ChefOrange.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💡", fontSize = 16.sp)
                Text(
                    text = "After I generate a recipe, tap the save buttons below it to add it to your meal plan and Nutrition tracker!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "QUICK SUGGESTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))

        QUICK_SUGGESTIONS.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(text = suggestion, fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = MaterialTheme.colorScheme.outline)
            )
        }
    }
}

class ChatViewModelFactory(private val userId: String?) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(
            repository = eu.tutorials.chefproj.Data.repository.NutriBotRepository(),
            userId = userId
        ) as T
    }
}
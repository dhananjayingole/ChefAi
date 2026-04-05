package eu.tutorials.chefproj.ui.screens


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import eu.tutorials.chefproj.Data.api.FridgeBlockedItem
import eu.tutorials.chefproj.Data.api.FridgeDetectedItem
import eu.tutorials.chefproj.Data.api.FridgeScanResult
import eu.tutorials.chefproj.ui.theme.*
import eu.tutorials.chefproj.ui.viewmodels.FridgeScanState
import eu.tutorials.chefproj.ui.viewmodels.FridgeScanViewModel
import eu.tutorials.chefproj.ui.viewmodels.FridgeScanViewModelFactory

// ── Colors (add to Color.kt if not already there) ────────────────────────────
private val FridgeBlue = Color(0xFF0EA5E9)
private val SuccessGreen = Color(0xFF10B981)
private val BlockedRed = Color(0xFFEF4444)
private val WarningOrange = Color(0xFFF97316)
private val FreshGreen = Color(0xFF22C55E)
private val UseSoonYellow = Color(0xFFF59E0B)

// ── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeScanScreen(
    userId: String,
    viewModel: FridgeScanViewModel = viewModel(factory = FridgeScanViewModelFactory(userId))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // Camera launcher
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri.value?.let { viewModel.onImageSelected(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {

            // ── Header ────────────────────────────────────────────────────
            item {
                FridgeScanHeader()
            }

            // ── Image area ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                if (uiState.selectedImageUri == null) {
                    ImagePickerCard(
                        onGallery = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    )
                } else {
                    SelectedImageCard(
                        uri = uiState.selectedImageUri!!,
                        onClear = { viewModel.clearImage() },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                    )
                }
            }

            // ── Scan button ───────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = uiState.selectedImageUri != null
                            && uiState.scanState !is FridgeScanState.Success,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(16.dp))
                        ScanButton(
                            isScanning = uiState.scanState is FridgeScanState.Scanning,
                            onClick = { viewModel.scanFridge(context) }
                        )
                    }
                }
            }

            // ── Scanning progress ─────────────────────────────────────────
            item {
                AnimatedVisibility(visible = uiState.scanState is FridgeScanState.Scanning) {
                    ScanningProgressCard(modifier = Modifier.padding(16.dp))
                }
            }

            // ── Error state ───────────────────────────────────────────────
            item {
                if (uiState.scanState is FridgeScanState.Error) {
                    ErrorCard(
                        message = (uiState.scanState as FridgeScanState.Error).message,
                        onRetry = { viewModel.scanFridge(context) },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // ── Results ───────────────────────────────────────────────────
            uiState.scanResult?.let { result ->

                // Metrics row
                item {
                    Spacer(Modifier.height(16.dp))
                    ScanMetricsRow(
                        result = result,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Scene description
                item {
                    if (result.sceneDescription.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "🔍 ${result.sceneDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }

                // Allowed items section
                if (result.allowedItems.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader(
                            icon = "✅",
                            title = "Added to Pantry",
                            count = result.allowedItems.size,
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    items(result.allowedItems) { item ->
                        AllowedItemCard(
                            item = item,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 3.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                // Blocked items section
                if (result.blockedItems.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        SectionHeader(
                            icon = "🚫",
                            title = "Not Added (Dietary Filter)",
                            count = result.blockedItems.size,
                            color = BlockedRed,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    items(result.blockedItems) { item ->
                        BlockedItemCard(
                            item = item,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 3.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                // Recipe suggestions
                if (result.suggestedRecipes.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        RecipeSuggestionsCard(
                            recipes = result.suggestedRecipes,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Reset button
                item {
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetScan() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan Another Fridge")
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun FridgeScanHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FridgeBlue.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(FridgeBlue, Color(0xFF6366F1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🧊", fontSize = 28.sp)
            }
            Column {
                Text(
                    text = "Fridge Scanner",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "AI-powered · Dietary filtered · Auto-adds to pantry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Image Picker ──────────────────────────────────────────────────────────────

@Composable
private fun ImagePickerCard(
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("📸", fontSize = 52.sp)
            Text(
                "Upload Fridge Photo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Take a clear photo of your open fridge.\nThe AI will identify every item.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onGallery,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FridgeBlue),
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose Photo", fontWeight = FontWeight.Bold)
            }

            // Tips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "💡 Ensure good lighting",
                    "📐 Shoot from front, not angle",
                    "🚪 Open door fully",
                    "🎯 Include all shelves",
                ).forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ── Selected Image Preview ────────────────────────────────────────────────────

@Composable
private fun SelectedImageCard(
    uri: Uri,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Box {
            AsyncImage(
                model = uri,
                contentDescription = "Fridge photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 340.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
            )
            // Clear button
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, "Remove", tint = Color.White,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Scan Button ───────────────────────────────────────────────────────────────

@Composable
private fun ScanButton(
    isScanning: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isScanning,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ChefOrange,
            disabledContainerColor = ChefOrange.copy(alpha = 0.5f),
        )
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
            Text("Scanning...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        } else {
            Text("🔍", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text("Scan Fridge & Add to Pantry", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── Scanning Progress ─────────────────────────────────────────────────────────

@Composable
private fun ScanningProgressCard(modifier: Modifier = Modifier) {
    val stages = listOf(
        "🤖 Initialising vision model...",
        "🔍 Scanning fridge contents...",
        "🧪 Applying dietary filters...",
        "📦 Adding items to pantry...",
    )
    var currentStage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1400)
            currentStage = (currentStage + 1) % stages.size
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FridgeBlue.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = FridgeBlue,
            )
            AnimatedContent(targetState = stages[currentStage]) { stage ->
                Text(
                    text = stage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FridgeBlue,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

// ── Error Card ────────────────────────────────────────────────────────────────

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlockedRed.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("❌", fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text("Scan Failed", fontWeight = FontWeight.Bold, color = BlockedRed)
                Text(message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRetry) {
                Text("Retry", color = ChefOrange, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Metrics Row ───────────────────────────────────────────────────────────────

@Composable
private fun ScanMetricsRow(
    result: FridgeScanResult,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricChip(
            icon = "🔍",
            value = "${result.totalDetected}",
            label = "Detected",
            color = FridgeBlue,
            modifier = Modifier.weight(1f),
        )
        MetricChip(
            icon = "✅",
            value = "${result.allowedItems.size}",
            label = "Added",
            color = SuccessGreen,
            modifier = Modifier.weight(1f),
        )
        MetricChip(
            icon = "🚫",
            value = "${result.blockedItems.size}",
            label = "Blocked",
            color = if (result.blockedItems.isNotEmpty()) BlockedRed else Color.Gray,
            modifier = Modifier.weight(1f),
        )
        MetricChip(
            icon = "🎯",
            value = "${(result.confidence * 100).toInt()}%",
            label = "Accuracy",
            color = ChefOrange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricChip(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(icon, fontSize = 18.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, color = color, fontSize = 17.sp)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    icon: String,
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Text(
                "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Allowed Item Card ─────────────────────────────────────────────────────────

@Composable
private fun AllowedItemCard(
    item: FridgeDetectedItem,
    modifier: Modifier = Modifier,
) {
    val (badge, badgeColor) = when {
        item.expiryRisk > 0.7f -> "🔴" to Color(0xFFEF4444)
        item.expiryRisk > 0.3f -> "🟡" to Color(0xFFF59E0B)
        else -> "🟢" to SuccessGreen
    }

    val categoryEmoji = when (item.category.lowercase()) {
        "vegetables" -> "🥦"
        "fruits" -> "🍎"
        "dairy" -> "🥛"
        "proteins" -> "🥩"
        "beverages" -> "🧃"
        "condiments" -> "🧂"
        "snacks" -> "🍪"
        "frozen" -> "🧊"
        else -> "📦"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(categoryEmoji, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("${item.quantity} ${item.unit}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.freshness.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.12f)) {
                            Text(item.freshness,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = badgeColor)
                        }
                    }
                }
            }
            Text(badge, fontSize = 16.sp)
        }
    }
}

// ── Blocked Item Card ─────────────────────────────────────────────────────────

@Composable
private fun BlockedItemCard(
    item: FridgeBlockedItem,
    modifier: Modifier = Modifier,
) {
    val (icon, chipColor) = when (item.restrictionType) {
        "diet" -> "🥗" to Color(0xFF059669)
        "health" -> "🏥" to Color(0xFF7C3AED)
        "allergy" -> "⚠️" to Color(0xFFD97706)
        else -> "🚫" to BlockedRed
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BlockedRed.copy(alpha = 0.04f)
        ),
        border = BorderStroke(1.dp, BlockedRed.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(icon, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.LineThrough,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    item.blockedReason,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = chipColor.copy(alpha = 0.12f)
            ) {
                Text(
                    item.restrictionType.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = chipColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ── Recipe Suggestions ────────────────────────────────────────────────────────

@Composable
private fun RecipeSuggestionsCard(
    recipes: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningOrange.copy(alpha = 0.06f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🍳", fontSize = 20.sp)
                Text(
                    "You can make with these items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WarningOrange,
                )
            }
            Spacer(Modifier.height(8.dp))
            recipes.take(3).forEach { recipe ->
                Text(
                    "• $recipe",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
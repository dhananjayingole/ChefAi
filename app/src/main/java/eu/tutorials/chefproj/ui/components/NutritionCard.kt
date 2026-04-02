package eu.tutorials.chefproj.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.tutorials.chefproj.Data.api.NutritionData
import eu.tutorials.chefproj.Data.api.PerServing
import eu.tutorials.chefproj.ui.theme.*

@Composable
fun NutritionCard(
    nutrition: NutritionData?,
    modifier: Modifier = Modifier
) {
    if (nutrition == null) return
    val perServing = nutrition.perServing ?: PerServing()

    val accuracyColor = when {
        nutrition.accuracyPct > 70 -> SuccessGreen
        nutrition.accuracyPct > 40 -> WarningAmber
        else -> ErrorRed
    }

    val total = (perServing.proteinG + perServing.carbsG + perServing.fatG).coerceAtLeast(1.0F)
    val proteinRatio = (perServing.proteinG / total).toFloat()
    val carbsRatio = (perServing.carbsG / total).toFloat()
    val fatRatio = (perServing.fatG / total).toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CalorieColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📊", fontSize = 16.sp)
                    }
                    Text(
                        text = "Nutrition Per Serving",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accuracyColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${nutrition.accuracyPct.toInt()}% USDA",
                        fontSize = 11.sp,
                        color = accuracyColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Macro ratio bar
            MacroRatioBar(
                proteinRatio = proteinRatio,
                carbsRatio = carbsRatio,
                fatRatio = fatRatio
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main macros grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroCircle(
                    value = "${perServing.calories.toInt()}",
                    label = "Calories",
                    unit = "kcal",
                    color = CalorieColor,
                    size = 72.dp
                )
                MacroCircle(
                    value = "${perServing.proteinG.toInt()}",
                    label = "Protein",
                    unit = "g",
                    color = ProteinColor,
                    size = 60.dp
                )
                MacroCircle(
                    value = "${perServing.carbsG.toInt()}",
                    label = "Carbs",
                    unit = "g",
                    color = CarbColor,
                    size = 60.dp
                )
                MacroCircle(
                    value = "${perServing.fatG.toInt()}",
                    label = "Fat",
                    unit = "g",
                    color = FatColor,
                    size = 60.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Minor nutrients row
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniNutrientBadge(
                    value = "${perServing.fiberG.toInt()}g",
                    label = "Fiber",
                    color = FiberColor
                )
                MiniNutrientBadge(
                    value = "${perServing.sodiumMg.toInt()}mg",
                    label = "Sodium",
                    color = SodiumColor
                )
                MiniNutrientBadge(
                    value = "${nutrition.perServing}",
                    label = "Servings",
                    color = WarmGray500
                )
            }
        }
    }
}

@Composable
private fun MacroRatioBar(
    proteinRatio: Float,
    carbsRatio: Float,
    fatRatio: Float,
    modifier: Modifier = Modifier
) {
    val animatedProtein by animateFloatAsState(
        targetValue = proteinRatio,
        animationSpec = tween(800, easing = EaseOutCubic), label = "proteinAnim"
    )
    val animatedCarbs by animateFloatAsState(
        targetValue = carbsRatio,
        animationSpec = tween(800, delayMillis = 100, easing = EaseOutCubic), label = "carbsAnim"
    )

    Column(modifier = modifier) {
        Text(
            text = "Macro Split",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(animatedProtein.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(ProteinColor)
            )
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.White))
            Box(
                modifier = Modifier
                    .weight(animatedCarbs.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(CarbColor)
            )
            Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.White))
            Box(
                modifier = Modifier
                    .weight((1f - proteinRatio - carbsRatio).coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(FatColor)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendDot("Protein", ProteinColor)
            LegendDot("Carbs", CarbColor)
            LegendDot("Fat", FatColor)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MacroCircle(
    value: String,
    label: String,
    unit: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    fontSize = (size.value * 0.26f).sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = 9.sp,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MiniNutrientBadge(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NutritionMetric(
    value: String,
    label: String,
    color: Color,
    small: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = if (small) 13.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
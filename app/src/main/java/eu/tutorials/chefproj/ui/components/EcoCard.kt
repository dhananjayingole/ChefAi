package eu.tutorials.chefproj.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.tutorials.chefproj.Data.api.EcoData
import eu.tutorials.chefproj.ui.theme.ErrorRed
import eu.tutorials.chefproj.ui.theme.SuccessGreen
import eu.tutorials.chefproj.ui.theme.WarmGray500
import eu.tutorials.chefproj.ui.theme.WarningAmber

@Composable
fun EcoCard(
    eco: EcoData?,
    modifier: Modifier = Modifier
) {
    if (eco == null) return

    val scoreColor = when {
        eco.score >= 75 -> SuccessGreen
        eco.score >= 50 -> WarningAmber
        else -> ErrorRed
    }

    val animatedScore by animateFloatAsState(
        targetValue = (eco.score / 100f).toFloat(),
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "ecoScoreAnim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌱", fontSize = 16.sp)
                    }
                    Text(
                        text = "Eco Impact",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = eco.tip,
                        fontSize = 12.sp,
                        color = SuccessGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    EcoMetricBadge(
                        value = "${eco.co2Kg}kg",
                        label = "CO₂ Used",
                        color = WarmGray500
                    )
                    EcoMetricBadge(
                        value = "${eco.co2SavedKg}kg",
                        label = "CO₂ Saved",
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Score circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(88.dp)
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(88.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(
                        color = scoreColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedScore,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${eco.score.toInt()}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "Grade ${eco.grade}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scoreColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EcoMetricBadge(value: String, label: String, color: Color) {
    Column {
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EcoMetric(value: String, label: String, color: Color = MaterialTheme.colorScheme.primary) {
    Column {
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

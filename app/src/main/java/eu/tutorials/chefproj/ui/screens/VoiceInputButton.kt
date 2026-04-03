package eu.tutorials.chefproj.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.speech.SpeechRecognizer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceInputButton(
    onVoiceResult: (String) -> Unit,
    onListeningState: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    // Permission state
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Speech recognizer instance
    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    // Notify parent about listening state
    LaunchedEffect(isListening) {
        onListeningState(isListening)
    }

    // Speech recognition listener
    val recognitionListener = remember {
        object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                isAnimating = true
            }

            override fun onBeginningOfSpeech() {
                isAnimating = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Optional: Use this for voice level animation
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                isAnimating = false
            }

            override fun onError(error: Int) {
                isListening = false
                isAnimating = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                // You can show error via snackbar if needed
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                isAnimating = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    onVoiceResult(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial results if needed
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    // Set up the listener
    DisposableEffect(speechRecognizer) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            speechRecognizer.destroy()
        }
    }

    // Scale animation for listening indicator
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceScale"
    )

    fun startVoiceRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            // Speech recognition not available
            return
        }

        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your recipe request...")
            putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }

    fun stopVoiceRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
            isAnimating = false
        }
    }

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(
                if (enabled && !isListening)
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    )
                else if (isListening)
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.error,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    )
                else
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        )
                    )
            )
    ) {
        // Animated ripple effect when listening
        if (isAnimating) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                Color.Transparent
                            ),
                            radius = scale
                        )
                    )
            )
        }

        IconButton(
            onClick = {
                if (isListening) {
                    stopVoiceRecognition()
                } else {
                    if (permissionState.status.isGranted) {
                        startVoiceRecognition()
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                }
            },
            enabled = enabled,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Phone else Icons.Default.Lock,
                contentDescription = if (isListening) "Stop listening" else "Voice input",
                tint = if (enabled && !isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // Permission denied dialog
    if (!permissionState.status.isGranted && permissionState.status.shouldShowRationale) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Microphone Permission Required") },
            text = { Text("Voice input requires microphone permission. Please grant access to use this feature.") },
            confirmButton = {
                TextButton(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VoiceTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        )
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Microphone icon
            Text("🎤", fontSize = 20.sp)

            // Animated dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .scale(scale1)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .scale(scale2)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .scale(scale3)
                )
            }

            // Text
            Text(
                text = "Listening...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Hint text
            Text(
                text = "Tap mic to stop",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
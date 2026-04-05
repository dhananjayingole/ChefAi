package eu.tutorials.chefproj.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.FridgeBlockedItem
import eu.tutorials.chefproj.Data.api.FridgeDetectedItem
import eu.tutorials.chefproj.Data.api.FridgeScanResult
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class FridgeScanState {
    object Idle : FridgeScanState()
    object Scanning : FridgeScanState()
    data class Success(val result: FridgeScanResult) : FridgeScanState()
    data class Error(val message: String) : FridgeScanState()
}

data class FridgeScanUiState(
    val selectedImageUri: Uri? = null,
    val scanState: FridgeScanState = FridgeScanState.Idle,
    val scanResult: FridgeScanResult? = null,
    val showBlockedDetail: Boolean = false,
    val activeFilters: List<String> = emptyList(),   // displayed to user
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class FridgeScanViewModel(
    private val repository: NutriBotRepository,
    private val userId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FridgeScanUiState())
    val uiState: StateFlow<FridgeScanUiState> = _uiState.asStateFlow()

    // ── Image selection ───────────────────────────────────────────────────────

    fun onImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                scanState = FridgeScanState.Idle,
                scanResult = null,
            )
        }
    }

    fun clearImage() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                scanState = FridgeScanState.Idle,
                scanResult = null,
            )
        }
    }

    // ── Scan ──────────────────────────────────────────────────────────────────

    fun scanFridge(context: Context) {
        val uri = _uiState.value.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(scanState = FridgeScanState.Scanning) }

            try {
                // Read image bytes from URI
                val imageBytes = context.contentResolver
                    .openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: run {
                        _uiState.update {
                            it.copy(scanState = FridgeScanState.Error("Could not read image"))
                        }
                        return@launch
                    }

                // Compress image if needed (> 2MB)
                val processedBytes = if (imageBytes.size > 2_000_000) {
                    compressImage(imageBytes)
                } else {
                    imageBytes
                }

                // Call API
                val result = repository.scanFridge(userId, processedBytes)

                result.fold(
                    onSuccess = { scanResult ->
                        _uiState.update {
                            it.copy(
                                scanState = FridgeScanState.Success(scanResult),
                                scanResult = scanResult,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                scanState = FridgeScanState.Error(
                                    error.message ?: "Scan failed. Please try again."
                                )
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(scanState = FridgeScanState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    fun toggleBlockedDetail() {
        _uiState.update { it.copy(showBlockedDetail = !it.showBlockedDetail) }
    }

    fun resetScan() {
        _uiState.update {
            it.copy(
                selectedImageUri = null,
                scanState = FridgeScanState.Idle,
                scanResult = null,
                showBlockedDetail = false,
            )
        }
    }

    // ── Image compression ─────────────────────────────────────────────────────

    private fun compressImage(bytes: ByteArray): ByteArray {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val stream = java.io.ByteArrayOutputStream()
            // Scale down if needed
            val maxDim = 1568
            val scale = if (maxOf(bitmap.width, bitmap.height) > maxDim) {
                maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            } else 1f
            val scaled = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else bitmap
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            bytes
        }
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────

class FridgeScanViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FridgeScanViewModel(NutriBotRepository(), userId) as T
    }
}
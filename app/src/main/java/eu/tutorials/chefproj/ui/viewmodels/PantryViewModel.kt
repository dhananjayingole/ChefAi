package eu.tutorials.chefproj.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.GroceryItem
import eu.tutorials.chefproj.Data.api.GroceryItemAddRequest
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PantryUiState(
    val items: List<GroceryItem> = emptyList(),
    val expiringItems: List<GroceryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class PantryViewModel(
    private val repository: NutriBotRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init {
        loadPantry()
        loadExpiringItems()
    }

    fun loadPantry() {
        _uiState.update { state -> state.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getPantry().collect { result ->
                result.fold(
                    onSuccess = { items ->
                        _uiState.update { state ->
                            state.copy(
                                items = items,
                                isLoading = false,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    }
                )
            }
        }
    }

    fun loadExpiringItems(days: Int = 3) {
        viewModelScope.launch {
            repository.getExpiringItems(days).collect { result ->
                result.fold(
                    onSuccess = { items ->
                        _uiState.update { state ->
                            state.copy(expiringItems = items)
                        }
                    },
                    onFailure = { /* Handle error silently */ }
                )
            }
        }
    }

    fun addItem(itemName: String, quantity: Float, unit: String, category: String? = null) {
        if (itemName.isBlank()) return

        _uiState.update { state -> state.copy(isAdding = true) }
        viewModelScope.launch {
            val request = GroceryItemAddRequest(
                itemName = itemName.lowercase(),
                quantity = quantity,
                unit = unit,
                category = category,
                isPerishable = category in listOf("vegetables", "fruits", "dairy", "proteins")
            )

            val result = repository.addGrocery(request)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isAdding = false,
                            successMessage = "Added $quantity $unit $itemName"
                        )
                    }
                    loadPantry()
                    loadExpiringItems()
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isAdding = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun removeItem(itemName: String) {
        viewModelScope.launch {
            val result = repository.removeGrocery(itemName)
            result.fold(
                onSuccess = {
                    loadPantry()
                    loadExpiringItems()
                },
                onFailure = { error ->
                    _uiState.update { state -> state.copy(error = error.message) }
                }
            )
        }
    }

    fun clearPantry() {
        viewModelScope.launch {
            val result = repository.clearPantry()
            result.fold(
                onSuccess = {
                    loadPantry()
                    loadExpiringItems()
                },
                onFailure = { error ->
                    _uiState.update { state -> state.copy(error = error.message) }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { state ->
            state.copy(
                error = null,
                successMessage = null
            )
        }
    }
}
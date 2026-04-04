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
    private val repository: NutriBotRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PantryUiState())
    val uiState: StateFlow<PantryUiState> = _uiState.asStateFlow()

    init {
        loadPantry()
        loadExpiringItems()
    }

    fun loadPantry() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getPantry(userId).collect { result ->
                result.fold(
                    onSuccess = { items ->
                        _uiState.update { it.copy(items = items, isLoading = false, error = null) }
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(isLoading = false, error = err.message) }
                    }
                )
            }
        }
    }

    fun loadExpiringItems(days: Int = 3) {
        viewModelScope.launch {
            repository.getExpiringItems(userId, days).collect { result ->
                result.fold(
                    onSuccess = { items -> _uiState.update { it.copy(expiringItems = items) } },
                    onFailure = { /* silent */ }
                )
            }
        }
    }

    fun addItem(itemName: String, quantity: Float, unit: String, category: String? = null) {
        if (itemName.isBlank()) return
        _uiState.update { it.copy(isAdding = true) }
        viewModelScope.launch {
            val request = GroceryItemAddRequest(
                itemName     = itemName.lowercase(),
                quantity     = quantity,
                unit         = unit,
                category     = category,
                isPerishable = category in listOf("vegetables", "fruits", "dairy", "proteins")
            )
            repository.addGrocery(userId, request).fold(
                onSuccess = {
                    _uiState.update { it.copy(isAdding = false, successMessage = "Added $quantity $unit $itemName") }
                    loadPantry(); loadExpiringItems()
                },
                onFailure = { err -> _uiState.update { it.copy(isAdding = false, error = err.message) } }
            )
        }
    }

    fun removeItem(itemName: String) {
        viewModelScope.launch {
            repository.removeGrocery(userId, itemName).fold(
                onSuccess = { loadPantry(); loadExpiringItems() },
                onFailure = { err -> _uiState.update { it.copy(error = err.message) } }
            )
        }
    }

    fun clearPantry() {
        viewModelScope.launch {
            repository.clearPantry(userId).fold(
                onSuccess = { loadPantry(); loadExpiringItems() },
                onFailure = { err -> _uiState.update { it.copy(error = err.message) } }
            )
        }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
}

class PantryViewModelFactory(private val userId: String) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PantryViewModel(NutriBotRepository(), userId) as T
    }
}
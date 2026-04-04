package eu.tutorials.chefproj.ui.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tutorials.chefproj.Data.api.FeedbackStats
import eu.tutorials.chefproj.Data.api.UserProfile
import eu.tutorials.chefproj.Data.repository.NutriBotRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val feedbackStats: FeedbackStats? = null,
    val cheapestProtein: Map<String, Any>? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProfileViewModel(
    private val repository: NutriBotRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { loadProfile(); loadFeedbackStats(); loadCheapestProtein() }

    fun loadProfile() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.getProfile(userId).fold(
                onSuccess = { p -> _uiState.update { it.copy(profile = p, isLoading = false) } },
                onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun updateProfile(profile: UserProfile) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            repository.updateProfile(userId, profile).fold(
                onSuccess = { p -> _uiState.update { it.copy(profile = p, isSaving = false, successMessage = "Profile updated!") } },
                onFailure = { err -> _uiState.update { it.copy(isSaving = false, error = err.message) } }
            )
        }
    }

    fun resetProfile() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.resetProfile(userId).fold(
                onSuccess = { _uiState.update { it.copy(profile = null, isLoading = false, successMessage = "Profile reset!") }; loadProfile() },
                onFailure = { err -> _uiState.update { it.copy(isLoading = false, error = err.message) } }
            )
        }
    }

    fun loadFeedbackStats() {
        viewModelScope.launch {
            repository.getFeedbackStats(userId).fold(
                onSuccess = { stats -> _uiState.update { it.copy(feedbackStats = stats) } },
                onFailure = { /* silent */ }
            )
        }
    }

    fun loadCheapestProtein(dietType: String = "vegetarian") {
        viewModelScope.launch {
            repository.getCheapestProtein(userId, dietType).fold(
                onSuccess = { data -> _uiState.update { it.copy(cheapestProtein = data) } },
                onFailure = { /* silent */ }
            )
        }
    }

    fun clearMessages() { _uiState.update { it.copy(error = null, successMessage = null) } }
}

class ProfileViewModelFactory(private val userId: String) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProfileViewModel(NutriBotRepository(), userId) as T
    }
}

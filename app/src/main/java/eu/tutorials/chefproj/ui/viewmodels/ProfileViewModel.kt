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

    init {
        loadProfile()
        loadFeedbackStats()
        loadCheapestProtein()
    }

    fun loadProfile() {
        _uiState.update { state -> state.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.getProfile(userId)
            result.fold(
                onSuccess = { profile ->
                    _uiState.update { state ->
                        state.copy(
                            profile = profile,
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

    fun updateProfile(profile: UserProfile) {
        _uiState.update { state -> state.copy(isSaving = true) }
        viewModelScope.launch {
            val result = repository.updateProfile(userId, profile)
            result.fold(
                onSuccess = { updatedProfile ->
                    _uiState.update { state ->
                        state.copy(
                            profile = updatedProfile,
                            isSaving = false,
                            successMessage = "Profile updated successfully"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSaving = false,
                            error = error.message
                        )
                    }
                }
            )
        }
    }

    fun resetProfile() {
        _uiState.update { state -> state.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.resetProfile(userId)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            profile = null,
                            isLoading = false,
                            successMessage = "Profile reset successfully"
                        )
                    }
                    loadProfile()
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

    fun loadFeedbackStats() {
        viewModelScope.launch {
            val result = repository.getFeedbackStats()
            result.fold(
                onSuccess = { stats ->
                    _uiState.update { state -> state.copy(feedbackStats = stats) }
                },
                onFailure = { /* Handle silently */ }
            )
        }
    }

    fun loadCheapestProtein(dietType: String = "vegetarian") {
        viewModelScope.launch {
            val result = repository.getCheapestProtein(dietType)
            result.fold(
                onSuccess = { data ->
                    _uiState.update { state -> state.copy(cheapestProtein = data) }
                },
                onFailure = { /* Handle silently */ }
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
package eu.tutorials.chefproj.ui.viewmodels

class PantryViewModelFactory : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return PantryViewModel(
            repository = eu.tutorials.chefproj.Data.repository.NutriBotRepository()
        ) as T
    }
}
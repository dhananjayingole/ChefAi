package eu.tutorials.chefproj

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import eu.tutorials.chefproj.ui.theme.ChefProjTheme
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import eu.tutorials.chefproj.ui.components.BottomNavigationBar
import eu.tutorials.chefproj.ui.screens.ChatScreen
import eu.tutorials.chefproj.ui.screens.NutritionScreen
import eu.tutorials.chefproj.ui.screens.PantryScreen
import eu.tutorials.chefproj.ui.screens.ProfileScreen
import eu.tutorials.chefproj.ui.screens.RecipesScreen

class MainActivity : ComponentActivity() {
    private val userId = "android_user_001"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChefProjTheme {
                NutriBotApp(userId = userId)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NutriBotApp(userId: String) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "chat"

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onItemSelected = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "chat",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("chat") {
                ChatScreen(userId = userId)
            }
            composable("pantry") {
                PantryScreen()
            }
            composable("recipes") {
                RecipesScreen(userId = userId)
            }
            composable("nutrition") {
                NutritionScreen()
            }
            composable("profile") {
                ProfileScreen(userId = userId)
            }
        }
    }
}

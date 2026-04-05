package eu.tutorials.chefproj

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import eu.tutorials.chefproj.Auth.AuthState
import eu.tutorials.chefproj.Auth.LoginScreen
import eu.tutorials.chefproj.Auth.UserManager
import eu.tutorials.chefproj.ui.components.BottomNavigationBar
import eu.tutorials.chefproj.ui.screens.*
import eu.tutorials.chefproj.ui.theme.ChefProjTheme

class MainActivity : ComponentActivity() {

    private lateinit var userManager: UserManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userManager = UserManager(applicationContext)

        setContent {
            ChefProjTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NutriBotApp(userManager = userManager)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NutriBotApp(userManager: UserManager) {
    val authState by userManager.authState.collectAsState()

    // Show different screens based on auth state
    when (authState) {
        is AuthState.Loading -> {
            LoadingScreen()
        }

        is AuthState.Unauthenticated -> {
            LoginScreen(
                onLoginSuccess = { userId ->
                    // User successfully logged in/signed up
                    // authState will automatically update to Authenticated
                    Unit
                },
                onAnonymousSignIn = {
                    // This will trigger signInAnonymously and update authState
                    Unit
                },
                userManager = userManager
            )
        }

        is AuthState.Authenticated -> {
            val userId = (authState as AuthState.Authenticated).userId
            MainAppScreen(userId = userId)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainAppScreen(userId: String) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "chat"

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = currentRoute,
                onItemSelected = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
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
            composable("fridge"){
                FridgeScanScreen(userId = userId)
            }
            composable("pantry") {
                PantryScreen(navController=navController,userId = userId)
            }
            composable("recipes") {
                RecipesScreen(userId = userId)
            }
            composable("nutrition") {
                NutritionScreen(userId = userId)
            }
            composable("profile") {
                ProfileScreen(userId = userId)
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading your kitchen...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👨‍🍳",
                fontSize = 32.sp
            )
        }
    }
}
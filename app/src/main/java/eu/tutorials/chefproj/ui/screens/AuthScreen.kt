package eu.tutorials.chefproj.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (FirebaseUser) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Text(
            text = "🥗 NutriBot",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your Personal AI Chef",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = if (isLogin) "Welcome Back!" else "Create Account",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )

        // Confirm Password (only for signup)
        if (!isLogin) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
        }

        // Error Message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        if (isLogin) {
                            // Login
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onAuthSuccess(auth.currentUser!!)
                                    } else {
                                        errorMessage = task.exception?.message ?: "Login failed"
                                    }
                                }
                        } else {
                            // Signup
                            if (password != confirmPassword) {
                                errorMessage = "Passwords don't match"
                                isLoading = false
                                return@launch
                            }

                            if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                isLoading = false
                                return@launch
                            }

                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        onAuthSuccess(auth.currentUser!!)
                                    } else {
                                        errorMessage = task.exception?.message ?: "Signup failed"
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = e.message
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLogin) "Login" else "Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle between Login and Signup
        TextButton(
            onClick = {
                isLogin = !isLogin
                errorMessage = null
                password = ""
                confirmPassword = ""
            }
        ) {
            Text(
                if (isLogin) "Don't have an account? Sign Up"
                else "Already have an account? Login"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guest Mode (Optional - for testing)
        OutlinedButton(
            onClick = {
                // Generate anonymous user
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onAuthSuccess(auth.currentUser!!)
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue as Guest")
        }
    }
}
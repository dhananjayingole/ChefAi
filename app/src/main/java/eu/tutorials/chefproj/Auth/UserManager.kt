package eu.tutorials.chefproj.Auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val userId: String, val isAnonymous: Boolean) : AuthState()
    object Unauthenticated : AuthState()
}

class UserManager(private val context: Context) {

    private val auth = Firebase.auth
    private val prefs: SharedPreferences = context.getSharedPreferences("nutribot_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUserId: String?
        get() = when (val state = _authState.value) {
            is AuthState.Authenticated -> state.userId
            else -> null
        }

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(
                userId = currentUser.uid,
                isAnonymous = currentUser.isAnonymous
            )
        } else {
            // Check if we have a stored anonymous ID from previous sessions
            val storedId = prefs.getString("anonymous_user_id", null)
            if (storedId != null) {
                _authState.value = AuthState.Authenticated(
                    userId = storedId,
                    isAnonymous = true
                )
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    suspend fun signInAnonymously(): Result<String> {
        return try {
            val result = auth.signInAnonymously().await()
            val userId = result.user?.uid ?: throw Exception("Sign-in failed")

            // Store in SharedPreferences as backup
            prefs.edit().putString("anonymous_user_id", userId).apply()

            _authState.value = AuthState.Authenticated(
                userId = userId,
                isAnonymous = true
            )
            Result.success(userId)
        } catch (e: Exception) {
            // Fallback to local UUID if Firebase fails
            val fallbackId = UUID.randomUUID().toString()
            prefs.edit().putString("anonymous_user_id", fallbackId).apply()
            _authState.value = AuthState.Authenticated(
                userId = fallbackId,
                isAnonymous = true
            )
            Result.success(fallbackId)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Sign-in failed")

            // Clear anonymous flag if exists
            prefs.edit().remove("anonymous_user_id").apply()

            _authState.value = AuthState.Authenticated(
                userId = userId,
                isAnonymous = false
            )
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Sign-up failed")

            // Clear anonymous flag if exists
            prefs.edit().remove("anonymous_user_id").apply()

            _authState.value = AuthState.Authenticated(
                userId = userId,
                isAnonymous = false
            )
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        prefs.edit().remove("anonymous_user_id").apply()
        _authState.value = AuthState.Unauthenticated
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isAnonymous(): Boolean = auth.currentUser?.isAnonymous ?: false
}
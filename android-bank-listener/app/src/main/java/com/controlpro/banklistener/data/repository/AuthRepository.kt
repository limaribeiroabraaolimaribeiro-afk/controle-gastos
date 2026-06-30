package com.controlpro.banklistener.data.repository

import com.controlpro.banklistener.data.local.SecurePreferences
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo

class AuthRepository(private val securePrefs: SecurePreferences) {

    private val supabase = SupabaseProvider.client

    suspend fun signIn(email: String, password: String): Result<UserInfo> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Login falhou"))

            val session = supabase.auth.currentSessionOrNull()
            session?.let {
                securePrefs.putString(SecurePreferences.KEY_ACCESS_TOKEN, it.accessToken)
                securePrefs.putString(SecurePreferences.KEY_REFRESH_TOKEN, it.refreshToken ?: "")
            }
            securePrefs.putString(SecurePreferences.KEY_USER_ID, user.id)
            securePrefs.putString(SecurePreferences.KEY_USER_EMAIL, user.email ?: "")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<UserInfo> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Cadastro falhou"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            // ignorar erro de logout
        } finally {
            securePrefs.clear()
        }
    }

    fun getCurrentUser(): UserInfo? {
        return supabase.auth.currentUserOrNull()
    }

    fun getUserId(): String? = securePrefs.getString(SecurePreferences.KEY_USER_ID)

    fun getUserEmail(): String? = securePrefs.getString(SecurePreferences.KEY_USER_EMAIL)

    fun isLoggedIn(): Boolean = getUserId() != null

    suspend fun restoreSession(): Boolean {
        return try {
            supabase.auth.awaitInitialization()
            supabase.auth.currentUserOrNull() != null
        } catch (e: Exception) {
            false
        }
    }
}

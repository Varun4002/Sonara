package com.sonara.auth

/**
 * Authentication state consumed by the UI. UI code depends only on this —
 * never on Google-specific classes.
 */
sealed interface AuthState {
    /** No session; onboarding should be shown. */
    data object SignedOut : AuthState

    /** A provider flow is running. */
    data object Authenticating : AuthState

    /** Signed in with a real provider account. */
    data class Authenticated(val account: AuthAccount) : AuthState

    /** Local-only session; no account synchronization. */
    data class Anonymous(val account: AuthAccount) : AuthState

    /** A flow failed; [message] is user-presentable. */
    data class Error(val message: String) : AuthState
}

/** Minimal account identity — only fields the app actually uses. */
data class AuthAccount(
    val id: String,
    val displayName: String,
    val isAnonymous: Boolean,
    val email: String? = null,
)

/** How the user entered the app. */
enum class AuthMethod { GOOGLE, ANONYMOUS }

/**
 * A single sign-in mechanism. Implementations own their credentials and
 * never leak tokens through this interface.
 */
interface AuthProvider {
    val method: AuthMethod

    /**
     * Runs the flow and returns the account, or null when the user
     * cancelled. Failures throw [AuthException] with a user-presentable
     * message.
     */
    suspend fun signIn(): AuthAccount?

    suspend fun signOut()
}

/** Carries a user-presentable message; technical detail stays in logs. */
class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

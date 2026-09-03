package com.sonara.auth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The only authentication surface the UI is allowed to touch. Owns the
 * provider registry and the auth state; persists outcomes through
 * [onSession] callbacks rather than exposing providers or tokens.
 */
class AuthManager(
    private val scope: CoroutineScope,
    google: AuthProvider,
    anonymous: AuthProvider,
    /** Whether the Google flow can actually run in this build. */
    val googleConfigured: Boolean,
    private val onSessionEstablished: suspend (AuthAccount, AuthMethod) -> Unit,
    private val onSignedOut: suspend () -> Unit,
) {
    private val providers = mapOf(google.method to google, anonymous.method to anonymous)

    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(method: AuthMethod) {
        if (_state.value is AuthState.Authenticating) return
        val provider = providers[method] ?: return
        _state.value = AuthState.Authenticating
        scope.launch {
            try {
                val account = provider.signIn()
                if (account == null) {
                    // Cancelled — back to the welcome choices.
                    _state.value = AuthState.SignedOut
                } else {
                    _state.value = if (account.isAnonymous) {
                        AuthState.Anonymous(account)
                    } else {
                        AuthState.Authenticated(account)
                    }
                    onSessionEstablished(account, method)
                }
            } catch (e: AuthException) {
                _state.value = AuthState.Error(e.message ?: "Couldn't connect your account.")
            } catch (e: Exception) {
                _state.value = AuthState.Error("Couldn't connect your account.")
            }
        }
    }

    /** Returns the UI to the welcome screen; providers revoke what they can. */
    fun signOut() {
        val current = _state.value
        scope.launch {
            if (current is AuthState.Authenticated || current is AuthState.Anonymous) {
                val method = if (current is AuthState.Authenticated) {
                    AuthMethod.GOOGLE
                } else {
                    AuthMethod.ANONYMOUS
                }
                providers[method]?.signOut()
                onSignedOut()
            }
            _state.value = AuthState.SignedOut
        }
    }

    /** Restores a persisted session without re-running any provider flow. */
    fun restore(account: AuthAccount, method: AuthMethod) {
        if (_state.value is AuthState.Authenticating) return
        _state.value = if (method == AuthMethod.ANONYMOUS) {
            AuthState.Anonymous(account)
        } else {
            AuthState.Authenticated(account)
        }
    }
}

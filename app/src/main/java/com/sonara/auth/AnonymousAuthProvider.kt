package com.sonara.auth

import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Local-only session. No account synchronization exists behind it and the
 * UI must not imply otherwise. The short delay keeps the flow honest about
 * being a real (if trivial) async operation and lets the loading state render.
 */
class AnonymousAuthProvider : AuthProvider {

    override val method: AuthMethod = AuthMethod.ANONYMOUS

    override suspend fun signIn(): AuthAccount? {
        delay(300)
        return AuthAccount(
            id = "local-" + UUID.randomUUID().toString().take(8),
            displayName = "Guest",
            isAnonymous = true,
        )
    }

    override suspend fun signOut() {
        // Nothing to revoke for a local identity.
    }
}

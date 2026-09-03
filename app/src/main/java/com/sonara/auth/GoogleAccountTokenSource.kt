package com.sonara.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Official Android path to a Google OAuth token for the device's own
 * account: [AccountManager.getAuthToken] via Google Play Services. The user
 * sees and consents to the grant. No cookies, no client secrets, nothing
 * read out of other apps. Tokens are requested fresh per session and never
 * logged or persisted.
 *
 * On Android 13+ `AccountManager` does not expose device Google accounts by
 * default — the app must prompt with [accountPickerIntent] and remember the
 * chosen name via [accountFromName].
 */
object GoogleAccountTokenSource {

    const val SCOPE_YOUTUBE = "oauth2:https://www.googleapis.com/auth/youtube"

    /** The first Google account the device exposes to this app, or null. */
    fun googleAccount(context: Context): Account? =
        AccountManager.get(context).getAccountsByType("com.google").firstOrNull()

    /** An intent letting the user pick one of their on-device Google accounts. */
    fun accountPickerIntent(context: Context): Intent =
        AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            false,
            "Choose a Google account for YouTube Music",
            "This lets Sonara fetch your YouTube Music playlists.",
            null,
            null,
        )

    /** Resolves a previously picked account name back to an [Account]. */
    fun accountFromName(context: Context, name: String): Account? =
        AccountManager.get(context).getAccountsByType("com.google")
            .firstOrNull { it.name == name }

    /**
     * Requests an OAuth access token for [account] with YouTube scope.
     * Throws [IllegalStateException] with a user-presentable message on failure.
     */
    suspend fun accessToken(context: Context, account: Account): String =
        withContext(Dispatchers.IO) {
            val am = AccountManager.get(context)
            suspendCancellableCoroutine { cont ->
                val future = am.getAuthToken(
                    account,
                    SCOPE_YOUTUBE,
                    Bundle.EMPTY,
                    true, // user may see a consent prompt — that is the point
                    { result ->
                        val token = result.result.getString(AccountManager.KEY_AUTHTOKEN)
                        if (token != null) {
                            cont.resume(token)
                        } else {
                            cont.resumeWithException(
                                IllegalStateException("Google authorization was not granted."),
                            )
                        }
                    },
                    null,
                )
                cont.invokeOnCancellation { future.cancel(true) }
            }
        }

    /** Invalidates a token (e.g. after a 401) so the next request re-consents. */
    fun invalidate(context: Context, token: String) {
        runCatching { AccountManager.get(context).invalidateAuthToken("com.google", token) }
    }
}

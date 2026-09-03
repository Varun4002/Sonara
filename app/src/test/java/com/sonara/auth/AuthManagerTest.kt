package com.sonara.auth

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthManagerTest {

    private lateinit var scope: CoroutineScope
    private lateinit var sessions: MutableList<Pair<AuthAccount, AuthMethod>>
    private lateinit var signOuts: MutableList<Unit>

    @Before
    fun setUp() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        sessions = mutableListOf()
        signOuts = mutableListOf()
    }

    @After
    fun tearDown() {
        scope.cancel()
        Dispatchers.resetMain()
    }

    private fun buildManager(
        google: AuthProvider = FakeProvider(AuthMethod.GOOGLE, account = null),
        anonymous: AuthProvider = FakeProvider(AuthMethod.ANONYMOUS, account = null),
        googleConfigured: Boolean = true,
    ) = AuthManager(
        scope = scope,
        google = google,
        anonymous = anonymous,
        googleConfigured = googleConfigured,
        onSessionEstablished = { account, method -> sessions.add(account to method) },
        onSignedOut = { signOuts.add(Unit) },
    )

    @Test
    fun `initial state is SignedOut`() {
        assertThat(buildManager().state.value).isInstanceOf(AuthState.SignedOut::class.java)
    }

    @Test
    fun `anonymous sign-in produces Anonymous state`() = runTest {
        val account = AuthAccount(id = "local-abc", displayName = "Guest", isAnonymous = true)
        val manager = buildManager(
            anonymous = FakeProvider(AuthMethod.ANONYMOUS, account = account),
        )
        manager.signIn(AuthMethod.ANONYMOUS)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.Anonymous::class.java)
        assertThat((manager.state.value as AuthState.Anonymous).account.id).isEqualTo("local-abc")
    }

    @Test
    fun `anonymous sign-in calls onSessionEstablished`() = runTest {
        val account = AuthAccount(id = "local-1", displayName = "Guest", isAnonymous = true)
        val manager = buildManager(
            anonymous = FakeProvider(AuthMethod.ANONYMOUS, account = account),
        )
        manager.signIn(AuthMethod.ANONYMOUS)
        advanceUntilIdle()
        assertThat(sessions).hasSize(1)
        assertThat(sessions[0].second).isEqualTo(AuthMethod.ANONYMOUS)
    }

    @Test
    fun `google sign-in produces Authenticated state`() = runTest {
        val account = AuthAccount(id = "g-123", displayName = "Varun", isAnonymous = false)
        val manager = buildManager(
            google = FakeProvider(AuthMethod.GOOGLE, account = account),
        )
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.Authenticated::class.java)
        assertThat((manager.state.value as AuthState.Authenticated).account.displayName).isEqualTo("Varun")
    }

    @Test
    fun `google sign-in calls onSessionEstablished with GOOGLE method`() = runTest {
        val account = AuthAccount(id = "g-456", displayName = "Test", isAnonymous = false)
        val manager = buildManager(
            google = FakeProvider(AuthMethod.GOOGLE, account = account),
        )
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        assertThat(sessions).hasSize(1)
        assertThat(sessions[0].second).isEqualTo(AuthMethod.GOOGLE)
    }

    @Test
    fun `sign-in cancellation returns to SignedOut`() = runTest {
        val manager = buildManager(
            google = FakeProvider(AuthMethod.GOOGLE, account = null),
        )
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.SignedOut::class.java)
    }

    @Test
    fun `sign-in failure produces Error state`() = runTest {
        val manager = buildManager(
            google = FailingProvider(AuthMethod.GOOGLE, "Network problem"),
        )
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        val state = manager.state.value
        assertThat(state).isInstanceOf(AuthState.Error::class.java)
        assertThat((state as AuthState.Error).message).isEqualTo("Network problem")
    }

    @Test
    fun `generic exception produces user-friendly error`() = runTest {
        val manager = buildManager(
            google = RawExceptionProvider(AuthMethod.GOOGLE, RuntimeException("oops")),
        )
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        val state = manager.state.value
        assertThat(state).isInstanceOf(AuthState.Error::class.java)
        assertThat((state as AuthState.Error).message).isEqualTo("Couldn't connect your account.")
    }

    @Test
    fun `duplicate sign-in while Authenticating is ignored`() = runTest {
        val slowAccount = AuthAccount(id = "g-slow", displayName = "Slow", isAnonymous = false)
        val manager = buildManager(
            google = SlowProvider(AuthMethod.GOOGLE, account = slowAccount, delayMs = 5000),
        )
        manager.signIn(AuthMethod.GOOGLE)
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.Authenticated::class.java)
        assertThat(sessions).hasSize(1)
    }

    @Test
    fun `sign-out clears authenticated session`() = runTest {
        val account = AuthAccount(id = "g-789", displayName = "User", isAnonymous = false)
        val manager = buildManager(
            google = FakeProvider(AuthMethod.GOOGLE, account = account),
        )
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.Authenticated::class.java)

        manager.signOut()
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.SignedOut::class.java)
        assertThat(signOuts).hasSize(1)
    }

    @Test
    fun `sign-out clears anonymous session`() = runTest {
        val account = AuthAccount(id = "local-xyz", displayName = "Guest", isAnonymous = true)
        val manager = buildManager(
            anonymous = FakeProvider(AuthMethod.ANONYMOUS, account = account),
        )
        manager.signIn(AuthMethod.ANONYMOUS)
        advanceUntilIdle()

        manager.signOut()
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.SignedOut::class.java)
    }

    @Test
    fun `sign-out calls provider signOut`() = runTest {
        val fake = FakeProvider(AuthMethod.GOOGLE, account = AuthAccount("g", "G", false))
        val manager = buildManager(google = fake)
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()

        manager.signOut()
        advanceUntilIdle()
        assertThat(fake.signedOut).isTrue()
    }

    @Test
    fun `sign-out from SignedOut is harmless`() = runTest {
        val manager = buildManager()
        manager.signOut()
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.SignedOut::class.java)
        assertThat(signOuts).isEmpty()
    }

    @Test
    fun `restore anonymous session`() {
        val manager = buildManager()
        val account = AuthAccount(id = "local-r", displayName = "Guest", isAnonymous = true)
        manager.restore(account, AuthMethod.ANONYMOUS)
        assertThat(manager.state.value).isInstanceOf(AuthState.Anonymous::class.java)
    }

    @Test
    fun `restore authenticated session`() {
        val manager = buildManager()
        val account = AuthAccount(id = "g-r", displayName = "Varun", isAnonymous = false)
        manager.restore(account, AuthMethod.GOOGLE)
        assertThat(manager.state.value).isInstanceOf(AuthState.Authenticated::class.java)
        assertThat((manager.state.value as AuthState.Authenticated).account.displayName).isEqualTo("Varun")
    }

    @Test
    fun `restore is ignored while Authenticating`() = runTest {
        val manager = buildManager(
            google = SlowProvider(AuthMethod.GOOGLE, account = AuthAccount("g", "G", false), delayMs = 5000),
        )
        manager.signIn(AuthMethod.GOOGLE)
        manager.restore(AuthAccount("g-restored", "Restored", false), AuthMethod.GOOGLE)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.Authenticated::class.java)
        assertThat((manager.state.value as AuthState.Authenticated).account.id).isEqualTo("g")
    }

    @Test
    fun `restart after anonymous login via restore`() {
        val manager = buildManager()
        manager.restore(AuthAccount("local-restart", "Guest", true), AuthMethod.ANONYMOUS)
        assertThat(manager.state.value).isInstanceOf(AuthState.Anonymous::class.java)
    }

    @Test
    fun `restart after google login via restore`() {
        val manager = buildManager()
        manager.restore(AuthAccount("g-restart", "Varun", false), AuthMethod.GOOGLE)
        assertThat(manager.state.value).isInstanceOf(AuthState.Authenticated::class.java)
    }

    @Test
    fun `sign out then sign in again`() = runTest {
        val account = AuthAccount("g-cycle", "User", false)
        val anonAccount = AuthAccount("local-cycle", "Guest", true)
        val manager = buildManager(
            google = FakeProvider(AuthMethod.GOOGLE, account = account),
            anonymous = FakeProvider(AuthMethod.ANONYMOUS, account = anonAccount),
        )
        manager.signIn(AuthMethod.GOOGLE)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.Authenticated::class.java)

        manager.signOut()
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.SignedOut::class.java)

        manager.signIn(AuthMethod.ANONYMOUS)
        advanceUntilIdle()
        assertThat(manager.state.value).isInstanceOf(AuthState.Anonymous::class.java)
    }
}

// --- Test doubles ---

private open class FakeProvider(
    override val method: AuthMethod,
    private val account: AuthAccount?,
) : AuthProvider {
    var signedOut = false
        private set

    override suspend fun signIn(): AuthAccount? = account

    override suspend fun signOut() {
        signedOut = true
    }
}

private class FailingProvider(
    override val method: AuthMethod,
    private val message: String = "Failed",
    private val cause: Throwable? = null,
) : AuthProvider {
    override suspend fun signIn(): AuthAccount? = throw AuthException(message, cause)
    override suspend fun signOut() {}
}

private class RawExceptionProvider(
    override val method: AuthMethod,
    private val exception: Throwable,
) : AuthProvider {
    override suspend fun signIn(): AuthAccount? = throw exception
    override suspend fun signOut() {}
}

private class SlowProvider(
    override val method: AuthMethod,
    private val account: AuthAccount,
    private val delayMs: Long,
) : AuthProvider {
    override suspend fun signIn(): AuthAccount {
        kotlinx.coroutines.delay(delayMs)
        return account
    }
    override suspend fun signOut() {}
}

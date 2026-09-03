package com.sonara.provider.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Minimal YouTube Music innertube client. Speaks the same protocol the YTM
 * web player uses — public endpoint shapes, no third-party code, no cookies,
 * no credentials logged. Anonymous requests cover catalog search and player
 * stream resolution; account-scoped features arrive with the account layer.
 *
 * @param authTokenProvider Supplies a Bearer token for the session's Google
 *   account (YouTube scope) when available. Null = anonymous requests only.
 */
class InnertubeClient(
    private val authTokenProvider: (suspend () -> String?)? = null,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** POSTs a youtubei/v1 request and returns the parsed response, or null on failure. */
    suspend fun post(endpoint: String, body: JsonObject): JsonElement? = withContext(Dispatchers.IO) {
        runCatching {
            val payload = json.encodeToString(
                JsonObject.serializer(),
                JsonObject(body + mapOf(CONTEXT_KEY to clientContext)),
            )
            val token = runCatching { authTokenProvider?.invoke() }.getOrNull()
            val request = Request.Builder()
                .url("$BASE_URL/$endpoint?key=$API_KEY&prettyPrint=false")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            val response = http.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) return@withContext null
                it.body?.string()?.let { text -> json.parseToJsonElement(text) }
            }
        }.getOrNull()
    }

    /**
     * POSTs to a browse endpoint (authenticated). Used for home feed,
     * library, playlists — features that require the user's session.
     */
    suspend fun browse(browseId: String): JsonElement? = withContext(Dispatchers.IO) {
        runCatching {
            val body = JsonObject(
                mapOf(
                    CONTEXT_KEY to clientContext,
                    "browseId" to JsonPrimitive(browseId),
                ),
            )
            val payload = json.encodeToString(JsonObject.serializer(), body)
            val token = runCatching { authTokenProvider?.invoke() }.getOrNull()
            android.util.Log.d("InnertubeClient", "browse($browseId) token=${token?.take(10)}")
            val request = Request.Builder()
                .url("$BASE_URL/browse?key=$API_KEY&prettyPrint=false")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "application/json")
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            val response = http.newCall(request).execute()
            android.util.Log.d("InnertubeClient", "browse response: ${response.code}")
            response.use {
                if (!it.isSuccessful) {
                    android.util.Log.w("InnertubeClient", "browse HTTP ${it.code}: ${it.body?.string()?.take(200)}")
                    return@withContext null
                }
                it.body?.string()?.let { text -> json.parseToJsonElement(text) }
            }
        }.getOrNull()
    }

    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"
        // Public WEB_REMIX client context, as used by the YTM web player.
        private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
        private const val CONTEXT_KEY = "context"
        private val clientContext = JsonObject(
            mapOf(
                "client" to JsonObject(
                    mapOf(
                        "clientName" to JsonPrimitive("WEB_REMIX"),
                        "clientVersion" to JsonPrimitive("1.20240403.01.00"),
                        "hl" to JsonPrimitive("en"),
                        "gl" to JsonPrimitive("US"),
                    ),
                ),
            ),
        )
    }
}

// -- JsonElement navigation helpers (responses are deep and shape-shifting) --

fun JsonObject.obj(vararg path: String): JsonObject? {
    var cur: JsonElement? = this
    for (key in path) {
        cur = (cur as? JsonObject)?.get(key)
        if (cur == null) return null
    }
    return cur as? JsonObject
}

fun JsonObject.arr(vararg path: String): List<JsonElement> {
    if (path.isEmpty()) return emptyList()
    var cur: JsonElement? = this
    for (i in 0 until path.lastIndex) {
        cur = (cur as? JsonObject)?.get(path[i])
        if (cur == null) return emptyList()
    }
    val last = (cur as? JsonObject)?.get(path.last())
        ?: return emptyList()
    return runCatching { last.jsonArray }.getOrDefault(emptyList())
}

/** Walks [path] to a leaf primitive and returns its string value, or null. */
fun JsonObject.strPath(vararg path: String): String? {
    if (path.isEmpty()) return null
    var cur: JsonElement? = this
    for (i in 0 until path.lastIndex) {
        cur = (cur as? JsonObject)?.get(path[i])
        if (cur == null) return null
    }
    return (cur as? JsonObject)?.get(path.last())?.str()
}

fun JsonElement?.str(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content

fun JsonElement?.int(): Int? =
    (this as? JsonPrimitive)?.content?.toIntOrNull()

package com.sonara.provider.youtube

import com.sonara.music.ContentSource
import com.sonara.music.Playlist
import com.sonara.music.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Minimal YouTube Data API v3 client used for the user's own library —
 * playlists and their tracks. Unlike the innertube endpoints, the **Data API
 * accepts a standard Google OAuth Bearer token**, so this is the correct path
 * for account-scoped content (verified: innertube browse returns HTTP 400 with
 * a Bearer token; the Data API serves the user's playlists with one).
 *
 * Only the public `youtube.readonly` surface is used: reading the user's
 * playlists and playlist items. Tokens come from [GoogleAccountTokenSource]
 * with the `youtube` scope and are never logged or persisted.
 */
class YouTubeDataClient(
    private val tokenProvider: suspend () -> String?,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** The user's own playlists (mine=true), oldest first, trimmed to [maxResults]. */
    suspend fun myPlaylists(maxResults: Int = 50): List<Playlist> =
        get("youtube/v3/playlists") { url ->
            url.newBuilder()
                .addQueryParameter("part", "snippet,contentDetails")
                .addQueryParameter("mine", "true")
                .addQueryParameter("maxResults", maxResults.toString())
                .build()
        }        ?.let { data -> data.arr("items")?.let(::parsePlaylists) } ?: emptyList()

    /** Tracks inside a single playlist, in playlist order. */
    suspend fun playlistItems(playlistId: String, maxResults: Int = 200): List<Track> =
        get("youtube/v3/playlistItems") { url ->
            url.newBuilder()
                .addQueryParameter("part", "snippet,contentDetails")
                .addQueryParameter("playlistId", playlistId)
                .addQueryParameter("maxResults", maxResults.toString())
                .build()
        }        ?.let { data -> data.arr("items")?.let(::parseTracks) } ?: emptyList()

    private suspend fun get(
        path: String,
        urlTransform: (okhttp3.HttpUrl) -> okhttp3.HttpUrl,
    ): JsonObject? = withContext(Dispatchers.IO) {
        runCatching {
            val token = tokenProvider() ?: return@runCatching null
            val url = urlTransform("https://www.googleapis.com/$path".toHttpUrl())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.w(
                        "YouTubeDataClient",
                        "$path HTTP ${response.code}: ${response.body?.string()?.take(200)}",
                    )
                    return@use null
                }
                response.body?.string()?.let { text -> json.parseToJsonElement(text) as? JsonObject }
            }
        }.getOrNull()
    }

    // -- Parsing -------------------------------------------------------------

    private fun parsePlaylists(items: JsonArray?): List<Playlist> =
        items.orEmpty().mapNotNull inner@{ element ->
            val obj = element as? JsonObject ?: return@inner null
            val id = obj["id"]?.jsonPrimitive?.content ?: return@inner null
            val snippet = obj["snippet"] as? JsonObject ?: return@inner null
            val title = snippet["title"]?.jsonPrimitive?.content.orEmpty()
            val art = thumbnailUrl(snippet)
            Playlist(
                id = id,
                title = title.ifEmpty { "Untitled playlist" },
                trackCount = (obj["contentDetails"] as? JsonObject)
                    ?.get("itemCount")?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                source = ContentSource.REMOTE,
                artworkUrl = art,
            )
        }

    private fun parseTracks(items: JsonArray?): List<Track> =
        items.orEmpty().mapNotNull inner@{ element ->
            val obj = element as? JsonObject ?: return@inner null
            // Some playlist items are private/unavailable videos; skip them.
            val status = obj["status"] as? JsonObject
            val privacy = status?.get("privacyStatus")?.jsonPrimitive?.content
            if (privacy == "private") return@inner null
            val snippet = obj["snippet"] as? JsonObject ?: return@inner null
            val videoId = (obj["contentDetails"] as? JsonObject)
                ?.get("videoId")?.jsonPrimitive?.content
                ?: (snippet["resourceId"] as? JsonObject)
                    ?.get("videoId")?.jsonPrimitive?.content
                ?: return@inner null
            val title = snippet["title"]?.jsonPrimitive?.content.orEmpty()
            val channel = snippet["videoOwnerChannelTitle"]?.jsonPrimitive?.content.orEmpty()
            val art = thumbnailUrl(snippet)
            // Duration is not returned by playlistItems; resolved lazily at play.
            Track(
                id = videoId,
                title = title.ifEmpty { "Untitled" },
                artist = channel,
                album = "",
                durationMs = 0,
                source = ContentSource.REMOTE,
                playbackKey = videoId,
                artworkUrl = art,
            )
        }

    private fun thumbnailUrl(snippet: JsonObject): String? {
        val thumbs = snippet["thumbnails"] as? JsonObject ?: return null
        val best = thumbs["high"] ?: thumbs["medium"] ?: thumbs["default"] ?: return null
        return (best as? JsonObject)?.get("url")?.jsonPrimitive?.content
    }

    /** Extension to safely read an array by key from a response object. */
    private fun JsonObject.arr(key: String): JsonArray? =
        runCatching { this[key] as JsonArray }.getOrNull()
}

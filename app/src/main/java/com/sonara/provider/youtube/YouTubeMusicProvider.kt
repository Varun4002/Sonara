package com.sonara.provider.youtube

import com.sonara.music.Album
import com.sonara.music.Artist
import com.sonara.music.HomeFeed
import com.sonara.music.MusicLibrary
import com.sonara.music.MusicProvider
import com.sonara.music.Playlist
import com.sonara.music.ProviderCapabilities
import com.sonara.music.SearchResults
import com.sonara.music.Track
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Real YouTube Music catalog provider.
 *
 * Two transport layers:
 *  - **innertube** (anonymous): home feed, search, playback stream resolution.
 *    These endpoints reject a Google OAuth Bearer token (HTTP 400), so they
 *    always run anonymous.
 *  - **YouTube Data API v3** ([YouTubeDataClient], authenticated with a real
 *    OAuth token): the user's own playlists and their contents. This is the
 *    only path that returns account-scoped library data.
 */
class YouTubeMusicProvider(
    private val authTokenProvider: (suspend () -> String?)? = null,
    private val dataClient: YouTubeDataClient? = null,
) : MusicProvider {

    private val client = InnertubeClient(authTokenProvider)
    private val isAuthenticated get() = authTokenProvider != null

    override val id: String = "youtube-music"

    override val capabilities: ProviderCapabilities
        get() = ProviderCapabilities(
            canSearch = true,
            canBrowseCatalog = isAuthenticated,
            canStreamPlayback = true,
            canSyncLibrary = isAuthenticated,
            canSyncLikes = isAuthenticated,
            canRecommend = isAuthenticated,
            canReadHome = isAuthenticated,
            canReadPlaylists = isAuthenticated,
            canReadHistory = false,
        )

    // -- Home feed ----------------------------------------------------------

    override suspend fun getHomeFeed(): HomeFeed {
        if (!isAuthenticated) {
            android.util.Log.d("YTMusicProvider", "Not authenticated — empty home")
            return HomeFeed(sections = emptyList())
        }
        android.util.Log.d("YTMusicProvider", "Fetching home via browse($HOME_BROWSE_ID)")
        val response = client.browse(HOME_BROWSE_ID) as? JsonObject
        if (response == null) {
            android.util.Log.w("YTMusicProvider", "browse returned null")
            return HomeFeed(emptyList())
        }
        android.util.Log.d("YTMusicProvider", "browse response keys: ${response.keys}")
        val contents = response["contents"]?.jsonObject
        android.util.Log.d("YTMusicProvider", "contents keys: ${contents?.keys}")
        val scbr = contents?.get("singleColumnBrowseResultsRenderer")?.jsonObject
        android.util.Log.d("YTMusicProvider", "singleColumnBrowseResultsRenderer: ${scbr != null}")
        val tabs = scbr?.get("tabs")?.jsonArray
        android.util.Log.d("YTMusicProvider", "tabs count: ${tabs?.size}")
        val tab0 = tabs?.firstOrNull()?.jsonObject
        val tabContent = tab0?.get("tabRenderer")?.jsonObject?.get("content")?.jsonObject
        android.util.Log.d("YTMusicProvider", "tab content keys: ${tabContent?.keys}")
        val slr = tabContent?.get("sectionListRenderer")?.jsonObject
        android.util.Log.d("YTMusicProvider", "sectionListRenderer: ${slr != null}")
        val slrContents = slr?.get("contents")?.jsonArray
        android.util.Log.d("YTMusicProvider", "sectionListRenderer contents count: ${slrContents?.size}")
        if (slrContents != null) {
            for (i in slrContents.indices) {
                val el = slrContents[i].jsonObject
                android.util.Log.d("YTMusicProvider", "  section[$i] keys: ${el.keys}")
                // Dump carousel contents
                val carousel = el.get("musicCarouselShelfRenderer")?.jsonObject
                if (carousel != null) {
                    val contents = carousel.get("contents")?.jsonArray
                    android.util.Log.d("YTMusicProvider", "  carousel[$i] contents count: ${contents?.size}")
                    for (j in (contents?.indices ?: emptyList())) {
                        val item = contents!![j].jsonObject
                        if (j == 0) {
                            val tworow = item.get("musicTwoRowItemRenderer")?.jsonObject
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow keys: ${tworow?.keys}")
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow title: ${tworow?.get("title")}")
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow subtitle: ${tworow?.get("subtitle")}")
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow thumbnailRenderer: ${tworow?.get("thumbnailRenderer") != null}")
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow navigationEndpoint: ${tworow?.get("navigationEndpoint")}")
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow flexColumns: ${tworow?.get("flexColumns") != null}")
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow thumbnail: ${tworow?.get("thumbnail") != null}")
                            android.util.Log.d("YTMusicProvider", "    firstTwoRow playlistItemData: ${tworow?.get("playlistItemData") != null}")
                        }
                    }
                }
            }
        }
        val sections = InnertubeMapper.homeFeedSections(response)
        android.util.Log.d("YTMusicProvider", "Home sections: ${sections.size}")
        sections.forEach { s ->
            android.util.Log.d("YTMusicProvider", "  Section '${s.title}': ${s.items.size} items, type=${s.type}")
        }
        android.util.Log.d("YTMusicProvider", "Home sections: ${sections.size}")
        return HomeFeed(sections = sections)
    }

    // -- Search -------------------------------------------------------------

    override suspend fun search(query: String): SearchResults {
        val response = client.post("search", InnertubeMapper.searchRequest(query)) as? JsonObject
        val tracks = response?.let { InnertubeMapper.searchResults(it) }.orEmpty()
        return SearchResults(query = query, tracks = tracks)
    }

    // -- Playback -----------------------------------------------------------

    override suspend fun resolvePlayback(track: Track): String? = resolveStream(track)

    suspend fun resolveStream(track: Track): String? {
        val response = client.post("player", InnertubeMapper.playerRequest(track.id))
            as? JsonObject ?: return null
        if (!InnertubeMapper.playabilityOk(response)) return null
        return InnertubeMapper.streamUrl(response)
    }

    // -- Library ------------------------------------------------------------

    override suspend fun getLibrary(): MusicLibrary {
        val data = dataClient
        if (data == null || !isAuthenticated) {
            return MusicLibrary(emptyList(), emptyList(), emptyList())
        }
        val playlists = data.myPlaylists()
        return MusicLibrary(playlists = playlists, albums = emptyList(), artists = emptyList())
    }

    override suspend fun getPlaylists(): List<Playlist> {
        val data = dataClient ?: return emptyList()
        if (!isAuthenticated) return emptyList()
        return data.myPlaylists()
    }

    override suspend fun getAlbums(): List<Album> = emptyList()
    override suspend fun getAlbum(id: String): Album? = null
    override suspend fun getArtists(): List<Artist> = emptyList()
    override suspend fun getArtist(id: String): Artist? = null

    override suspend fun getTrack(id: String): Track? =
        search(id).tracks.firstOrNull { it.id == id }

    companion object {
        private const val HOME_BROWSE_ID = "FEmusic_home"
        private const val LIBRARY_BROWSE_ID = "FEmusic_library"
    }
}

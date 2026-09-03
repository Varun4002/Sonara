package com.sonara.provider.youtube

import com.sonara.music.Album
import com.sonara.music.Artist
import com.sonara.music.ContentSource
import com.sonara.music.HomeFeed
import com.sonara.music.HomeItem
import com.sonara.music.HomeSection
import com.sonara.music.HomeSectionType
import com.sonara.music.Playlist
import com.sonara.music.Track
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Maps innertube responses to Sonara models. Navigation-based parsing —
 * innertube shapes shift constantly, so we walk the tree defensively and
 * return empty rather than crashing on unknown layouts.
 */
object InnertubeMapper {

    // -- Search -------------------------------------------------------------

    fun searchResults(response: JsonObject): List<Track> {
        val tracks = mutableListOf<Track>()
        val shelves = response.arr("contents", "tabbedSearchResultsRenderer", "tabs")
        for (tab in shelves) {
            val content = (tab as? JsonObject)
                ?.obj("tabRenderer", "content") ?: continue
            for (section in content.arr("sectionListRenderer", "contents")) {
                val shelf = (section as? JsonObject)?.obj("musicShelfRenderer") ?: continue
                for (item in shelf.arr("contents")) {
                    parseListItem((item as? JsonObject)?.obj("musicResponsiveListItemRenderer"))
                        ?.let(tracks::add)
                }
            }
        }
        return tracks.distinctBy { it.id }
    }

    // -- Home feed (FEmusic_home browse response) ---------------------------

    fun homeFeedSections(response: JsonObject): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()
        val tabs = response.arr("contents", "singleColumnBrowseResultsRenderer", "tabs")
        android.util.Log.d("InnertubeMapper", "tabs from arr: ${tabs.size}")
        for (tab in tabs) {
            val tabObj = tab as? JsonObject ?: continue
            android.util.Log.d("InnertubeMapper", "tab keys: ${tabObj.keys}")
            val tabRenderer = tabObj.obj("tabRenderer")
            android.util.Log.d("InnertubeMapper", "tabRenderer: ${tabRenderer != null}, keys: ${tabRenderer?.keys}")
            val content = tabRenderer?.obj("content")
            android.util.Log.d("InnertubeMapper", "content: ${content != null}, keys: ${content?.keys}")
            val slr = content?.obj("sectionListRenderer")
            android.util.Log.d("InnertubeMapper", "sectionListRenderer: ${slr != null}, keys: ${slr?.keys}")
            val slrContents = slr?.arr("contents")
            android.util.Log.d("InnertubeMapper", "slr contents via arr(): ${slrContents?.size}")
            for (shelv in (slrContents ?: emptyList())) {
                val obj = shelv as? JsonObject ?: continue
                android.util.Log.d("InnertubeMapper", "shelv keys: ${obj.keys}")
                obj.obj("musicCarouselShelfRenderer")?.let { carousel ->
                    val section = parseCarouselShelf(carousel)
                    android.util.Log.d("InnertubeMapper", "carousel result: ${section?.title} (${section?.items?.size} items)")
                    section?.let(sections::add)
                }
                obj.obj("musicShelfRenderer")?.let { shelf ->
                    parseMusicShelf(shelf)?.let(sections::add)
                }
            }
        }
        android.util.Log.d("InnertubeMapper", "homeFeedSections total: ${sections.size}")
        return sections
    }

    private fun parseCarouselShelf(carousel: JsonObject): HomeSection? {
        val title = carousel.arr("header", "musicCarouselShelfBasicHeaderRenderer", "title", "runs")
            ?.getOrNull(0)
            ?.let { (it as? JsonObject)?.get("text").str() } ?: "Section"
        val items = mutableListOf<HomeItem>()
        for (content in carousel.arr("contents")) {
            val obj = content as? JsonObject ?: continue
            obj.obj("musicTwoRowItemRenderer")?.let { renderer ->
                val item = parseTwoRowItem(renderer)
                android.util.Log.d("InnertubeMapper", "parseTwoRowItem for '$title': ${item != null}")
                item?.let(items::add)
            }
        }
        android.util.Log.d("InnertubeMapper", "parseCarouselShelf '$title': ${items.size} items")
        if (items.isEmpty()) return null
        return HomeSection(
            id = "carousel_${title.hashCode()}",
            title = title,
            type = HomeSectionType.RECOMMENDED,
            items = items,
        )
    }

    private fun parseMusicShelf(shelf: JsonObject): HomeSection? {
        val title = shelf.arr("title", "runs")?.getOrNull(0)
            ?.let { (it as? JsonObject)?.get("text").str() } ?: "Section"
        val items = mutableListOf<HomeItem>()
        for (content in shelf.arr("contents")) {
            val renderer = (content as? JsonObject)
                ?.obj("musicResponsiveListItemRenderer") ?: continue
            parseListItem(renderer)?.let { items.add(HomeItem.TrackItem(it)) }
        }
        if (items.isEmpty()) return null
        return HomeSection(
            id = "shelf_${title.hashCode()}",
            title = title,
            type = HomeSectionType.RECOMMENDED,
            items = items,
        )
    }

    // -- Library (FEmusic_library browse response) ---------------------------

    fun libraryPlaylists(response: JsonObject): List<Playlist> {
        val playlists = mutableListOf<Playlist>()
        for (tab in response.arr("contents", "singleColumnBrowseResultsRenderer", "tabs")) {
            val content = (tab as? JsonObject)
                ?.obj("tabRenderer", "content") ?: continue
            for (shelv in content.arr("sectionListRenderer", "contents")) {
                val shelf = (shelv as? JsonObject)?.obj("musicShelfRenderer") ?: continue
                for (item in shelf.arr("contents")) {
                    val renderer = (item as? JsonObject)
                        ?.obj("musicResponsiveListItemRenderer") ?: continue
                    parsePlaylistItem(renderer)?.let(playlists::add)
                }
            }
        }
        return playlists
    }

    // -- Shared item parsers ------------------------------------------------

    /**
     * Parses a musicTwoRowItemRenderer — the primary item type in home carousels.
     * These are typically playlists or albums. The item has:
     *   title.runs[0].text, subtitle.runs[0].text,
     *   thumbnailRenderer.musicTwoRowItemThumbnailRenderer.thumbnail.thumbnails[],
     *   navigationEndpoint.browseEndpoint.browseId (playlist/album) or
     *   navigationEndpoint.watchEndpoint.videoId (track).
     */
    fun parseTwoRowItem(renderer: JsonObject): HomeItem? {
        val titleText = renderer.arr("title", "runs")?.getOrNull(0)
            ?.let { (it as? JsonObject)?.get("text").str() } ?: run {
            android.util.Log.d("InnertubeMapper", "parseTwoRowItem: no title")
            return null
        }
        val subtitleText = renderer.arr("subtitle", "runs")
            ?.mapNotNull { (it as? JsonObject)?.get("text").str() }
            ?.joinToString("") { it }.orEmpty()
        val thumbnail = renderer.arr(
            "thumbnailRenderer", "musicTwoRowItemThumbnailRenderer",
            "thumbnail", "thumbnails",
        )?.lastOrNull()
            ?.let { (it as? JsonObject)?.get("url").str() }

        // Determine content type from navigationEndpoint.
        val browseId = renderer.strPath("navigationEndpoint", "browseEndpoint", "browseId")
        val watchId = renderer.strPath("navigationEndpoint", "watchEndpoint", "videoId")
        val playlistId = renderer.strPath("navigationEndpoint", "watchPlaylistEndpoint", "playlistId")
            // browseEndpoint may be a playlist — check pageType config
            ?: browseId?.takeIf {
                val pageType = renderer.strPath(
                    "navigationEndpoint", "browseEndpoint",
                    "browseEndpointContextSupportedConfigs",
                    "browseEndpointContextMusicConfig", "pageType",
                )
                pageType?.contains("PLAYLIST") == true
            }

        return when {
            watchId != null -> HomeItem.TrackItem(
                Track(
                    id = watchId, title = titleText, artist = subtitleText,
                    album = "", durationMs = 0, source = ContentSource.REMOTE,
                    playbackKey = watchId, artworkUrl = thumbnail,
                ),
            )
            playlistId != null -> HomeItem.PlaylistItem(
                Playlist(
                    id = playlistId, title = titleText,
                    trackCount = 0, source = ContentSource.REMOTE,
                    artworkUrl = thumbnail,
                ),
            )
            browseId != null -> HomeItem.AlbumItem(
                Album(
                    id = browseId, title = titleText, artist = subtitleText,
                    year = null, trackIds = emptyList(),
                    source = ContentSource.REMOTE, artworkUrl = thumbnail,
                ),
            )
            else -> null
        }
    }

    private fun parseListItem(item: JsonObject?): Track? {
        item ?: return null
        val id = item.strPath("playlistItemData", "videoId")
            ?: item.strPath("overlay", "musicItemThumbnailOverlayRenderer", "content",
                "musicPlayButtonRenderer", "playNavigationEndpoint", "watchEndpoint", "videoId")
            ?: return null
        val flex = item.arr("flexColumns")
        val title = flex.getOrNull(0)
            ?.let { (it as? JsonObject)?.obj("musicResponsiveListItemFlexColumnRenderer") }
            ?.arr("text", "runs")?.getOrNull(0)
            ?.let { (it as? JsonObject)?.get("text").str() } ?: return null
        val subtitle = flex.getOrNull(1)
            ?.let { (it as? JsonObject)?.obj("musicResponsiveListItemFlexColumnRenderer") }
            ?.arr("text", "runs")
            ?.mapNotNull { (it as? JsonObject)?.get("text").str() }
            ?.joinToString("") { it }.orEmpty()
        val thumb = item.arr("thumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails")
            ?.lastOrNull()?.let { (it as? JsonObject)?.get("url").str() }
        val parts = subtitle.split(" • ")
        val artist = parts.getOrNull(0).orEmpty()
        val album = parts.getOrNull(1).orEmpty()
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = parseDuration(parts.lastOrNull()),
            source = ContentSource.REMOTE,
            playbackKey = id,
            artworkUrl = thumb,
        )
    }

    private fun parsePlaylistItem(renderer: JsonObject): Playlist? {
        val data = renderer.obj("playlistItemData") ?: return null
        val id = data.get("playlistId").str() ?: return null
        val title = renderer.arr("flexColumns")
            ?.getOrNull(0)
            ?.let { (it as? JsonObject)?.obj("musicResponsiveListItemFlexColumnRenderer") }
            ?.arr("text", "runs")?.getOrNull(0)
            ?.let { (it as? JsonObject)?.get("text").str() } ?: "Playlist"
        val thumb = renderer.arr("thumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails")
            ?.lastOrNull()?.let { (it as? JsonObject)?.get("url").str() }
        return Playlist(
            id = id, title = title, trackCount = 0,
            source = ContentSource.REMOTE, artworkUrl = thumb,
        )
    }

    // -- Player stream extraction --------------------------------------------

    fun streamUrl(playerResponse: JsonObject): String? {
        val formats = playerResponse.arr("streamingData", "adaptiveFormats") +
            playerResponse.arr("streamingData", "formats")
        val audio = formats.mapNotNull { it as? JsonObject }
            .filter { it["mimeType"].str()?.startsWith("audio/") == true }
            .filter { it.get("url").str() != null }
            .filter { it.obj("playabilityStatus") == null }
        val chosen = audio.minByOrNull { it.get("bitrate").int() ?: 0 } ?: return null
        return chosen.get("url").str()
    }

    fun playabilityOk(playerResponse: JsonObject): Boolean =
        playerResponse.obj("playabilityStatus")?.get("status").str() == "OK"

    // -- Request builders ---------------------------------------------------

    fun playerRequest(videoId: String): JsonObject = buildJsonObject {
        putJsonObject("videoId") { put("videoId", videoId) }
        put("contentCheckOk", true)
        put("racyCheckOk", true)
    }

    fun searchRequest(query: String): JsonObject = buildJsonObject {
        put("query", query)
    }

    // -- Helpers ------------------------------------------------------------

    private fun parseDuration(text: String?): Long {
        val parts = text?.split(":")?.mapNotNull { it.trim().toLongOrNull() } ?: return 0
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            else -> 0
        }
    }
}

package com.sonara.music

/**
 * Provider-neutral catalog models. The UI and playback layers consume only
 * these — never a provider's raw response types.
 *
 * Artwork: tracks and albums carry an optional [artworkUrl] for remote art
 * and an optional [artworkMediaId] for local/res resource lookup via Coil.
 * At least one should be non-null for a visual surface to render.
 */

/** Where a piece of content came from. */
enum class ContentSource { LOCAL, REMOTE }

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val source: ContentSource,
    /** Provider-local playback key (resource name today, stream id later). */
    val playbackKey: String,
    val artworkMediaId: String? = null,
    val artworkUrl: String? = null,
)

data class Playlist(
    val id: String,
    val title: String,
    val trackCount: Int,
    val source: ContentSource,
    val artworkUrl: String? = null,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val trackIds: List<String>,
    val source: ContentSource,
    val artworkMediaId: String? = null,
    val artworkUrl: String? = null,
)

data class Artist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
    val source: ContentSource,
    val artworkUrl: String? = null,
)

data class SearchResults(
    val query: String,
    val tracks: List<Track>,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

data class MusicLibrary(
    val playlists: List<Playlist>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val likedTracks: List<Track> = emptyList(),
)

// ---------------------------------------------------------------------------
// Home feed
// ---------------------------------------------------------------------------

data class HomeFeed(
    val sections: List<HomeSection>,
)

data class HomeSection(
    val id: String,
    val title: String,
    val type: HomeSectionType,
    val items: List<HomeItem>,
)

enum class HomeSectionType {
    QUICK_PICKS,
    CONTINUE_LISTENING,
    RECENTLY_PLAYED,
    RECOMMENDED,
    PLAYLISTS,
    ALBUMS,
    ARTISTS,
    NEW_RELEASES,
}

sealed interface HomeItem {
    data class TrackItem(val track: Track) : HomeItem
    data class AlbumItem(val album: Album) : HomeItem
    data class ArtistItem(val artist: Artist) : HomeItem
    data class PlaylistItem(val playlist: Playlist) : HomeItem
}

// ---------------------------------------------------------------------------
// Capabilities
// ---------------------------------------------------------------------------

/** What the active provider can actually do for the current session. */
data class ProviderCapabilities(
    val canSearch: Boolean,
    val canBrowseCatalog: Boolean,
    val canStreamPlayback: Boolean,
    val canSyncLibrary: Boolean,
    val canSyncLikes: Boolean,
    val canRecommend: Boolean,
    val canReadHome: Boolean,
    val canReadPlaylists: Boolean,
    val canReadHistory: Boolean,
) {
    companion object {
        val LocalOnly = ProviderCapabilities(
            canSearch = true,
            canBrowseCatalog = true,
            canStreamPlayback = true,
            canReadHome = true,
            canReadPlaylists = true,
            canReadHistory = true,
            canSyncLibrary = false,
            canSyncLikes = false,
            canRecommend = false,
        )
        val Unavailable = ProviderCapabilities(
            canSearch = false,
            canBrowseCatalog = false,
            canStreamPlayback = false,
            canReadHome = false,
            canReadPlaylists = false,
            canReadHistory = false,
            canSyncLibrary = false,
            canSyncLikes = false,
            canRecommend = false,
        )
    }
}

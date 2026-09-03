package com.sonara.music

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Caching layer between the UI and [MusicProvider]. Owns the single write
 * path for home feed, search, and library state. UI consumes [StateFlow]
 * only — never calls the provider directly.
 *
 * Architecture:
 *   UI → MusicRepository → MusicProvider → network/disk
 *
 * The repository caches the home feed so that:
 *   1. screen rotation doesn't re-fetch,
 *   2. background refresh can update silently,
 *   3. error states can still show cached content.
 */
class MusicRepository(private val provider: MusicProvider) {

    // -- Home feed --------------------------------------------------------

    private val _homeFeed = MutableStateFlow<HomeFeed?>(null)
    val homeFeed: StateFlow<HomeFeed?> = _homeFeed.asStateFlow()

    private val _homeState = MutableStateFlow<HomeScreenState>(HomeScreenState.Loading)
    val homeState: StateFlow<HomeScreenState> = _homeState.asStateFlow()

    /**
     * Fetches the home feed from the provider. On success the result is
     * cached and emitted. On failure, cached data is shown when available;
     * otherwise an error state is emitted.
     */
    suspend fun refreshHome() {
        _homeState.value = HomeScreenState.Loading
        try {
            val feed = provider.getHomeFeed()
            _homeFeed.value = feed
            _homeState.value = if (feed.sections.isEmpty()) {
                HomeScreenState.Empty
            } else {
                HomeScreenState.Loaded(feed, providerId = provider.id)
            }
        } catch (e: Exception) {
            val cached = _homeFeed.value
            _homeState.value = if (cached != null && cached.sections.isNotEmpty()) {
                HomeScreenState.Loaded(cached, providerId = provider.id)
            } else {
                HomeScreenState.Error(e.message ?: "Couldn't load your music.")
            }
        }
    }

    // -- Search -----------------------------------------------------------

    suspend fun search(query: String): SearchResults = provider.search(query)

    // -- Library ----------------------------------------------------------

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _libraryLoading = MutableStateFlow(false)
    val libraryLoading: StateFlow<Boolean> = _libraryLoading.asStateFlow()

    /** Fetches the user's playlists from the provider. */
    suspend fun refreshPlaylists() {
        _libraryLoading.value = true
        _playlists.value = try {
            provider.getPlaylists()
        } catch (e: Exception) {
            emptyList()
        }
        _libraryLoading.value = false
    }

    suspend fun getLibrary(): MusicLibrary = provider.getLibrary()

    // -- Albums / Artists / Playlists -------------------------------------

    suspend fun getAlbums(): List<Album> = provider.getAlbums()

    suspend fun getAlbum(id: String): Album? = provider.getAlbum(id)

    suspend fun getArtists(): List<Artist> = provider.getArtists()

    suspend fun getPlaylists(): List<Playlist> = provider.getPlaylists()

    // -- Track resolution -------------------------------------------------

    suspend fun getTrack(id: String): Track? = provider.getTrack(id)

    /** Resolves a playable source for a track (playback layer only). */
    suspend fun resolvePlayback(track: Track): String? = provider.resolvePlayback(track)

    /** Provider capabilities, constant for the session lifetime. */
    val capabilities: ProviderCapabilities get() = provider.capabilities
    val providerId: String get() = provider.id
}

/** Screen-level state for the Home surface. */
sealed interface HomeScreenState {
    data object Loading : HomeScreenState
    data class Loaded(val feed: HomeFeed, val providerId: String) : HomeScreenState
    data class Error(val message: String) : HomeScreenState
    data object Empty : HomeScreenState
}

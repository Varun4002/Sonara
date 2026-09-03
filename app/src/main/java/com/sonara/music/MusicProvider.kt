package com.sonara.music

/**
 * The music data boundary. The UI asks this interface for catalog and
 * library content; it never knows (or cares) whether the answer comes from
 * the bundled demo catalog, YouTube Music, or anything else.
 *
 * Deliberately minimal: methods return plain models and suspend so real
 * providers can do network work. Streaming playback stays in the playback
 * layer — this interface describes "what songs exist", not "how audio plays".
 */
interface MusicProvider {

    val id: String

    /** What this provider supports for the current session. */
    val capabilities: ProviderCapabilities

    /** The home feed: greeting-driven sections of content. */
    suspend fun getHomeFeed(): HomeFeed

    suspend fun search(query: String): SearchResults

    suspend fun getAlbums(): List<Album>

    suspend fun getAlbum(id: String): Album?

    suspend fun getArtists(): List<Artist>

    suspend fun getArtist(id: String): Artist?

    suspend fun getPlaylists(): List<Playlist>

    suspend fun getLibrary(): MusicLibrary

    /** Resolve a track by its provider-local ID. */
    suspend fun getTrack(id: String): Track?

    /**
     * Resolves a playable source URI for [track], or null when the provider
     * cannot stream it. The playback layer consumes this — never the UI.
     */
    suspend fun resolvePlayback(track: Track): String?
}

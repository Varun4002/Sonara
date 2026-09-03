package com.sonara.music

import com.sonara.playback.DemoCatalog
import com.sonara.playback.DemoTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * The bundled demo catalog as a [MusicProvider]. Real, local data — clearly
 * labeled LOCAL everywhere it surfaces. It keeps the app fully usable until
 * a remote provider exists, and defines the contract that provider must meet.
 */
class DemoMusicProvider : MusicProvider {

    override val id: String = "demo"

    override val capabilities: ProviderCapabilities = ProviderCapabilities.LocalOnly

    override suspend fun getHomeFeed(): HomeFeed = withContext(Dispatchers.Default) {
        val sections = mutableListOf<HomeSection>()

        // Quick Picks — always present: the full demo catalog as immediately playable tracks.
        sections.add(
            HomeSection(
                id = "quick_picks",
                title = "Quick Picks",
                type = HomeSectionType.QUICK_PICKS,
                items = DemoCatalog.tracks.map { it.toHomeItem() },
            ),
        )

        // Continue Listening — from demo data (first 2 tracks as "recently heard").
        sections.add(
            HomeSection(
                id = "continue_listening",
                title = "Continue Listening",
                type = HomeSectionType.CONTINUE_LISTENING,
                items = DemoCatalog.tracks.take(2).map { it.toHomeItem() },
            ),
        )

        // Albums
        sections.add(
            HomeSection(
                id = "albums",
                title = "Albums",
                type = HomeSectionType.ALBUMS,
                items = DemoCatalog.albums().map { album ->
                    HomeItem.AlbumItem(
                        album = Album(
                            id = "album:$album",
                            title = album,
                            artist = DemoCatalog.tracksForAlbum(album).firstOrNull()?.artist.orEmpty(),
                            year = DemoCatalog.tracksForAlbum(album).firstOrNull()?.year,
                            trackIds = DemoCatalog.tracksForAlbum(album).map { it.id },
                            source = ContentSource.LOCAL,
                            artworkMediaId = DemoCatalog.tracksForAlbum(album).firstOrNull()?.id,
                        ),
                    )
                },
            ),
        )

        // Artists
        sections.add(
            HomeSection(
                id = "artists",
                title = "Artists",
                type = HomeSectionType.ARTISTS,
                items = DemoCatalog.artists().map { artist ->
                    HomeItem.ArtistItem(
                        artist = Artist(
                            id = "artist:$artist",
                            name = artist,
                            trackIds = DemoCatalog.tracksForArtist(artist).map { it.id },
                            source = ContentSource.LOCAL,
                        ),
                    )
                },
            ),
        )

        HomeFeed(sections = sections)
    }

    override suspend fun search(query: String): SearchResults = withContext(Dispatchers.Default) {
        val tracks = DemoCatalog.search(query).map { it.toTrack() }
        SearchResults(
            query = query,
            tracks = tracks,
        )
    }

    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.Default) {
        DemoCatalog.albums().map { album ->
            val tracks = DemoCatalog.tracksForAlbum(album)
            Album(
                id = "album:$album",
                title = album,
                artist = tracks.firstOrNull()?.artist.orEmpty(),
                year = tracks.firstOrNull()?.year,
                trackIds = tracks.map { it.id },
                source = ContentSource.LOCAL,
                artworkMediaId = tracks.firstOrNull()?.id,
            )
        }
    }

    override suspend fun getAlbum(id: String): Album? =
        getAlbums().firstOrNull { it.id == id }

    override suspend fun getArtists(): List<Artist> = withContext(Dispatchers.Default) {
        DemoCatalog.artists().map { artist ->
            val tracks = DemoCatalog.tracksForArtist(artist)
            Artist(
                id = "artist:$artist",
                name = artist,
                trackIds = tracks.map { it.id },
                source = ContentSource.LOCAL,
            )
        }
    }

    override suspend fun getArtist(id: String): Artist? =
        getArtists().firstOrNull { it.id == id }

    override suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.Default) {
        // No local playlist support yet — honest empty.
        emptyList()
    }

    override suspend fun getLibrary(): MusicLibrary = MusicLibrary(
        playlists = emptyList(),
        albums = getAlbums(),
        artists = getArtists(),
    )

    override suspend fun getTrack(id: String): Track? = withContext(Dispatchers.Default) {
        DemoCatalog.trackById(id)?.toTrack()
    }

    override suspend fun resolvePlayback(track: Track): String? {
        val demo = DemoCatalog.trackById(track.id) ?: return null
        // Resource URIs are resolved by the playback layer with the package name.
        return "demo:${demo.resourceResName}"
    }
}

/** Normalized view of a demo track. */
fun DemoTrack.toTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    source = ContentSource.LOCAL,
    playbackKey = resourceResName,
    artworkMediaId = id,
)

private fun DemoTrack.toHomeItem(): HomeItem.TrackItem = HomeItem.TrackItem(toTrack())

/** Time-based greeting for the home screen. */
fun timeBasedGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        hour in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

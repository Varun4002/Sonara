package com.sonara.playback

/**
 * The five bundled ambient tracks that back playback until the catalog stage
 * arrives. Kept free of Android types so it is plain-JVM testable; resource
 * names are resolved to playable URIs by [PlayerConnection].
 */
data class DemoTrack(
    val id: String,
    val title: String,
    val artist: String,
    val resourceResName: String,
    val album: String,
    val year: Int,
    val trackNumber: Int,
    val durationMs: Long,
)

object DemoCatalog {

    val tracks: List<DemoTrack> = listOf(
        DemoTrack("aurora", "Aurora", "Sonara Sessions", "demo_aurora", "Northern Hours", 2026, 1, 14_000L),
        DemoTrack("drift", "Drift", "Sonara Sessions", "demo_drift", "Northern Hours", 2026, 2, 14_000L),
        DemoTrack("halo", "Halo", "Sonara Sessions", "demo_halo", "Low Light", 2026, 1, 14_000L),
        DemoTrack("lumen", "Lumen", "Sonara Sessions", "demo_lumen", "Low Light", 2026, 2, 14_000L),
        DemoTrack("vela", "Vela", "Sonara Sessions", "demo_vela", "Low Light", 2026, 3, 14_000L),
    )

    /** Stable media ids must be unique — the mini-player keys state off them. */
    fun trackById(mediaId: String): DemoTrack? = tracks.firstOrNull { it.id == mediaId }

    fun albums(): List<String> = tracks.map { it.album }.distinct()

    fun tracksForAlbum(album: String): List<DemoTrack> = tracks.filter { it.album == album }

    fun artists(): List<String> = tracks.map { it.artist }.distinct()

    fun tracksForArtist(artist: String): List<DemoTrack> = tracks.filter { it.artist == artist }

    /** Case-insensitive containment match across the fields a user can name. */
    fun search(query: String): List<DemoTrack> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return tracks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                it.album.contains(q, ignoreCase = true)
        }
    }
}

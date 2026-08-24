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
)

object DemoCatalog {

    val tracks: List<DemoTrack> = listOf(
        DemoTrack("aurora", "Aurora", "Sonara Sessions", "demo_aurora"),
        DemoTrack("drift", "Drift", "Sonara Sessions", "demo_drift"),
        DemoTrack("halo", "Halo", "Sonara Sessions", "demo_halo"),
        DemoTrack("lumen", "Lumen", "Sonara Sessions", "demo_lumen"),
        DemoTrack("vela", "Vela", "Sonara Sessions", "demo_vela"),
    )

    /** Stable media ids must be unique — the mini-player keys state off them. */
    fun trackById(mediaId: String): DemoTrack? = tracks.firstOrNull { it.id == mediaId }
}

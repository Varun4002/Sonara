package com.sonara.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DemoCatalogTest {

    @Test
    fun `catalog has unique ids and non-blank metadata`() {
        val tracks = DemoCatalog.tracks
        assertThat(tracks).isNotEmpty()
        assertThat(tracks.map { it.id }).containsNoDuplicates()
        tracks.forEach { track ->
            assertThat(track.title).isNotEmpty()
            assertThat(track.artist).isNotEmpty()
            assertThat(track.resourceResName).isNotEmpty()
        }
    }

    @Test
    fun `every catalog entry resolves by media id`() {
        DemoCatalog.tracks.forEach { track ->
            assertThat(DemoCatalog.trackById(track.id)).isEqualTo(track)
        }
        assertThat(DemoCatalog.trackById("does-not-exist")).isNull()
    }
}

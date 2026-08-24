package com.sonara.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DemoCatalogSearchTest {

    @Test
    fun `blank queries match nothing`() {
        assertThat(DemoCatalog.search("")).isEmpty()
        assertThat(DemoCatalog.search("   ")).isEmpty()
    }

    @Test
    fun `matches by title case-insensitively`() {
        val results = DemoCatalog.search("aur")
        assertThat(results.map { it.id }).containsExactly("aurora")
    }

    @Test
    fun `matches by artist`() {
        val results = DemoCatalog.search("sonara sessions")
        assertThat(results).hasSize(DemoCatalog.tracks.size)
    }

    @Test
    fun `matches by album`() {
        val results = DemoCatalog.search("northern")
        assertThat(results.map { it.id }).containsExactly("aurora", "drift").inOrder()
    }

    @Test
    fun `unknown terms match nothing`() {
        assertThat(DemoCatalog.search("zzz-not-a-track")).isEmpty()
    }

    @Test
    fun `albums and artists groupings are consistent`() {
        DemoCatalog.albums().forEach { album ->
            assertThat(DemoCatalog.tracksForAlbum(album)).isNotEmpty()
        }
        DemoCatalog.artists().forEach { artist ->
            assertThat(DemoCatalog.tracksForArtist(artist)).isNotEmpty()
        }
    }
}

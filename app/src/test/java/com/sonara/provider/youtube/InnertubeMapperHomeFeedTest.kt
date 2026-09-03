package com.sonara.provider.youtube

import com.google.common.truth.Truth.assertThat
import com.sonara.music.HomeItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InnertubeMapperHomeFeedTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun homeFeedSections_parsesPlaylistCarousels_whenNavigationLeavesArePrimitives() {
        val response = buildJsonObject {
            putJsonObject("contents") {
                putJsonObject("singleColumnBrowseResultsRenderer") {
                    putJsonArray("tabs") {
                        addJsonObject {
                            putJsonObject("tabRenderer") {
                                put("title", "Home")
                                putJsonObject("content") {
                                    putJsonObject("sectionListRenderer") {
                                        putJsonArray("contents") {
                                            addJsonObject {
                                                putJsonObject("musicCarouselShelfRenderer") {
                                                    putJsonObject("header") {
                                                        putJsonObject("musicCarouselShelfBasicHeaderRenderer") {
                                                            putJsonObject("title") {
                                                                putJsonArray("runs") {
                                                                    addJsonObject { put("text", "Recommended") }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    putJsonArray("contents") {
                                                        addJsonObject {
                                                            putJsonObject("musicTwoRowItemRenderer") {
                                                                putJsonObject("title") {
                                                                    putJsonArray("runs") {
                                                                        addJsonObject { put("text", "Rain Therapy") }
                                                                    }
                                                                }
                                                                putJsonObject("subtitle") {
                                                                    putJsonArray("runs") {
                                                                        addJsonObject { put("text", "Artist One") }
                                                                    }
                                                                }
                                                                putJsonObject("navigationEndpoint") {
                                                                    putJsonObject("browseEndpoint") {
                                                                        put("browseId", "VLRDCLAK5uy_qwerty")
                                                                        putJsonObject("browseEndpointContextSupportedConfigs") {
                                                                            putJsonObject("browseEndpointContextMusicConfig") {
                                                                                put("pageType", "MUSIC_PAGE_TYPE_PLAYLIST")
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val sections = InnertubeMapper.homeFeedSections(response)

        assertThat(sections).hasSize(1)
        val section = sections.single()
        assertThat(section.title).isEqualTo("Recommended")
        assertThat(section.items).hasSize(1)
        val item = section.items.single()
        assertThat(item).isInstanceOf(HomeItem.PlaylistItem::class.java)
        assertThat((item as HomeItem.PlaylistItem).playlist.id).isEqualTo("VLRDCLAK5uy_qwerty")
        assertThat(item.playlist.title).isEqualTo("Rain Therapy")
    }

    @Test
    fun homeFeedSections_parsesTrackCarousel_whenWatchEndpointPresent() {
        val response = buildJsonObject {
            putJsonObject("contents") {
                putJsonObject("singleColumnBrowseResultsRenderer") {
                    putJsonArray("tabs") {
                        addJsonObject {
                            putJsonObject("tabRenderer") {
                                putJsonObject("content") {
                                    putJsonObject("sectionListRenderer") {
                                        putJsonArray("contents") {
                                            addJsonObject {
                                                putJsonObject("musicCarouselShelfRenderer") {
                                                    putJsonObject("header") {
                                                        putJsonObject("musicCarouselShelfBasicHeaderRenderer") {
                                                            putJsonObject("title") {
                                                                putJsonArray("runs") {
                                                                    addJsonObject { put("text", "Songs") }
                                                                }
                                                            }
                                                        }
                                                    }
                                                    putJsonArray("contents") {
                                                        addJsonObject {
                                                            putJsonObject("musicTwoRowItemRenderer") {
                                                                putJsonObject("title") {
                                                                    putJsonArray("runs") {
                                                                        addJsonObject { put("text", "Track Name") }
                                                                    }
                                                                }
                                                                putJsonObject("navigationEndpoint") {
                                                                    putJsonObject("watchEndpoint") {
                                                                        put("videoId", "abc123")
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val sections = InnertubeMapper.homeFeedSections(response)

        val item = sections.single().items.single()
        assertThat(item).isInstanceOf(HomeItem.TrackItem::class.java)
        assertThat((item as HomeItem.TrackItem).track.id).isEqualTo("abc123")
        assertThat(item.track.title).isEqualTo("Track Name")
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(
        key: String,
        block: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit,
    ) {
        put(key, kotlinx.serialization.json.buildJsonArray(block))
    }

    private fun kotlinx.serialization.json.JsonArrayBuilder.addJsonObject(
        block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
    ) {
        add(kotlinx.serialization.json.buildJsonObject(block))
    }
}

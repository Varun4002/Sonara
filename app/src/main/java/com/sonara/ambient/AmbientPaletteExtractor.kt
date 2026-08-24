package com.sonara.ambient

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

/**
 * Artwork → [AmbientPalette]. Thin wrapper over androidx Palette so the tonal
 * processing in [buildAmbientPalette] stays pure and testable. Extraction runs
 * off the UI thread; failure returns neutral and the environment stays calm.
 */
object AmbientPaletteExtractor {

    fun extract(bitmap: Bitmap): AmbientPalette = try {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val candidates = buildList {
            palette.dominantSwatch?.rgb?.let { add(it) }
            palette.vibrantSwatch?.rgb?.let { add(it) }
            palette.mutedSwatch?.rgb?.let { add(it) }
            palette.lightVibrantSwatch?.rgb?.let { add(it) }
            palette.darkVibrantSwatch?.rgb?.let { add(it) }
        }.distinct().map { androidx.compose.ui.graphics.Color(it) }
        buildAmbientPalette(candidates)
    } catch (_: Exception) {
        AmbientPalette.Neutral
    }
}

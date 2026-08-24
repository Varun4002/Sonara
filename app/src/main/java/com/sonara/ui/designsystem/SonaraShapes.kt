package com.sonara.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner-radius tokens. [pill] uses percent-based rounding so it stays a full
 * pill at any height.
 */
object SonaraShapes {
    val small = RoundedCornerShape(10.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(24.dp)
    val card = RoundedCornerShape(20.dp)
    val player = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val pill = RoundedCornerShape(50)

    /** Mapping into Material's shape slots so M3 components inherit the scale. */
    fun asMaterialShapes(): Shapes = Shapes(
        extraSmall = small,
        small = medium,
        medium = card,
        large = large,
        extraLarge = player,
    )
}

package com.heofen.botgram.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

typealias BotgramBackdrop = LayerBackdrop

/** Общий backdrop для жидкого стекла поверх основного контента. */
@Composable
fun rememberBotgramBackdrop(): BotgramBackdrop {
    val backgroundColor = MaterialTheme.colorScheme.background
    return rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
}

fun Modifier.botgramBackdropSource(backdrop: BotgramBackdrop): Modifier =
    layerBackdrop(backdrop)

/** Единый liquid-glass стиль для стеклянных панелей приложения. */
@Composable
fun Modifier.botgramLiquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    blurRadius: Dp = 1.dp,
    lensEnabled: Boolean = shape is CornerBasedShape
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return background(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            shape = shape
        )
    }

    val surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)

    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurRadius.toPx())
            if (lensEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lens(
                    refractionHeight = 16.dp.toPx(),
                    refractionAmount = 32.dp.toPx(),
                    depthEffect = true
                )
            }
        },
        onDrawSurface = { drawRect(surface) }
    )
}

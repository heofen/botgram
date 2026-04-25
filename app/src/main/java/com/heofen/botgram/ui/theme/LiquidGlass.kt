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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.RectangleShape

typealias BotgramBackdrop = LayerBackdrop

/** Backdrop для стеклянных панелей (кнопки, пилюля имени). */
@Composable
fun rememberBotgramBackdrop(): BotgramBackdrop {
    val backgroundColor = MaterialTheme.colorScheme.background
    return rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
}

/**
 * Отдельный backdrop для прогрессивного блюра*/
@Composable
fun rememberBotgramMonetBlurBackdrop(): BotgramBackdrop {
    val backgroundColor = MaterialTheme.colorScheme.background
    val monetPrimary   = MaterialTheme.colorScheme.primaryContainer
    val monetSecondary = MaterialTheme.colorScheme.secondaryContainer
    return rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    monetPrimary.copy(alpha = 0.28f),
                    monetSecondary.copy(alpha = 0.14f),
                    Color.Transparent
                )
            )
        )
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

/**
 * Прогрессивный блюр для шапки профиля.
 */
@Composable
fun Modifier.botgramProgressiveBlur(
    backdrop: Backdrop,
    blurRadius: Dp = 55.dp
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return this.background(
            Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    Color.Transparent
                )
            )
        )
    }

    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Black.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                ),
                blendMode = BlendMode.DstIn
            )
        }
        .drawBackdrop(
            backdrop = backdrop,
            shape = { RectangleShape },
            effects = {
                blur(blurRadius.toPx())
            },
            onDrawSurface = {}
        )
}

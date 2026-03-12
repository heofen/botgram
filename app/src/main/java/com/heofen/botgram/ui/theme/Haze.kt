package com.heofen.botgram.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

@Composable
fun botgramHazeStyle(): HazeStyle = HazeStyle(
    blurRadius = 20.dp,
    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
    tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)),
    noiseFactor = 0.05f
)

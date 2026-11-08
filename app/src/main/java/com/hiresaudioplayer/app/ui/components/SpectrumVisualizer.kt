package com.hiresaudioplayer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hiresaudioplayer.app.ui.theme.CyanAccent
import com.hiresaudioplayer.app.ui.theme.GoldAccent

@Composable
fun SpectrumVisualizer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    barCount: Int = 48
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height

        val step = width / barCount
        val barWidth = step * 0.65f
        val maxIndex = magnitudes.size.coerceAtMost(barCount)

        val gradient = Brush.verticalGradient(
            colors = listOf(GoldAccent, CyanAccent),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until barCount) {
            val mag = if (i < maxIndex) magnitudes[i] else 0.05f
            val barHeight = (mag * canvasHeight).coerceIn(4f, canvasHeight)

            val x = i * step + (step - barWidth) / 2f
            val y = canvasHeight - barHeight

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}


package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    shadowElevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // Elegant translucent glass colors
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0C1017UL
    
    val glassBg = if (isDark) {
        Color(0x991E293B) // Dark slate with transparency
    } else {
        Color(0xB3FFFFFF) // Light mode semi-transparent white
    }
    
    val glassBorder = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x33FFFFFF),
                Color(0x1AFFFFFF),
                Color(0x0DFFFFFF)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0x80FFFFFF),
                Color(0x4DFFFFFF),
                Color(0x1A6750A4)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .background(
                color = glassBg,
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                border = BorderStroke(borderWidth, glassBorder),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

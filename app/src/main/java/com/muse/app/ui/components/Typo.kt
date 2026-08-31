package com.muse.app.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muse.app.ui.theme.MuseColors

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MuseColors.White,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.SansSerif,
            color = color
        )
    )
}

@Composable
fun MetaText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MuseColors.Gray2,
    size: TextUnit = 10.sp,
    letterSpacing: TextUnit = 2.sp,
    align: TextAlign = TextAlign.Start,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = TextStyle(
            fontSize = size,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            fontFamily = FontFamily.Monospace,
            textAlign = align,
            color = color
        ),
        maxLines = 2
    )
}

@Composable
fun Hairline(color: Color, modifier: Modifier = Modifier, thicknessDp: Float = 1f) {
    Box(modifier.fillMaxWidth().height(thicknessDp.dp).background(color))
}

@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaleDown: Float = 0.97f,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "press"
    )
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        content()
    }
}

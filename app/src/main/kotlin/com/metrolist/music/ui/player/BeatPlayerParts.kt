/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import com.metrolist.music.ui.theme.BeatNeonLine
import com.metrolist.music.ui.theme.BeatStyle
import com.metrolist.music.ui.theme.LocalBeatLook
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Full-screen player background: a deep base tinted by the cover, two soft glows in the cover's
 * colors (top-left and right), and a darker floor so the controls stay readable.
 * [colors] is [glow, secondGlow, base] from [com.metrolist.music.ui.theme.PlayerColorExtractor].
 */
@Composable
fun BeatGlowBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val glow = colors[0]
    val secondGlow = colors.getOrElse(1) { glow }
    val base = colors.getOrElse(2) { Color.Black }
    val look = LocalBeatLook.current
    if (look.style == BeatStyle.NEON) {
        // Black room with a lit grid; the cover's color only glows behind the artwork.
        Box(
            modifier = modifier.drawBehind {
                drawRect(Color(0xFF05060B))
                val step = 28.dp.toPx()
                val line = glow.copy(alpha = 0.07f)
                var x = 0f
                while (x < size.width) {
                    drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(glow.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.3f),
                        radius = size.width * 0.8f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(look.accent.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height),
                        radius = size.width * 0.75f,
                    ),
                )
            },
        )
        return
    }
    if (look.style == BeatStyle.BENTO) {
        // Solid color from the cover at the top, fading to pure black where the controls are.
        Box(
            modifier = modifier.drawBehind {
                drawRect(Color.Black)
                drawRect(
                    Brush.verticalGradient(
                        0f to lerp(glow, Color.Black, 0.15f),
                        0.45f to lerp(base, Color.Black, 0.5f),
                        0.72f to Color.Black,
                    ),
                )
            },
        )
        return
    }
    Box(
        modifier = modifier.drawBehind {
            drawRect(base)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = 0.85f), glow.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.06f),
                    radius = size.width * 1.25f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    colors = listOf(secondGlow.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 1.05f, size.height * 0.42f),
                    radius = size.width * 0.95f,
                ),
            )
            drawRect(
                Brush.verticalGradient(
                    0.45f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.6f),
                ),
            )
        },
    )
}

/** Round player button that dips slightly when pressed. */
@Composable
fun BeatRoundButton(
    @DrawableRes icon: Int,
    label: String,
    size: Dp,
    iconSize: Dp,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val look = LocalBeatLook.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "beatButtonScale",
    )
    val styledContainer = when (look.style) {
        BeatStyle.CLASSIC -> containerColor
        BeatStyle.GLASS -> if (primary) Color.White else Color.White.copy(alpha = 0.06f + 0.14f * look.glassOpacity)
        BeatStyle.NEON -> if (primary) look.accent.copy(alpha = 0.16f) else Color(0xFF0A0E17)
        BeatStyle.BENTO -> if (primary) Color.White else Color(0xFF1C1C1F)
    }
    val styledContent = when (look.style) {
        BeatStyle.CLASSIC -> contentColor
        BeatStyle.NEON -> if (primary) look.accent else Color.White
        else -> if (primary) Color(0xFF111111) else Color.White
    }
    val shape = if (look.style == BeatStyle.NEON) RoundedCornerShape(if (primary) 20.dp else 16.dp) else CircleShape
    val container by animateColorAsState(styledContainer, label = "beatButtonContainer")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (look.style == BeatStyle.NEON && primary) {
                    Modifier.shadow(20.dp, shape, clip = false, ambientColor = look.accent, spotColor = look.accent)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(container)
            .then(
                when {
                    look.style == BeatStyle.NEON ->
                        Modifier.border(if (primary) 1.5.dp else 1.dp, if (primary) look.accent else BeatNeonLine, shape)
                    look.style == BeatStyle.GLASS && !primary ->
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.28f), shape)
                    else -> Modifier
                },
            )
            .alpha(if (enabled) 1f else 0.4f)
            .semantics { contentDescription = label }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = styledContent),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = styledContent,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Shuffle / repeat: icon only, full strength with a small dot underneath when active. */
@Composable
fun BeatToggleButton(
    @DrawableRes icon: Int,
    active: Boolean,
    color: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha by animateFloatAsState(if (active) 1f else 0.5f, label = "beatToggleAlpha")
    val dot by animateFloatAsState(if (active) 1f else 0f, label = "beatToggleDot")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics { contentDescription = label }
            .toggleable(value = active, enabled = enabled, role = Role.Switch, onValueChange = { onClick() }),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(6.dp))
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(24.dp)
                    .alpha(if (enabled) alpha else 0.3f),
            )
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .scale(dot)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

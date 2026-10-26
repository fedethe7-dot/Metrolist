/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BeatTheme
import com.metrolist.music.ui.theme.BeatThemes

/** Colors offered for the "Personalizzato" theme: saturated tones that keep white text readable. */
private val BeatSwatches = listOf(
    0xFFFF3B30, 0xFFFF5E62, 0xFFFF2E93, 0xFFE040FB, 0xFF9B5CFF, 0xFF6A5CFF,
    0xFF3B5BFF, 0xFF1E88E5, 0xFF00B4D8, 0xFF00D4C8, 0xFF11C77E, 0xFF9BE15D,
    0xFFFFD60A, 0xFFFFB020, 0xFFFF7A00, 0xFFB3191B, 0xFF8E9AA6, 0xFFB5DAFF,
).map { Color(it) }

@Composable
fun BeatThemePickerDialog(
    selectedId: String,
    customA: Color,
    customB: Color,
    onSelect: (BeatTheme) -> Unit,
    onSaveCustom: (Color, Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(20.dp),
        ) {
            AnimatedContent(targetState = editing, label = "beatThemeEditor") { isEditing ->
                if (isEditing) {
                    BeatCustomThemeEditor(
                        initialA = customA,
                        initialB = customB,
                        onBack = { editing = false },
                        onSave = { a, b ->
                            onSaveCustom(a, b)
                            editing = false
                        },
                    )
                } else {
                    BeatThemeGrid(
                        selectedId = selectedId,
                        custom = BeatThemes.custom(customA, customB),
                        onSelect = onSelect,
                        onCustomClick = { editing = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun BeatThemeGrid(
    selectedId: String,
    custom: BeatTheme,
    onSelect: (BeatTheme) -> Unit,
    onCustomClick: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.beat_themes_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(BeatThemes.All, key = { it.id }) { theme ->
                BeatThemeTile(
                    theme = theme,
                    selected = theme.id == selectedId,
                    onClick = { onSelect(theme) },
                )
            }
            item(key = BeatThemes.CUSTOM_ID) {
                BeatThemeTile(
                    theme = custom,
                    selected = selectedId == BeatThemes.CUSTOM_ID,
                    badgeIcon = R.drawable.palette,
                    onClick = onCustomClick,
                )
            }
        }
    }
}

@Composable
private fun BeatThemeTile(
    theme: BeatTheme,
    selected: Boolean,
    onClick: () -> Unit,
    badgeIcon: Int? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(theme.background)
                .background(
                    Brush.linearGradient(
                        if (theme.gradient.size > 1) theme.gradient else theme.gradient + theme.gradient,
                    ),
                    alpha = 0.9f,
                )
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(18.dp),
                ),
        ) {
            val icon = if (selected) R.drawable.check else badgeIcon
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(theme.nameRes),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun BeatCustomThemeEditor(
    initialA: Color,
    initialB: Color,
    onBack: () -> Unit,
    onSave: (Color, Color) -> Unit,
) {
    var colorA by remember { mutableStateOf(initialA) }
    var colorB by remember { mutableStateOf(initialB) }
    val preview = BeatThemes.custom(colorA, colorB)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.dismiss),
                )
            }
            Text(
                text = stringResource(R.string.beat_theme_custom),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(preview.background)
                .background(Brush.linearGradient(preview.gradient), alpha = 0.9f),
        )
        Spacer(Modifier.height(16.dp))
        BeatSwatchRow(
            title = stringResource(R.string.beat_custom_color_1),
            selected = colorA,
            onPick = { colorA = it },
        )
        Spacer(Modifier.height(14.dp))
        BeatSwatchRow(
            title = stringResource(R.string.beat_custom_color_2),
            selected = colorB,
            onPick = { colorB = it },
        )
        Spacer(Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onBack) { Text(stringResource(android.R.string.cancel)) }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onSave(colorA, colorB) }) { Text(stringResource(R.string.beat_custom_apply)) }
        }
    }
}

@Composable
private fun BeatSwatchRow(
    title: String,
    selected: Color,
    onPick: (Color) -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        BeatSwatches.chunked(6).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
            ) {
                row.forEach { color ->
                    val isSelected = color.toArgb() == selected.toArgb()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable { onPick(color) },
                    ) {
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

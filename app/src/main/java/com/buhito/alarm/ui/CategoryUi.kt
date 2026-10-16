package com.buhito.alarm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buhito.alarm.R

private val EMOJIS = listOf(
    "🏠", "🎓", "🍽️", "💊", "🧘", "🏃", "🎮", "🎵", "🛒", "🐶", "🚌", "💧", "🌱", "☕", "📞", "🎂",
)
private val COLORS = listOf(
    0xFFFF8A9B, 0xFFFFB86B, 0xFFFFE066, 0xFF8BE3A0, 0xFF6FE0D6, 0xFF7EC8FF, 0xFFB59CFF, 0xFFF29BE6,
)

/** Diálogo para crear una categoría propia: nombre, emoji y color. */
@Composable
fun NewCategoryDialog(onCreate: (name: String, emoji: String, color: Long) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(EMOJIS.first()) }
    var color by remember { mutableLongStateOf(COLORS.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.category_name)) },
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (e in EMOJIS) {
                        Pill(text = e, selected = emoji == e, onClick = { emoji = e }, accent = Color(color))
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in COLORS) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (color == c) Text("✓", color = Color.Black)
                        }
                    }
                }
                // vista previa
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Surface(shape = CircleShape, color = Color(color).copy(alpha = 0.18f)) {
                        Text(
                            text = "$emoji ${name.ifBlank { stringResource(R.string.category_name) }}",
                            color = Color(color),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name.trim(), emoji, color) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

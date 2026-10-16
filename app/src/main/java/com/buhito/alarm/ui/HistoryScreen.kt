package com.buhito.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buhito.alarm.R
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.data.HistoryEntry
import com.buhito.alarm.data.RingResult
import com.buhito.alarm.data.resolveCategory
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.util.clockText
import com.buhito.alarm.util.dayLabel
import com.buhito.alarm.util.timeText

/** Pestaña Historial: cada vez que sonó una alarma y qué hiciste (detener, posponer o dejarla pasar). */
@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    customs: List<CustomCategory>,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60_000L
    val week = entries.filter { it.firedAt >= weekAgo }
    val stopped = week.count { it.result == RingResult.STOPPED }
    val missed = week.count { it.result == RingResult.MISSED }
    val owlMood = when {
        week.isEmpty() -> OwlMood.Sleepy
        missed > stopped -> OwlMood.Calm
        else -> OwlMood.Happy
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.history_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.history_week, stopped, missed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                OwlMascot(Modifier.size(96.dp), mood = owlMood)
            }
        }
        if (entries.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                )
            }
        }
        items(entries, key = { it.id }) { entry ->
            val category = resolveCategory(context, entry.category, customs)
            val (icon, labelRes, color) = when (entry.result) {
                RingResult.STOPPED -> Triple("✅", R.string.result_stopped, Color(0xFF7BE0B0))
                RingResult.SNOOZED -> Triple("💤", R.string.result_snoozed, MaterialTheme.colorScheme.primary)
                RingResult.MISSED -> Triple("😴", R.string.result_missed, MaterialTheme.colorScheme.error)
                else -> Triple("🔔", R.string.result_ringing, MaterialTheme.colorScheme.secondary)
            }
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, style = MaterialTheme.typography.headlineMedium)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            text = "${clockText(context, entry.hour, entry.minute)}  ·  " +
                                entry.label.ifBlank { stringResource(R.string.alarm_default_title) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        Text(
                            text = "${dayLabel(context, entry.firedAt)}, ${timeText(context, entry.firedAt)}  ·  " +
                                "${category.emoji} ${category.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge, color = color)
                }
            }
        }
        if (entries.isNotEmpty()) {
            item(key = "clear") {
                TextButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.history_clear), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    confirmClear = false
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

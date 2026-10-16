package com.buhito.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buhito.alarm.R
import com.buhito.alarm.data.Stopwatch
import com.buhito.alarm.ui.mascots.StopwatchCat
import com.buhito.alarm.util.formatStopwatch
import kotlinx.coroutines.delay

/** Pestaña de cronómetros: varios cronómetros guardados, cada uno con vueltas, y el gatito arriba. */
@Composable
fun StopwatchScreen(
    stopwatches: List<Stopwatch>,
    onCreate: (String) -> Unit,
    onToggle: (Stopwatch) -> Unit,
    onLap: (Stopwatch) -> Unit,
    onReset: (Stopwatch) -> Unit,
    onDelete: (Stopwatch) -> Unit,
) {
    val anyRunning = stopwatches.any { it.running }
    val runningCount = stopwatches.count { it.running }

    // Reloj compartido: late rápido (décimas) solo si algo corre; si no, casi no gasta.
    val now by produceState(System.currentTimeMillis(), anyRunning) {
        while (true) {
            value = System.currentTimeMillis()
            delay(if (anyRunning) 50 else 1000)
        }
    }

    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Stopwatch?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                            text = stringResource(R.string.sw_title),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = if (runningCount > 0) stringResource(R.string.sw_running_n, runningCount)
                            else stringResource(R.string.sw_all_paused),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    StopwatchCat(running = anyRunning, modifier = Modifier.width(150.dp))
                }
            }

            if (stopwatches.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.sw_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            stringResource(R.string.sw_empty_body),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            items(stopwatches, key = { it.id }) { sw ->
                StopwatchCard(
                    sw = sw,
                    now = now,
                    onToggle = { onToggle(sw) },
                    onLap = { onLap(sw) },
                    onReset = { onReset(sw) },
                    onDelete = { pendingDelete = sw },
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { creating = true },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.sw_new), style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        )
    }

    if (creating) {
        val defaultName = stringResource(R.string.sw_default_name, stopwatches.size + 1)
        var name by remember { mutableStateOf(defaultName) }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.sw_new)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    label = { Text(stringResource(R.string.sw_name_hint)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCreate(name.ifBlank { defaultName })
                    creating = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    pendingDelete?.let { sw ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.sw_delete_title)) },
            text = { Text(sw.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(sw)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun StopwatchCard(
    sw: Stopwatch,
    now: Long,
    onToggle: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val elapsed = sw.elapsed(now)
    val laps = sw.lapList()
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PanelTitle(sw.name)
                Text(
                    text = if (sw.running) stringResource(R.string.sw_running) else stringResource(R.string.sw_paused),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sw.running) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = formatStopwatch(elapsed),
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (sw.running) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onToggle,
                modifier = Modifier.weight(1.4f),
            ) {
                Text(
                    stringResource(
                        when {
                            sw.running -> R.string.sw_pause
                            elapsed > 0 -> R.string.sw_resume
                            else -> R.string.sw_start
                        },
                    ),
                )
            }
            OutlinedButton(onClick = onLap, enabled = sw.running, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.sw_lap))
            }
            OutlinedButton(onClick = onReset, enabled = elapsed > 0, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.sw_reset))
            }
        }
        if (laps.isNotEmpty()) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // las 4 vueltas más recientes (la última arriba)
                val shown = laps.indices.reversed().take(4)
                for (i in shown) {
                    val split = laps[i] - (if (i > 0) laps[i - 1] else 0L)
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.sw_lap_n, i + 1),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text("+" + formatStopwatch(split), fontFamily = FontFamily.Monospace)
                        Text(
                            formatStopwatch(laps[i]),
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                }
                if (laps.size > shown.size) {
                    Text(
                        stringResource(R.string.sw_more_laps, laps.size - shown.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

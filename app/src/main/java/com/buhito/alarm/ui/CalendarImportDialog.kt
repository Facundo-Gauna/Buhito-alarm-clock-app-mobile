package com.buhito.alarm.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buhito.alarm.R
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.data.allCategories
import com.buhito.alarm.engine.CalendarEvent
import com.buhito.alarm.engine.CalendarSource
import com.buhito.alarm.util.Prefs
import com.buhito.alarm.util.dayLabel
import com.buhito.alarm.util.timeText

/**
 * Elegís eventos de tu calendario y se crean alarmas de una sola vez: a la hora del evento
 * o unos minutos antes, con el título como etiqueta.
 */
@Composable
fun CalendarImportDialog(
    customs: List<CustomCategory>,
    onCreate: (events: List<CalendarEvent>, minutesBefore: Int, category: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(CalendarSource.hasPermission(context)) }
    var events by remember { mutableStateOf<List<CalendarEvent>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var minutesBefore by remember { mutableIntStateOf(15) }
    var category by remember { mutableStateOf("GENERAL") }
    var days by remember { mutableIntStateOf(Prefs.calendarDays(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(granted, days) {
        if (granted) {
            events = null
            events = CalendarSource.upcoming(context, days)
        } else {
            launcher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (granted) {
                    // Cuánto hacia adelante mirar: se recuerda para la próxima vez.
                    Text(stringResource(R.string.cal_range), style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (d in intArrayOf(7, 14, 30, 60, 90, 180, 365)) {
                            Pill(
                                text = if (d == 365) stringResource(R.string.cal_one_year) else stringResource(R.string.cal_days, d),
                                selected = days == d,
                                onClick = {
                                    days = d
                                    Prefs.setCalendarDays(context, d)
                                },
                            )
                        }
                    }
                }
                when {
                    !granted -> Text(stringResource(R.string.cal_denied))
                    events == null -> Text(stringResource(R.string.cal_loading))
                    events!!.isEmpty() -> Text(stringResource(R.string.cal_empty))
                    else -> {
                        Column(
                            Modifier
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            for (e in events!!) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { selected = if (e.key in selected) selected - e.key else selected + e.key },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = e.key in selected,
                                        onCheckedChange = { selected = if (it) selected + e.key else selected - e.key },
                                    )
                                    Column(Modifier.padding(start = 4.dp)) {
                                        Text(e.title, maxLines = 1)
                                        Text(
                                            "${dayLabel(context, e.beginMillis)}, ${timeText(context, e.beginMillis)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        Text(stringResource(R.string.cal_remind), style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (m in intArrayOf(0, 10, 15, 30, 60)) {
                                Pill(
                                    text = if (m == 0) stringResource(R.string.cal_on_time) else stringResource(R.string.cal_before, m),
                                    selected = minutesBefore == m,
                                    onClick = { minutesBefore = m },
                                )
                            }
                        }
                        Text(stringResource(R.string.category_title), style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in allCategories(context, customs)) {
                                Pill(
                                    text = "${c.emoji} ${c.name}",
                                    selected = category == c.key,
                                    onClick = { category = c.key },
                                    accent = Color(c.color),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = { onCreate(events.orEmpty().filter { it.key in selected }, minutesBefore, category) },
            ) { Text(stringResource(R.string.cal_create, selected.size)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

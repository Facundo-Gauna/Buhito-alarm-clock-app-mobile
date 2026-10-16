package com.buhito.alarm.ui

import android.text.format.DateFormat
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.buhito.alarm.engine.SleepDetector
import com.buhito.alarm.engine.SleepScheduler
import com.buhito.alarm.util.Prefs
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buhito.alarm.R
import com.buhito.alarm.data.SleepRecord
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.ui.theme.BuhitoColors
import com.buhito.alarm.util.TimeUtil
import com.buhito.alarm.util.avgHoursPerMonth
import com.buhito.alarm.util.clockText
import com.buhito.alarm.util.dayLabel
import com.buhito.alarm.util.formatSleep
import com.buhito.alarm.util.hoursPerDay
import com.buhito.alarm.util.inMonth
import com.buhito.alarm.util.inYear
import com.buhito.alarm.util.minuteText
import com.buhito.alarm.util.summarize
import com.buhito.alarm.util.wakeDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val GOAL_HOURS = 8f
private val FACES = listOf("😫", "😕", "😐", "🙂", "😄")

/** Pestaña Sueño: registrar cuándo te dormís y despertás, y ver promedios del mes, del año y totales. */
@Composable
fun SleepScreen(
    records: List<SleepRecord>,
    onDetect: suspend () -> Int,
    onSave: (SleepRecord) -> Unit,
    onDelete: (SleepRecord) -> Unit,
) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 mes, 1 año, 2 total
    val today = remember { LocalDate.now() }
    var monthIndex by rememberSaveable { mutableIntStateOf(today.year * 12 + today.monthValue - 1) }
    var year by rememberSaveable { mutableIntStateOf(today.year) }
    var editing by remember { mutableStateOf<SleepRecord?>(null) }
    var showManual by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.sleep_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    stringResource(R.string.sleep_awake),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            OwlMascot(Modifier.size(96.dp), mood = OwlMood.Sleepy)
        }

        DetectionPanel(onDetect = onDetect, onAddManual = { showManual = true })

        // Selector de período
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(stringResource(R.string.sleep_month), mode == 0, { mode = 0 })
            Pill(stringResource(R.string.sleep_year), mode == 1, { mode = 1 })
            Pill(stringResource(R.string.sleep_total), mode == 2, { mode = 2 })
        }

        when (mode) {
            0 -> MonthView(
                records = records,
                month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1),
                onPrev = { monthIndex-- },
                onNext = { if (monthIndex < today.year * 12 + today.monthValue - 1) monthIndex++ },
                onEdit = { editing = it },
            )
            1 -> YearView(records, year, onPrev = { year-- }, onNext = { if (year < today.year) year++ })
            else -> TotalView(records)
        }
        Box(Modifier.height(16.dp))
    }

    if (showManual || editing != null) {
        SleepEditDialog(
            initial = editing,
            onSave = {
                onSave(it)
                showManual = false
                editing = null
            },
            onDelete = editing?.let { rec ->
                {
                    onDelete(rec)
                    editing = null
                }
            },
            onDismiss = {
                showManual = false
                editing = null
            },
        )
    }
}

// ---------- Detección automática ----------

@Composable
private fun DetectionPanel(onDetect: suspend () -> Int, onAddManual: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var auto by remember { mutableStateOf(Prefs.sleepAuto(context)) }
    var minHours by remember { mutableIntStateOf(Prefs.sleepMinHours(context)) }
    var delayMin by remember { mutableIntStateOf(Prefs.sleepDelayMin(context)) }
    var bedFrom by remember { mutableIntStateOf(Prefs.sleepBedFromHour(context)) }
    var checkHour by remember { mutableIntStateOf(Prefs.sleepCheckHour(context)) }
    var showAdjust by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // El permiso se da en Ajustes de Android: se vuelve a leer cada vez que se vuelve a la app.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val hasAccess = remember(refresh) { SleepDetector.hasAccess(context) }

    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PanelTitle(stringResource(R.string.sleep_auto_title), icon = "🌙")
                Text(
                    stringResource(R.string.sleep_auto_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(
                checked = auto,
                onCheckedChange = {
                    auto = it
                    Prefs.setSleepAuto(context, it)
                    SleepScheduler.schedule(context)
                },
            )
        }

        if (!SleepDetector.supported) {
            Text(
                stringResource(R.string.sleep_auto_unsupported),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        } else if (auto && !hasAccess) {
            Text(
                stringResource(R.string.sleep_auto_need_access),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
            Button(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(stringResource(R.string.sleep_auto_grant)) }
        } else if (auto) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            val n = onDetect()
                            busy = false
                            Toast.makeText(
                                context,
                                if (n > 0) context.getString(R.string.sleep_detected_n, n) else context.getString(R.string.sleep_detected_none),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    },
                ) { Text(stringResource(R.string.sleep_detect_now)) }
                TextButton(onClick = { showAdjust = !showAdjust }) {
                    Text(stringResource(if (showAdjust) R.string.sleep_hide_adjust else R.string.sleep_adjust))
                }
            }
            if (showAdjust) {
                AdjustRow(stringResource(R.string.sleep_adj_min), listOf(3, 4, 5), minHours, { "$it h" }) {
                    minHours = it
                    Prefs.setSleepMinHours(context, it)
                }
                AdjustRow(stringResource(R.string.sleep_adj_delay), listOf(0, 10, 20, 30), delayMin, { "$it min" }) {
                    delayMin = it
                    Prefs.setSleepDelayMin(context, it)
                }
                AdjustRow(stringResource(R.string.sleep_adj_from), listOf(18, 20, 22), bedFrom, { "%02d:00".format(it) }) {
                    bedFrom = it
                    Prefs.setSleepBedFromHour(context, it)
                }
                AdjustRow(stringResource(R.string.sleep_adj_check), listOf(7, 8, 9, 10, 11, 12), checkHour, { "%02d:00".format(it) }) {
                    checkHour = it
                    Prefs.setSleepCheckHour(context, it)
                    SleepScheduler.schedule(context)
                }
            }
        }
        TextButton(onClick = onAddManual, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.sleep_add_manual))
        }
    }
}

@Composable
private fun AdjustRow(label: String, options: List<Int>, selected: Int, text: (Int) -> String, onSelect: (Int) -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp),
    )
    Row(
        Modifier
            .padding(top = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (o in options) Pill(text(o), selected == o, { onSelect(o) })
    }
}

// ---------- Vistas ----------

@Composable
private fun PeriodNav(title: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrev) { Text("◀", fontSize = 18.sp) }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onNext) { Text("▶", fontSize = 18.sp) }
    }
}

@Composable
private fun MonthView(
    records: List<SleepRecord>,
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onEdit: (SleepRecord) -> Unit,
) {
    val context = LocalContext.current
    val inMonth = records.inMonth(month)
    val summary = summarize(inMonth)
    val title = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
        .replaceFirstChar { it.uppercase() }
    PeriodNav(title, onPrev, onNext)
    if (summary == null) {
        EmptyNote()
        return
    }
    SummaryTiles(summary.avgMs, summary.avgBedMinute, summary.avgWakeMinute, summary.count, summary.avgQuality)
    Panel(Modifier.fillMaxWidth()) {
        PanelTitle(stringResource(R.string.sleep_hours_per_night))
        val values = records.hoursPerDay(month)
        BarChart(
            values = values,
            labels = values.indices.map { if ((it + 1) % 5 == 0 || it == 0) "${it + 1}" else "" },
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(R.string.sleep_goal_hint, GOAL_HOURS.toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    Panel(Modifier.fillMaxWidth()) {
        PanelTitle(stringResource(R.string.sleep_nights))
        if (inMonth.any { it.auto }) {
            Text(
                stringResource(R.string.sleep_auto_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        for (r in inMonth.sortedByDescending { it.wakeAt }) {
            val bed = TimeUtil.toLocal(r.sleepAt)
            val wake = TimeUtil.toLocal(r.wakeAt)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEdit(r) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(dayLabel(context, r.wakeDate()), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${clockText(context, bed.hour, bed.minute)} → ${clockText(context, wake.hour, wake.minute)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (r.auto) Text("🤖", modifier = Modifier.padding(end = 8.dp))
                if (r.quality > 0) Text(FACES[r.quality - 1], modifier = Modifier.padding(end = 10.dp))
                Text(formatSleep(r.durationMs), color = MaterialTheme.colorScheme.secondary)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((r.durationMs / 3_600_000f / GOAL_HOURS).coerceIn(0.03f, 1f))
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

@Composable
private fun YearView(records: List<SleepRecord>, year: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    PeriodNav(year.toString(), onPrev, onNext)
    val summary = summarize(records.inYear(year))
    if (summary == null) {
        EmptyNote()
        return
    }
    SummaryTiles(summary.avgMs, summary.avgBedMinute, summary.avgWakeMinute, summary.count, summary.avgQuality)
    val perMonth = records.avgHoursPerMonth(year)
    Panel(Modifier.fillMaxWidth()) {
        PanelTitle(stringResource(R.string.sleep_avg_per_month))
        BarChart(
            values = perMonth.map { it ?: 0f },
            labels = (1..12).map { Month.of(it).getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            modifier = Modifier.padding(top = 12.dp),
        )
        for (m in 1..12) {
            val s = summarize(records.inMonth(YearMonth.of(year, m))) ?: continue
            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(
                    Month.of(m).getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${formatSleep(s.avgMs)}  ·  ${stringResource(R.string.sleep_nights_n, s.count)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TotalView(records: List<SleepRecord>) {
    val context = LocalContext.current
    val summary = summarize(records)
    if (summary == null) {
        EmptyNote()
        return
    }
    SummaryTiles(summary.avgMs, summary.avgBedMinute, summary.avgWakeMinute, summary.count, summary.avgQuality)
    Panel(Modifier.fillMaxWidth()) {
        PanelTitle(stringResource(R.string.sleep_all_time))
        InfoRow(stringResource(R.string.sleep_total_hours), "${summary.totalMs / 3_600_000L} h")
        InfoRow(
            stringResource(R.string.sleep_longest),
            "${formatSleep(summary.longest.durationMs)} · ${dayLabel(context, summary.longest.wakeDate())}",
        )
        InfoRow(
            stringResource(R.string.sleep_shortest),
            "${formatSleep(summary.shortest.durationMs)} · ${dayLabel(context, summary.shortest.wakeDate())}",
        )
    }
    val years = records.filter { !it.open }.map { it.wakeDate().year }.distinct().sortedDescending()
    if (years.isNotEmpty()) {
        Panel(Modifier.fillMaxWidth()) {
            PanelTitle(stringResource(R.string.sleep_per_year))
            for (y in years) {
                val s = summarize(records.inYear(y)) ?: continue
                InfoRow("$y", "${formatSleep(s.avgMs)}  ·  ${stringResource(R.string.sleep_nights_n, s.count)}")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@Composable
private fun EmptyNote() {
    Text(
        stringResource(R.string.sleep_empty),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
    )
}

@Composable
private fun SummaryTiles(avgMs: Long, bedMinute: Int, wakeMinute: Int, count: Int, quality: Float?) {
    val context = LocalContext.current
    // Tarjeta principal: anillo con el promedio frente a la meta de 8 h.
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SleepRing(hours = avgMs / 3_600_000f, text = formatSleep(avgMs))
            Column(Modifier.weight(1f).padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HeroStat(stringResource(R.string.sleep_avg), formatSleep(avgMs))
                HeroStat(stringResource(R.string.sleep_nights), count.toString())
                if (quality != null) {
                    HeroStat(
                        stringResource(R.string.sleep_avg_quality),
                        "${FACES[(quality.coerceIn(1f, 5f) - 0.5f).toInt().coerceIn(0, 4)]} ${"%.1f".format(quality)}",
                    )
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(stringResource(R.string.sleep_avg_bed), minuteText(context, bedMinute), Modifier.weight(1f), icon = "🌙")
        StatTile(stringResource(R.string.sleep_avg_wake), minuteText(context, wakeMinute), Modifier.weight(1f), icon = "☀️")
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Anillo animado: cuánto del objetivo de 8 horas cubre el promedio. */
@Composable
private fun SleepRing(hours: Float, text: String, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState((hours / GOAL_HOURS).coerceIn(0f, 1f), tween(900), label = "ring")
    val track = MaterialTheme.colorScheme.outlineVariant
    val arc = MaterialTheme.colorScheme.secondary
    Box(modifier.size(124.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 14.dp.toPx()
            val box = Size(size.width - w, size.height - w)
            val topLeft = Offset(w / 2, w / 2)
            drawArc(track, -90f, 360f, false, topLeft, box, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(arc, -90f, 360f * progress, false, topLeft, box, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, icon: String? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/** Gráfico de barras simple, con una línea punteada en la meta de horas. */
@Composable
private fun BarChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val maxValue = maxOf(GOAL_HOURS, values.maxOrNull() ?: 0f) * 1.1f
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.secondary
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(130.dp)) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                for (v in values) {
                    val fraction by animateFloatAsState((v / maxValue).coerceIn(0f, 1f), tween(500), label = "bar")
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 1.dp)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (v >= GOAL_HOURS) barColor else barColor.copy(alpha = 0.55f)),
                    )
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - GOAL_HOURS / maxValue)
                drawLine(
                    color = goalColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            for (l in labels) {
                Box(Modifier.weight(1f)) {
                    Text(
                        l,
                        fontSize = 10.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = BuhitoColors.InkDim,
                        modifier = Modifier.align(Alignment.Center).wrapContentWidth(unbounded = true),
                    )
                }
            }
        }
    }
}

// ---------- Cargar / editar una noche ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepEditDialog(
    initial: SleepRecord?,
    onSave: (SleepRecord) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val is24 = DateFormat.is24HourFormat(context)
    val today = remember { LocalDate.now() }
    val initBed = initial?.let { TimeUtil.toLocal(it.sleepAt) }
    val initWake = initial?.let { TimeUtil.toLocal(it.wakeAt) }

    var wakeDay by remember { mutableStateOf(initial?.wakeDate() ?: today) }
    val bedState = rememberTimePickerState(initBed?.hour ?: 23, initBed?.minute ?: 0, is24)
    val wakeState = rememberTimePickerState(initWake?.hour ?: 7, initWake?.minute ?: 0, is24)
    var quality by remember { mutableIntStateOf(initial?.quality ?: 0) }

    // Si te dormiste a una hora "mayor" que la de despertar (23:30 → 07:00), fue el día anterior.
    val bedMinute = bedState.hour * 60 + bedState.minute
    val wakeMinute = wakeState.hour * 60 + wakeState.minute
    val bedDay = if (bedMinute > wakeMinute) wakeDay.minusDays(1) else wakeDay
    val sleepAt = TimeUtil.triggerFor(bedDay, bedState.hour, bedState.minute)
    val wakeAt = TimeUtil.triggerFor(wakeDay, wakeState.hour, wakeState.minute)
    val valid = bedMinute != wakeMinute && wakeAt <= System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_add_manual)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.sleep_wake_day), style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { wakeDay = wakeDay.minusDays(1) }) { Text("◀") }
                    Text(
                        dayLabel(context, wakeDay),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { if (wakeDay < today) wakeDay = wakeDay.plusDays(1) }) { Text("▶") }
                }
                Text(stringResource(R.string.sleep_fell_asleep), style = MaterialTheme.typography.labelMedium)
                TimeInput(bedState)
                Text(stringResource(R.string.sleep_woke_up), style = MaterialTheme.typography.labelMedium)
                TimeInput(wakeState)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FACES.forEachIndexed { i, face ->
                        Pill(face, selected = quality == i + 1, onClick = { quality = if (quality == i + 1) 0 else i + 1 })
                    }
                }
                if (valid) {
                    Text(
                        formatSleep(wakeAt - sleepAt),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(SleepRecord(id = initial?.id ?: 0L, sleepAt = sleepAt, wakeAt = wakeAt, quality = quality)) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

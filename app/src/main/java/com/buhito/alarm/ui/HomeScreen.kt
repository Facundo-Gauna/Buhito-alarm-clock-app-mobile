package com.buhito.alarm.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buhito.alarm.R
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.data.CategoryInfo
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.data.allCategories
import com.buhito.alarm.data.resolveCategory
import com.buhito.alarm.ui.mascots.CatMascot
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.mascots.OwlMood
import com.buhito.alarm.ui.theme.BuhitoColors
import com.buhito.alarm.util.AlarmReadiness
import com.buhito.alarm.util.Permissions
import com.buhito.alarm.util.daysSummary
import com.buhito.alarm.util.dayLabel
import com.buhito.alarm.util.formatDuration
import com.buhito.alarm.util.timeParts
import kotlinx.coroutines.delay
import java.time.LocalTime

private const val FILTER_ALL = "ALL"
private const val FILTER_ACTIVE = "ACTIVE"
private const val FILTER_PAUSED = "PAUSED"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    alarms: List<Alarm>,
    listState: LazyListState,
    readiness: AlarmReadiness,
    customs: List<CustomCategory>,
    onNew: () -> Unit,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    onDelete: (Alarm) -> Unit,
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Alarm?>(null) }
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val next = alarms.filter { it.enabled && it.triggerAt > now }.minByOrNull { it.triggerAt }
    val nextText = if (next == null) {
        stringResource(R.string.no_upcoming)
    } else {
        stringResource(R.string.next_alarm_in, formatDuration(context, next.triggerAt - now))
    }

    // Filtro: si la categoría elegida ya no tiene alarmas, se vuelve a "Todas".
    val usedCategories = alarms.map { it.category }.toSet()
    val activeFilter = when (filter) {
        FILTER_ALL, FILTER_ACTIVE, FILTER_PAUSED -> filter
        in usedCategories -> filter
        else -> FILTER_ALL
    }
    val visible = alarms.filter {
        when (activeFilter) {
            FILTER_ALL -> true
            FILTER_ACTIVE -> it.enabled
            FILTER_PAUSED -> !it.enabled
            else -> it.category == activeFilter
        }
    }

    // Personalidad del búho: saluda al abrir y dice una frase cuando lo tocás.
    val owlLines = stringArrayResource(R.array.owl_lines)
    val greeting = stringResource(
        when (remember { LocalTime.now().hour }) {
            in 5..11 -> R.string.greet_morning
            in 12..18 -> R.string.greet_afternoon
            in 19..22 -> R.string.greet_evening
            else -> R.string.greet_night
        },
    )
    var bubble by remember { mutableStateOf("") }
    var bubbleVisible by remember { mutableStateOf(false) }
    var bubbleToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(greeting) {
        bubble = greeting
        bubbleVisible = true
        bubbleToken++
    }
    LaunchedEffect(bubbleToken) {
        if (bubbleVisible) {
            delay(3800)
            bubbleVisible = false
        }
    }
    val owlMood = if (alarms.any { it.enabled }) OwlMood.Calm else OwlMood.Sleepy

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                HomeHeader(
                    nextText = nextText,
                    mood = owlMood,
                    bubble = bubble,
                    bubbleVisible = bubbleVisible,
                    onOwlTap = {
                        bubble = owlLines.random()
                        bubbleVisible = true
                        bubbleToken++
                    },
                )
            }

            if (!readiness.exactAlarms) item(key = "perm-exact") {
                PermissionCard(
                    title = stringResource(R.string.perm_exact_title),
                    body = stringResource(R.string.perm_exact_body),
                    onFix = { context.safeStart(Permissions.exactAlarmsSettingsIntent(context)) },
                )
            }
            if (!readiness.notifications) item(key = "perm-notif") {
                PermissionCard(
                    title = stringResource(R.string.perm_notif_title),
                    body = stringResource(R.string.perm_notif_body),
                    onFix = { context.safeStart(Permissions.notificationSettingsIntent(context)) },
                )
            }
            if (!readiness.fullScreen) item(key = "perm-fsi") {
                PermissionCard(
                    title = stringResource(R.string.perm_fsi_title),
                    body = stringResource(R.string.perm_fsi_body),
                    onFix = { context.safeStart(Permissions.fullScreenSettingsIntent(context)) },
                )
            }
            if (!readiness.battery) item(key = "perm-batt") {
                PermissionCard(
                    title = stringResource(R.string.perm_batt_title),
                    body = stringResource(R.string.perm_batt_body),
                    onFix = { context.safeStart(Permissions.batterySettingsIntent()) },
                )
            }

            if (alarms.isEmpty()) {
                item(key = "empty") { EmptyState() }
            } else {
                item(key = "filters") {
                    FilterRow(
                        selected = activeFilter,
                        categories = allCategories(context, customs).filter { it.key in usedCategories },
                        onSelect = { filter = it },
                    )
                }
                if (visible.isEmpty()) {
                    item(key = "no-matches") {
                        Text(
                            text = stringResource(R.string.no_matches),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                }
                items(visible, key = { it.id }) { alarm ->
                    SwipeableAlarmCard(
                        alarm = alarm,
                        customs = customs,
                        onClick = { onEdit(alarm) },
                        onToggle = { onToggle(alarm, it) },
                        onSwipeDelete = { pendingDelete = alarm },
                    )
                }
                item(key = "footer") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CatMascot(Modifier.width(240.dp))
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNew,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            shape = RoundedCornerShape(22.dp),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.new_alarm), style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        )
    }

    pendingDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(alarm)
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
private fun HomeHeader(
    nextText: String,
    mood: OwlMood,
    bubble: String,
    bubbleVisible: Boolean,
    onOwlTap: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = nextText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            OwlMascot(Modifier.size(104.dp), mood = mood, onClick = onOwlTap)
        }
        AnimatedVisibility(
            visible = bubbleVisible,
            modifier = Modifier.align(Alignment.End),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            SpeechBubble(bubble, Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun FilterRow(selected: String, categories: List<CategoryInfo>, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(stringResource(R.string.filter_all), selected == FILTER_ALL, { onSelect(FILTER_ALL) })
        Pill(stringResource(R.string.filter_active), selected == FILTER_ACTIVE, { onSelect(FILTER_ACTIVE) })
        Pill(stringResource(R.string.filter_paused), selected == FILTER_PAUSED, { onSelect(FILTER_PAUSED) })
        for (category in categories) {
            Pill(
                text = "${category.emoji} ${category.name}",
                selected = selected == category.key,
                onClick = { onSelect(category.key) },
                accent = Color(category.color),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CatMascot(Modifier.width(240.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 6.dp)
                .padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun PermissionCard(title: String, body: String, onFix: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            }
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) { Text(stringResource(R.string.perm_fix)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAlarmCard(
    alarm: Alarm,
    customs: List<CustomCategory>,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onSwipeDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onSwipeDelete()
            false // la tarjeta vuelve a su lugar; el borrado se confirma en un diálogo
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(BuhitoColors.Danger.copy(alpha = 0.85f))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
            }
        },
    ) {
        AlarmCard(alarm, customs, onClick, onToggle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmCard(alarm: Alarm, customs: List<CustomCategory>, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val parts = timeParts(context, alarm.hour, alarm.minute)
    val category = resolveCategory(context, alarm.category, customs)
    val accent = Color(category.color)
    val soundName = alarm.soundName.ifBlank { stringResource(R.string.default_alarm_tone) }
    val dim = if (alarm.enabled) 1f else 0.5f
    val whenText = if (alarm.repeatDays != 0) {
        daysSummary(context, alarm.repeatDays)
    } else {
        dayLabel(context, alarm.triggerAt)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .alpha(dim),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = parts.main,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (parts.suffix.isNotEmpty()) {
                        Text(
                            text = parts.suffix,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
                        )
                    }
                }
                val subtitle = buildString {
                    append(whenText)
                    if (alarm.label.isNotBlank()) append("  ·  ").append(alarm.label)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = accent.copy(alpha = 0.18f)) {
                        Text(
                            text = "${category.emoji} ${category.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            text = "♪ $soundName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun Context.safeStart(intent: Intent) {
    runCatching { startActivity(intent) }
}

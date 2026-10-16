package com.buhito.alarm.ui

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buhito.alarm.R
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.data.AudioStorage
import com.buhito.alarm.data.Category
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.data.allCategories
import com.buhito.alarm.data.resolveCategory
import com.buhito.alarm.data.Template
import com.buhito.alarm.engine.SoundUtil
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.theme.BuhitoColors
import com.buhito.alarm.util.Days
import com.buhito.alarm.util.TimeUtil
import com.buhito.alarm.util.dayLabel
import com.buhito.alarm.util.formatDuration
import com.buhito.alarm.util.formatSeconds
import com.buhito.alarm.util.nextRepeating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    initial: Alarm?,
    templates: List<Template>,
    customs: List<CustomCategory>,
    onAddCategory: (name: String, emoji: String, color: Long) -> Unit,
    onSave: (Alarm) -> Unit,
    onSaveTemplate: (Template) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
    onDelete: (Alarm) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preview = rememberSoundPreview()

    // Valores iniciales (si se edita) o por defecto (si es nueva: dentro de una hora).
    val start = remember {
        if (initial != null) {
            TimeUtil.toLocal(initial.triggerAt).withHour(initial.hour).withMinute(initial.minute)
        } else {
            TimeUtil.toLocal(System.currentTimeMillis() + 60 * 60_000L)
        }
    }
    val is24 = DateFormat.is24HourFormat(context)

    // El reloj se recrea cuando se aplica una plantilla (seedVersion), para que muestre su hora.
    var seedHour by rememberSaveable { mutableIntStateOf(start.hour) }
    var seedMinute by rememberSaveable { mutableIntStateOf(start.minute) }
    var seedVersion by rememberSaveable { mutableIntStateOf(0) }
    val timeState = key(seedVersion) { rememberTimePickerState(seedHour, seedMinute, is24) }

    var dateIso by rememberSaveable { mutableStateOf(start.toLocalDate().toString()) }
    var label by rememberSaveable { mutableStateOf(initial?.label ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: Category.GENERAL.name) }
    var repeatDays by rememberSaveable { mutableIntStateOf(initial?.repeatDays ?: 0) }
    var soundUri by rememberSaveable { mutableStateOf(initial?.soundUri ?: "") }
    var soundName by rememberSaveable { mutableStateOf(initial?.soundName ?: "") }
    var customSound by rememberSaveable { mutableStateOf(initial?.customSound ?: false) }
    var volume by rememberSaveable { mutableIntStateOf(initial?.volume ?: 100) }
    var loop by rememberSaveable { mutableStateOf(initial?.loop ?: true) }
    var durationSec by rememberSaveable { mutableIntStateOf(initial?.durationSec ?: 120) }
    var vibrate by rememberSaveable { mutableStateOf(initial?.vibrate ?: true) }
    var confirmStop by rememberSaveable { mutableStateOf(initial?.confirmStop ?: false) }
    var qrStop by rememberSaveable { mutableStateOf(initial?.qrStop ?: false) }
    var speakLabel by rememberSaveable { mutableStateOf(initial?.speakLabel ?: false) }
    var chainCount by rememberSaveable { mutableIntStateOf(initial?.chainCount ?: 0) }
    var chainGapMin by rememberSaveable { mutableIntStateOf(initial?.chainGapMin ?: 10) }
    var showNewCategory by rememberSaveable { mutableStateOf(false) }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTones by rememberSaveable { mutableStateOf(false) }
    var showRecorder by rememberSaveable { mutableStateOf(false) }
    var showDelete by rememberSaveable { mutableStateOf(false) }
    var showTemplateName by rememberSaveable { mutableStateOf(false) }
    var templateName by rememberSaveable { mutableStateOf("") }
    var templateToDelete by remember { mutableStateOf<Template?>(null) }

    val date = LocalDate.parse(dateIso)
    val hour = timeState.hour
    val minute = timeState.minute
    val trigger = if (repeatDays == 0) {
        TimeUtil.effectiveTrigger(date, hour, minute)
    } else {
        nextRepeating(hour, minute, repeatDays)
    }

    /** Si el audio actual se importó/grabó en esta sesión y todavía no se guardó, se descarta. */
    fun discardUnsavedSound() {
        if (customSound && soundUri != initial?.soundUri) AudioStorage.delete(soundUri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        qrStop = granted
        if (!granted) android.widget.Toast.makeText(context, context.getString(R.string.qr_camera_needed), android.widget.Toast.LENGTH_LONG).show()
    }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching { AudioStorage.import(context, uri) }.getOrNull()
            }
            if (imported == null) {
                Toast.makeText(context, R.string.audio_import_failed, Toast.LENGTH_SHORT).show()
            } else {
                preview.stop()
                discardUnsavedSound()
                soundUri = imported.path
                soundName = imported.displayName
                customSound = true
            }
        }
    }

    val applyTemplate: (Template) -> Unit = { t ->
        scope.launch {
            preview.stop()
            // La alarma se queda con su propia copia del audio de la plantilla.
            val ownPath = if (t.customSound) {
                withContext(Dispatchers.IO) { AudioStorage.duplicate(context, t.soundUri) }
            } else {
                t.soundUri
            }
            discardUnsavedSound()
            if (ownPath == null) {
                soundUri = ""
                soundName = ""
                customSound = false
            } else {
                soundUri = ownPath
                soundName = t.soundName
                customSound = t.customSound
            }
            label = t.label
            category = t.category
            repeatDays = t.repeatDays
            volume = t.volume
            loop = t.loop
            durationSec = t.durationSec
            vibrate = t.vibrate
            seedHour = t.hour
            seedMinute = t.minute
            seedVersion++
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Barra superior
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(if (initial == null) R.string.new_alarm else R.string.edit_alarm),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            OwlMascot(Modifier.size(52.dp), onClick = {})
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Plantillas (solo al crear una alarma nueva)
            if (initial == null && templates.isNotEmpty()) {
                Panel(Modifier.fillMaxWidth()) {
                    PanelTitle(stringResource(R.string.templates_title), icon = "✨")
                    Row(
                        Modifier
                            .padding(top = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (t in templates) {
                            Pill(
                                text = "${resolveCategory(context, t.category, customs).emoji} ${t.name}",
                                selected = false,
                                onClick = { applyTemplate(t) },
                                onLongClick = { templateToDelete = t },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.template_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            // Hora, repetición y fecha
            Panel(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(
                        R.string.rings_summary,
                        dayLabel(context, trigger),
                        formatDuration(context, trigger - System.currentTimeMillis()),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                TimePicker(
                    state = timeState,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp),
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = MaterialTheme.colorScheme.onPrimary,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        periodSelectorBorderColor = MaterialTheme.colorScheme.outline,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
                        periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.primary,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )

                PanelTitle(stringResource(R.string.repeat_title), icon = "🔁")
                Row(
                    Modifier
                        .padding(top = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pill(stringResource(R.string.repeat_once), repeatDays == 0, { repeatDays = 0 })
                    Pill(stringResource(R.string.weekdays), repeatDays == Days.WEEKDAYS, { repeatDays = Days.WEEKDAYS })
                    Pill(stringResource(R.string.every_day), repeatDays == Days.ALL, { repeatDays = Days.ALL })
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (i in 0..6) {
                        DayCircle(
                            letter = DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            selected = Days.has(repeatDays, i),
                            onClick = { repeatDays = repeatDays xor Days.bit(i) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (repeatDays == 0) {
                    Row(
                        Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val today = LocalDate.now()
                        Pill(stringResource(R.string.today), date == today, { dateIso = today.toString() })
                        Pill(stringResource(R.string.tomorrow), date == today.plusDays(1), { dateIso = today.plusDays(1).toString() })
                        val custom = date != today && date != today.plusDays(1)
                        Pill(
                            text = if (custom) dayLabel(context, date) else stringResource(R.string.pick_date),
                            selected = custom,
                            onClick = { showDatePicker = true },
                        )
                    }
                }
            }

            // Categoría y etiqueta
            Panel(Modifier.fillMaxWidth()) {
                PanelTitle(stringResource(R.string.category_title), icon = "🏷️")
                Row(
                    Modifier
                        .padding(top = 10.dp, bottom = 14.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (c in allCategories(context, customs)) {
                        Pill(
                            text = "${c.emoji} ${c.name}",
                            selected = category == c.key,
                            onClick = { category = c.key },
                            accent = Color(c.color),
                        )
                    }
                    Pill(
                        text = "＋ ${stringResource(R.string.new_category)}",
                        selected = false,
                        onClick = { showNewCategory = true },
                    )
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(60) },
                    label = { Text(stringResource(R.string.label_field)) },
                    placeholder = { Text(stringResource(R.string.label_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Sonido
            Panel(Modifier.fillMaxWidth()) {
                PanelTitle(stringResource(R.string.sound_title), icon = "🔔")
                Row(
                    Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PreviewButton(
                        playing = preview.playingKey == "current",
                        onClick = { preview.toggle("current", SoundUtil.resolve(soundUri, customSound), volume / 100f) },
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = soundName.ifBlank { stringResource(R.string.default_alarm_tone) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(if (customSound) R.string.sound_file else R.string.sound_system),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { preview.stop(); showTones = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.choose_tone), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    OutlinedButton(
                        onClick = { preview.stop(); pickAudio.launch(arrayOf("audio/*")) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.choose_file), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                OutlinedButton(
                    onClick = { preview.stop(); showRecorder = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                ) { Text("🎙️  ${stringResource(R.string.record_voice)}") }

                Row(
                    Modifier.padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.volume_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$volume%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { volume = ((it / 10f).roundToInt() * 10).coerceIn(10, 100) },
                    valueRange = 10f..100f,
                    steps = 8,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.loop_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.loop_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = loop, onCheckedChange = { loop = it })
                }
            }

            // Duración y vibración
            Panel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PanelTitle(stringResource(R.string.duration_title), icon = "⏳")
                        Text(
                            text = stringResource(R.string.duration_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = formatSeconds(context, durationSec),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Slider(
                    value = durationSec.toFloat(),
                    onValueChange = { durationSec = ((it / 30f).roundToInt() * 30).coerceIn(30, 600) },
                    valueRange = 30f..600f,
                    steps = 18,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.vibration),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }
            }

            // Más opciones: confirmar para detener, leer la etiqueta y secuencia de timbres
            Panel(Modifier.fillMaxWidth()) {
                PanelTitle(stringResource(R.string.more_options), icon = "⚙️")
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.confirm_stop_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.confirm_stop_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = confirmStop, onCheckedChange = { confirmStop = it })
                }
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("📷 " + stringResource(R.string.qr_stop_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.qr_stop_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = qrStop, onCheckedChange = { on ->
                        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.CAMERA,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (!on || hasCamera) qrStop = on else cameraLauncher.launch(android.Manifest.permission.CAMERA)
                    })
                }
                Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.speak_label_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.speak_label_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = speakLabel, onCheckedChange = { speakLabel = it })
                }
                Text(
                    stringResource(R.string.chain_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    stringResource(R.string.chain_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier
                        .padding(top = 10.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (n in intArrayOf(0, 1, 2, 3, 5)) {
                        Pill(
                            text = if (n == 0) stringResource(R.string.chain_off) else "+$n",
                            selected = chainCount == n,
                            onClick = { chainCount = n },
                        )
                    }
                }
                if (chainCount > 0) {
                    Text(
                        stringResource(R.string.chain_every),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Row(
                        Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (m in intArrayOf(5, 10, 15)) {
                            Pill(
                                text = stringResource(R.string.minutes_short, m),
                                selected = chainGapMin == m,
                                onClick = { chainGapMin = m },
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    templateName = label.trim()
                    showTemplateName = true
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text(stringResource(R.string.save_template)) }

            if (initial != null) {
                TextButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            }
        }

        // Barra inferior con Guardar
        Button(
            onClick = {
                preview.stop()
                onSave(
                    Alarm(
                        id = initial?.id ?: 0L,
                        label = label.trim(),
                        hour = hour,
                        minute = minute,
                        triggerAt = trigger,
                        repeatDays = repeatDays,
                        category = category,
                        enabled = true,
                        soundUri = soundUri,
                        soundName = soundName,
                        customSound = customSound,
                        volume = volume,
                        loop = loop,
                        durationSec = durationSec,
                        vibrate = vibrate,
                        confirmStop = confirmStop,
                        qrStop = qrStop,
                        speakLabel = speakLabel,
                        chainCount = chainCount,
                        chainGapMin = chainGapMin,
                    ),
                )
            },
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(56.dp),
        ) { Text(stringResource(R.string.save), style = MaterialTheme.typography.labelLarge) }
    }

    if (showNewCategory) {
        NewCategoryDialog(
            onCreate = { name, emoji, color ->
                onAddCategory(name, emoji, color)
                showNewCategory = false
            },
            onDismiss = { showNewCategory = false },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isBefore(LocalDate.now())
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        dateIso = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }

    if (showTones) {
        SystemTonesSheet(
            selectedUri = if (customSound) "\u0000" else soundUri,
            preview = preview,
            onSelect = { tone ->
                discardUnsavedSound()
                soundUri = tone.uri
                soundName = if (tone.uri.isEmpty()) "" else tone.name
                customSound = false
            },
            onDismiss = { showTones = false },
        )
    }

    if (showRecorder) {
        RecorderSheet(
            preview = preview,
            onUse = { path, name ->
                discardUnsavedSound()
                soundUri = path
                soundName = name
                customSound = true
                showRecorder = false
            },
            onDismiss = { showRecorder = false },
        )
    }

    if (showTemplateName) {
        AlertDialog(
            onDismissRequest = { showTemplateName = false },
            title = { Text(stringResource(R.string.save_template)) },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it.take(30) },
                    label = { Text(stringResource(R.string.template_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = templateName.trim().ifEmpty { resolveCategory(context, category, customs).name }
                    onSaveTemplate(
                        Template(
                            name = name,
                            label = label.trim(),
                            hour = hour,
                            minute = minute,
                            repeatDays = repeatDays,
                            category = category,
                            soundUri = soundUri,
                            soundName = soundName,
                            customSound = customSound,
                            volume = volume,
                            loop = loop,
                            durationSec = durationSec,
                            vibrate = vibrate,
                        ),
                    )
                    Toast.makeText(context, R.string.template_saved, Toast.LENGTH_SHORT).show()
                    showTemplateName = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateName = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    templateToDelete?.let { t ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text(stringResource(R.string.delete_template_title)) },
            text = { Text(t.name) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTemplate(t)
                    templateToDelete = null
                }) { Text(stringResource(R.string.delete), color = BuhitoColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete(initial)
                }) { Text(stringResource(R.string.delete), color = BuhitoColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** Círculo con la inicial de un día de la semana, seleccionable. */
@Composable
private fun DayCircle(letter: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (selected) colors.primary.copy(alpha = 0.25f) else colors.surfaceVariant)
            .border(1.dp, if (selected) colors.primary else colors.outlineVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.primary else colors.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

package com.buhito.alarm.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buhito.alarm.R
import com.buhito.alarm.data.Alarm
import com.buhito.alarm.engine.AlarmRepository
import com.buhito.alarm.engine.Backup
import com.buhito.alarm.engine.SleepRepository
import com.buhito.alarm.engine.StopwatchRepository
import com.buhito.alarm.ui.mascots.NightBackground
import com.buhito.alarm.util.Permissions
import com.buhito.alarm.util.Prefs
import com.buhito.alarm.util.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Navegación mínima por estado: null = lista; 0 = alarma nueva; otro valor = id de la alarma a editar. */
@Composable
fun BuhitoRoot(repository: AlarmRepository, stopwatchRepository: StopwatchRepository, backup: Backup, sleepRepository: SleepRepository, startTab: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alarms by repository.alarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val templates by repository.templates.collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberLazyListState()
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(startTab) } // 0 alarmas, 1 cronómetros, 2 sueño, 3 historial, 4 ajustes
    val customs by repository.customCategories.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by repository.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val sleepRecords by sleepRepository.records.collectAsStateWithLifecycle(initialValue = emptyList())
    val addCategory: (String, String, Long) -> Unit = { name, emoji, color ->
        scope.launch { repository.addCategory(name, emoji, color) }
    }
    val stopwatches by stopwatchRepository.stopwatches.collectAsStateWithLifecycle(initialValue = emptyList())
    val showBar = editing == null
    var showTour by rememberSaveable { mutableStateOf(!Prefs.onboardingDone(context)) }

    // Mensaje de festejo (título, texto) que se muestra un momento después de guardar.
    var celebration by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Estado de permisos: se vuelve a leer cada vez que el usuario vuelve desde Ajustes.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val readiness = remember(refresh) { Permissions.readiness(context) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Permissions.notificationsGranted(context)) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = editing != null && celebration == null) { editing = null }

    NightBackground {
        Column(Modifier.fillMaxSize()) {
            // La barra de abajo ya se encarga del borde inferior: el contenido no lo repite.
            Box(
                Modifier
                    .weight(1f)
                    .then(if (showBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
            ) {
                AnimatedContent(
                    targetState = editing,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(140))
                    },
                    label = "screens",
                ) { target ->
                    if (target == null && tab == 4) {
                        SettingsScreen(
                            customs = customs,
                            onReplayTour = { showTour = true },
                            backup = backup,
                            onAddCategory = addCategory,
                            onDeleteCategory = { c -> scope.launch { repository.deleteCategory(c) } },
                            onCreateFromCalendar = { events, minutes, category ->
                                scope.launch {
                                    val created = repository.createFromCalendar(events, minutes, category)
                                    Toast.makeText(context, context.getString(R.string.cal_created, created), Toast.LENGTH_LONG).show()
                                }
                            },
                        )
                    } else if (target == null && tab == 2) {
                        SleepScreen(
                            records = sleepRecords,
                            onDetect = { sleepRepository.detectNew().size },
                            onSave = { r -> scope.launch { sleepRepository.save(r) } },
                            onDelete = { r -> scope.launch { sleepRepository.delete(r) } },
                        )
                    } else if (target == null && tab == 3) {
                        HistoryScreen(
                            entries = history,
                            customs = customs,
                            onClear = { scope.launch { repository.clearHistory() } },
                        )
                    } else if (target == null && tab == 1) {
                        StopwatchScreen(
                            stopwatches = stopwatches,
                            onCreate = { name -> scope.launch { stopwatchRepository.create(name) } },
                            onToggle = { sw -> scope.launch { stopwatchRepository.toggle(sw) } },
                            onLap = { sw -> scope.launch { stopwatchRepository.lap(sw) } },
                            onReset = { sw -> scope.launch { stopwatchRepository.reset(sw) } },
                            onDelete = { sw -> scope.launch { stopwatchRepository.delete(sw) } },
                        )
                    } else if (target == null) {
                        HomeScreen(
                            alarms = alarms,
                            listState = listState,
                            readiness = readiness,
                            customs = customs,
                            onNew = { editing = 0L },
                            onEdit = { editing = it.id },
                            onToggle = { alarm, enabled -> scope.launch { repository.setEnabled(alarm, enabled) } },
                            onDelete = { alarm -> scope.launch { repository.delete(alarm) } },
                        )
                    } else {
                        // Para editar, primero se carga la alarma completa (así sobrevive a rotar la pantalla).
                        val loaded by produceState<LoadState>(LoadState.Loading, target) {
                            value = if (target == 0L) LoadState.Ready(null)
                            else LoadState.Ready(repository.get(target))
                        }
                        (loaded as? LoadState.Ready)?.let { ready ->
                            EditorScreen(
                                initial = ready.alarm,
                                templates = templates,
                                customs = customs,
                                onAddCategory = addCategory,
                                onSave = { alarm ->
                                    scope.launch {
                                        val saved = repository.save(alarm)
                                        val left = formatDuration(context, saved.triggerAt - System.currentTimeMillis())
                                        celebration = context.getString(R.string.saved_title) to
                                            context.getString(R.string.saved_body, left)
                                        delay(1500)
                                        celebration = null
                                        editing = null
                                    }
                                },
                                onSaveTemplate = { template -> scope.launch { repository.saveTemplate(template) } },
                                onDeleteTemplate = { template -> scope.launch { repository.deleteTemplate(template) } },
                                onDelete = { alarm ->
                                    scope.launch {
                                        repository.delete(alarm)
                                        editing = null
                                    }
                                },
                                onClose = { editing = null },
                            )
                        }
                    }
                }

            }
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    val colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    )
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Text("⏰") },
                        label = { Text(stringResource(R.string.tab_alarms)) },
                        colors = colors,
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Text("⏱️") },
                        label = { Text(stringResource(R.string.tab_stopwatch)) },
                        colors = colors,
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Text("🌙") },
                        label = { Text(stringResource(R.string.tab_sleep), maxLines = 1) },
                        colors = colors,
                    )
                    NavigationBarItem(
                        selected = tab == 3,
                        onClick = { tab = 3 },
                        icon = { Text("📜") },
                        label = { Text(stringResource(R.string.tab_history), maxLines = 1) },
                        colors = colors,
                    )
                    NavigationBarItem(
                        selected = tab == 4,
                        onClick = { tab = 4 },
                        icon = { Text("⚙️") },
                        label = { Text(stringResource(R.string.tab_settings), maxLines = 1) },
                        colors = colors,
                    )
                }
            }
        }

        celebration?.let { (title, body) -> SavedCelebration(title, body) }

        if (showTour) {
            OnboardingScreen(onFinish = {
                Prefs.setOnboardingDone(context, true)
                showTour = false
            })
        }
    }
}

private sealed interface LoadState {
    data object Loading : LoadState
    data class Ready(val alarm: Alarm?) : LoadState
}

package com.buhito.alarm.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import com.buhito.alarm.util.Prefs
import com.buhito.alarm.util.qrBitmap
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buhito.alarm.R
import com.buhito.alarm.data.CustomCategory
import com.buhito.alarm.engine.Backup
import com.buhito.alarm.engine.CalendarEvent
import kotlinx.coroutines.launch

/** Pestaña Ajustes: categorías propias y copia de seguridad. */
@Composable
fun SettingsScreen(
    customs: List<CustomCategory>,
    onReplayTour: () -> Unit,
    backup: Backup,
    onAddCategory: (name: String, emoji: String, color: Long) -> Unit,
    onDeleteCategory: (CustomCategory) -> Unit,
    onCreateFromCalendar: (events: List<CalendarEvent>, minutesBefore: Int, category: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showNew by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<CustomCategory?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var showNewQr by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val message = runCatching { backup.export(uri) }.fold(
                onSuccess = { context.getString(R.string.backup_done, it) },
                onFailure = { context.getString(R.string.backup_failed) },
            )
            busy = false
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    var strict by remember { mutableStateOf(Prefs.qrStrict(context)) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        strict = granted
        Prefs.setQrStrict(context, granted)
        if (!granted) Toast.makeText(context, context.getString(R.string.qr_camera_needed), Toast.LENGTH_LONG).show()
    }
    var qrText by remember { mutableStateOf(Prefs.qrCode(context)) }
    val qrImage = remember(qrText) { qrBitmap(qrText) }
    val saveQrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png"),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { qrImage.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }.isSuccess
            Toast.makeText(context, context.getString(if (ok) R.string.qr_saved else R.string.backup_failed), Toast.LENGTH_LONG).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestore = uri }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Panel(Modifier.fillMaxWidth()) {
            PanelTitle(stringResource(R.string.lang_title), icon = "🌍")
            Text(
                stringResource(R.string.lang_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            LanguagePicker()
            OutlinedButton(onClick = onReplayTour, modifier = Modifier.padding(top = 14.dp)) {
                Text("🦉 " + stringResource(R.string.ob_replay))
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            PanelTitle(stringResource(R.string.qr_settings_title), icon = "📷")
            Text(
                stringResource(R.string.qr_settings_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            androidx.compose.foundation.Image(
                bitmap = qrImage.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .size(180.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp)),
            )
            SettingRow(
                title = stringResource(R.string.qr_strict_title),
                subtitle = stringResource(R.string.qr_strict_hint),
                icon = "🔒",
                modifier = Modifier.padding(top = 10.dp),
                trailing = {
                    androidx.compose.material3.Switch(checked = strict, onCheckedChange = { on ->
                        if (!on || com.buhito.alarm.util.cameraGranted(context)) {
                            strict = on
                            Prefs.setQrStrict(context, on)
                        } else cameraLauncher.launch(android.Manifest.permission.CAMERA)
                    })
                },
            )
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { saveQrLauncher.launch("buhito-qr.png") }) { Text(stringResource(R.string.qr_save)) }
                OutlinedButton(onClick = { showNewQr = true }) { Text(stringResource(R.string.qr_new)) }
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            PanelTitle(stringResource(R.string.my_categories), icon = "🏷️")
            if (customs.isEmpty()) {
                Text(
                    stringResource(R.string.my_categories_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            for (c in customs) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color(c.color).copy(alpha = 0.18f),
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        Text(
                            "${c.emoji} ${c.name}",
                            color = Color(c.color),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { toDelete = c }) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            OutlinedButton(onClick = { showNew = true }, modifier = Modifier.padding(top = 14.dp)) {
                Text("＋ ${stringResource(R.string.new_category)}")
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            PanelTitle(stringResource(R.string.cal_title), icon = "📅")
            Text(
                stringResource(R.string.cal_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(onClick = { showCalendar = true }, modifier = Modifier.padding(top = 14.dp)) {
                Text("📅 " + stringResource(R.string.cal_button))
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            PanelTitle(stringResource(R.string.backup_title), icon = "💾")
            Text(
                stringResource(R.string.backup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(enabled = !busy, onClick = { exportLauncher.launch("buhito-backup.zip") }) {
                    Text(stringResource(R.string.backup_export))
                }
                OutlinedButton(enabled = !busy, onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }) {
                    Text(stringResource(R.string.backup_import))
                }
            }
        }
    }

    if (showCalendar) {
        CalendarImportDialog(
            customs = customs,
            onCreate = { events, minutes, category ->
                onCreateFromCalendar(events, minutes, category)
                showCalendar = false
            },
            onDismiss = { showCalendar = false },
        )
    }

    if (showNew) {
        NewCategoryDialog(
            onCreate = { name, emoji, color ->
                onAddCategory(name, emoji, color)
                showNew = false
            },
            onDismiss = { showNew = false },
        )
    }

    if (showNewQr) {
        AlertDialog(
            onDismissRequest = { showNewQr = false },
            title = { Text(stringResource(R.string.qr_new_title)) },
            text = { Text(stringResource(R.string.qr_new_body)) },
            confirmButton = {
                TextButton(onClick = { qrText = Prefs.newQrCode(context); showNewQr = false }) {
                    Text(stringResource(R.string.qr_new))
                }
            },
            dismissButton = { TextButton(onClick = { showNewQr = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_body, "${c.emoji} ${c.name}")) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCategory(c)
                    toDelete = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_title)) },
            text = { Text(stringResource(R.string.restore_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    scope.launch {
                        busy = true
                        val message = runCatching { backup.restore(uri) }.fold(
                            onSuccess = { context.getString(R.string.restore_done, it) },
                            onFailure = { context.getString(R.string.restore_failed) },
                        )
                        busy = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.backup_import), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

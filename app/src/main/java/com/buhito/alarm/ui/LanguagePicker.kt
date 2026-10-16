package com.buhito.alarm.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.buhito.alarm.util.Locales
import com.buhito.alarm.util.Prefs
import java.util.Locale

/** Lista de idiomas como píldoras. Al elegir uno se guarda y la pantalla se recrea ya traducida. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val current = Prefs.language(context).ifBlank { Locale.getDefault().language }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (lang in Locales.all) {
            Pill(
                text = lang.name,
                selected = current == lang.tag,
                onClick = {
                    if (current != lang.tag) {
                        Prefs.setLanguage(context, lang.tag)
                        (context as? Activity)?.recreate()
                    }
                },
            )
        }
    }
}

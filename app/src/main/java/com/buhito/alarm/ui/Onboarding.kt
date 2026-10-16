package com.buhito.alarm.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.buhito.alarm.R
import com.buhito.alarm.ui.mascots.NightBackground
import com.buhito.alarm.ui.mascots.OwlMascot
import com.buhito.alarm.ui.mascots.OwlMood

private class TourStep(val textRes: Int, val mood: OwlMood, val icon: String)

private val steps = listOf(
    TourStep(R.string.ob_1, OwlMood.Happy, "🦉"),
    TourStep(R.string.ob_2, OwlMood.Calm, "⏰"),
    TourStep(R.string.ob_3, OwlMood.Listening, "🎙️"),
    TourStep(R.string.ob_4, OwlMood.Ringing, "👆"),
    TourStep(R.string.ob_5, OwlMood.Calm, "🔔"),
    TourStep(R.string.ob_6, OwlMood.Sleepy, "🌙"),
    TourStep(R.string.ob_7, OwlMood.Happy, "🚀"),
)

/** Recorrido de bienvenida: el búho explica la app paso a paso (la primera vez y desde Ajustes). */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val last = step == steps.lastIndex
    androidx.activity.compose.BackHandler(onBack = onFinish)
    NightBackground(Modifier.pointerInput(Unit) {}) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!last) TextButton(onClick = onFinish) { Text(stringResource(R.string.ob_skip)) }
                else Spacer(Modifier.height(48.dp))
            }

            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                OwlMascot(Modifier.size(170.dp), mood = steps[step].mood, onClick = {})
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { it / 6 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 6 })
                    },
                    label = "tour",
                ) { s ->
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 6.dp,
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(steps[s].icon, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(steps[s].textRes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            if (s == 0) {
                                Spacer(Modifier.height(16.dp))
                                LanguagePicker()
                            }
                        }
                    }
                }
            }

            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in steps.indices) {
                    Box(
                        Modifier
                            .size(if (i == step) 22.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == step) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.outline,
                            ),
                    )
                }
            }
            Button(
                onClick = { if (last) onFinish() else step++ },
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(
                    stringResource(if (last) R.string.ob_start else R.string.ob_next),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

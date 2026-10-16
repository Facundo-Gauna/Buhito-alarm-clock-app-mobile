<div align="center">

# Buhito 🦉

**An alarm clock with personality — your own sounds, a night-watch owl and a sleepy cat.**

100 % local · no account · no ads · no tracking

[![Build APK](https://github.com/OWNER/REPO/actions/workflows/build.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/build.yml)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![License: MIT](https://img.shields.io/badge/license-MIT-blue)

[Leer en español](README.es.md)

</div>
## Features

- ⏰ **Alarms that ring on the lock screen** — exact time, full-screen alert, vibration, snooze 5/10/15 min.
- 🎙️ **Your own sound** — system tones, any audio on your phone, or record your own voice.
- 🔁 **Repeat by day**, volume per alarm, sound loop or one-shot, **sequences** of extra rings, templates.
- 🏷️ **Categories** (built-in and your own, with emoji and colour) and filters.
- 🗣️ **Reads the label aloud** (text-to-speech).
- 🛑 **Ways to stop an alarm**: normal button, **hold to stop** (2 s) or **scan your Buhito QR code** so you have to get out of bed. An optional **strict mode** (Settings) removes snooze and every other way out for QR alarms.
- 🌙 **Sleep tracking** — detected automatically from when your screen stayed off at night (Usage access), adjustable, with notifications; monthly, yearly and total averages.
- ⏱️ **Stopwatches** with a curious cat.
- 📅 **Import calendar events** as alarms (any range, up to a year).
- 📜 **History** of every ring and 💾 **backup / restore** to a single `.zip`.
- 🦉 **A welcome tour** with the owl, animated mascots and a starry night theme.
- 🌍 **11 languages**: English, Español, Português, Français, Deutsch, Italiano, Русский, 中文, 日本語, हिन्दी, العربية (with right-to-left support).

## Install

1. Go to the [**Releases**](../../releases) page and download `buhito-x.y.z.apk`.
2. Open it on your phone. Android will ask you to allow installing from that source.
3. For alarms to ring over the lock screen, allow **notifications**, **alarms & reminders**, **full-screen alerts** and set battery to **unrestricted**. The app shows yellow cards on the home screen when something is missing.

Requires Android 8.0 (API 26) or newer. Automatic sleep detection needs Android 9+.

## Build from source

You need [Android Studio](https://developer.android.com/studio) (Ladybug or newer) with JDK 17.

```bash
git clone https://github.com/Facundo-Gauna/Buhito-alarm-clock-app-mobile.git
cd REPO
```

1. Open the folder in Android Studio and wait for the Gradle sync.
2. Plug in a phone with USB debugging and press **Run**.

Command line (needs Gradle 8.9 installed, or generate the wrapper once with `gradle wrapper --gradle-version 8.9`):

```bash
gradle assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
```

### Automatic APK builds

`.github/workflows/build.yml` builds the debug APK on every push and pull request (download it from the run's **Artifacts**). Pushing a tag such as `v1.0.0` also creates a GitHub Release with the APK attached:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Privacy

Everything stays on your phone. Buhito has no server, no account and no analytics. Permissions are used only for what they say:

| Permission | Why |
| --- | --- |
| Notifications, full-screen intent, exact alarms | Ring on time, over the lock screen |
| Microphone | Record your voice as an alarm sound (only when you tap record) |
| Camera | Scan your QR code to stop an alarm (only when you enable QR stop) |
| Calendar (read) | Import events as alarms (only when you tap the button) |
| Usage access | Know when the screen was off to estimate your sleep (optional) |

## How it works

```
AlarmScheduler ──setAlarmClock──▶ Android
      ▼
AlarmReceiver ──▶ AlarmService (foreground: audio + vibration + timeout)
                       │ full-screen notification
                       ▼
                  AlarmActivity (show over lock screen)
```

- `data/` – Room database (v7, with real migrations).
- `engine/` – scheduling, service, receivers, notifications, sleep detection, backup.
- `ui/` – Jetpack Compose screens; `ui/mascots/` – owl, cat and sky drawn on a Canvas (no image assets).
- Plain manual dependency wiring in `BuhitoApp`: no Hilt, no heavy frameworks.

Development notes and test checklists (Spanish): [docs/DEVELOPMENT.es.md](docs/DEVELOPMENT.es.md).

## Roadmap

- Wake-up mini-games (math, memory, shake) as extra ways to stop an alarm.
- An owl and a cat with more personality and presence across the whole app.
- Notifications for running stopwatches.

## Contributing

Issues and pull requests are welcome. Translations live in `app/src/main/res/values-*/strings.xml`; to add a language, copy `values/strings.xml`, translate it and add the language to `util/Locales.kt` and `res/xml/locales_config.xml`.

## Credits and licenses

- Code: [MIT](LICENSE).
- Font **Outfit** — SIL Open Font License 1.1 ([licenses/Outfit-OFL.txt](licenses/Outfit-OFL.txt)).
- [ZXing](https://github.com/zxing/zxing) (Apache 2.0) for QR generation and scanning; [CameraX](https://developer.android.com/training/camerax), [Jetpack Compose](https://developer.android.com/jetpack/compose) and [Room](https://developer.android.com/training/data-storage/room) (Apache 2.0).

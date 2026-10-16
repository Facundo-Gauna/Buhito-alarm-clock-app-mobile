# Changelog

## 1.0.0 — first public release
- Stop alarms by scanning a personal **QR code** (generated in Settings; falls back to hold-to-stop without a camera).
- **Strict QR mode** (Settings): alarms that use QR can only be switched off by scanning it (no snooze, no other button, notification only opens the alarm). Without camera permission it is not enforced, so an alarm can never become impossible to stop.
- Sleep screen: animated ring for the average vs the 8 h goal, nicer tiles and per-night bars.
- **11 languages** with a selector in Settings and in the welcome tour (incl. right-to-left Arabic).
- **Welcome tour** with the owl on first launch (replayable from Settings).
- New look: Outfit typeface, readable light text on dark panels, icons on panel titles.
- Database v7 (`qrStop`) with a migration that keeps all existing data.

## 0.x — development phases
- Alarms, lock-screen ringing, snooze, own audio and voice recording, categories, templates, stopwatches with the cat, history, sequences, text-to-speech, backup/restore, calendar import and automatic sleep tracking.

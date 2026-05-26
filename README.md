# JustSimple Reminder

A clean, reliable alarm-style reminder app for Android. Set one-time or recurring reminders and get notified exactly when you need to be — even with the app closed.

---

## Features

- **Alarm-style delivery** — full-screen alert + sound when a reminder fires, even from a locked screen
- **Recurring reminders** — once, daily, weekly, or monthly
- **Snooze** — 5 min, 10 min, 15 min, 1 h, 3 h, 1 day, or custom time
- **12 h / 24 h time format** — configurable in Settings
- **Free & Premium tiers** — free plan supports up to 2 active reminders; Premium is unlimited
- **No ads for Premium users** — banner ad shown only on the free tier
- **Polish & English** — full localisation for both languages
- **Alarm reliability diagnostics** — built-in screen showing permission status, device info, and OEM-specific setup steps (Xiaomi / Samsung / Huawei / OPPO / Vivo / Realme / OnePlus)

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + StateFlow |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Alarms | AlarmManager (`setAlarmClock`) |
| Billing | RevenueCat |
| Ads | AdMob (banner) |
| Language | AppCompat per-app locale |

- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)

---

## Project Setup

### 1. Clone the repository

```bash
git clone https://github.com/your-username/justsimple-reminder.git
cd justsimple-reminder
```

### 2. Add secret keys

Create `local.properties` in the project root (already git-ignored) and add your RevenueCat API key:

```
revenuecat.api.key=your_revenuecat_public_sdk_key
```

### 3. Add real AdMob IDs

Replace the placeholder IDs in `app/src/main/res/values/strings.xml`:

```xml
<string name="admob_app_id" translatable="false">ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX</string>
<string name="admob_banner_unit_id" translatable="false">ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX</string>
```

### 4. Build

Open in Android Studio and run, or build from the command line:

```bash
./gradlew assembleDebug
```

---

## Architecture Overview

```
ui/
  reminders/       # Reminder list screen
  addedit/         # Add / edit reminder screen
  alarm/           # Full-screen alarm activity
  settings/        # Settings screen
  diagnostics/     # Diagnostics + OEM reliability screens
  navigation/      # NavGraph

alarms/
  receivers/       # AlarmReceiver, AlarmActionReceiver, BootReceiver, TimeChangeReceiver
  service/         # AlarmForegroundService

domain/
  scheduler/       # AlarmScheduler — wraps AlarmManager.setAlarmClock()
  recurrence/      # RecurrenceType + next-trigger calculation
  entitlement/     # PremiumManager

data/
  db/              # Room database + ReminderEntity + ReminderDao
  repository/      # ReminderRepository
  preferences/     # UserPreferences (DataStore)

ads/               # AdBannerView composable
billing/           # RevenueCatManager
reliability/       # OemReliabilityGuide — deep-links to OEM battery / autostart settings
diagnostics/       # DeviceInfoProvider
```

### Alarm delivery flow

```
AlarmManager fires
  → AlarmReceiver.onReceive()
    → startForegroundService(AlarmForegroundService)
      → startForeground() + playAlarmSound()       ← within 5 s ANR window
      → startActivity(AlarmActivity)               ← immediate, before DB fetch
      → async: fetch reminder → update notification with title + Dismiss / Snooze buttons
```

---

## Permissions

| Permission | Reason |
|---|---|
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Fire reminders at the exact scheduled time |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep alarm service alive while delivering |
| `USE_FULL_SCREEN_INTENT` | Show alarm screen over the lock screen |
| `WAKE_LOCK` | Wake the screen when an alarm fires |
| `RECEIVE_BOOT_COMPLETED` | Reschedule alarms after device reboot |
| `POST_NOTIFICATIONS` | Show alarm notification (Android 13+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Request battery unrestricted mode |
| `VIBRATE` | Vibrate on alarm (device-dependent) |
| `INTERNET` / `ACCESS_NETWORK_STATE` | RevenueCat + AdMob |

---

## Releasing

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. Build a signed release APK / AAB:
   ```bash
   ./gradlew bundleRelease
   ```
3. Upload to Play Console
4. Replace AdMob test IDs with real ones before going live
5. Make sure RevenueCat entitlement ID matches `PremiumManager.ENTITLEMENT_ID` (`"premium"`)

---

## Privacy Policy

Hosted at [https://justsimplereminder-legal.netlify.app/](https://justsimplereminder-legal.netlify.app/)

---

## License

Private — all rights reserved.

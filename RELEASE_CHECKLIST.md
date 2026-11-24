# DAYFLOW — Release Checklist & OEM Optimization Guide

This document outlines the mandatory release verification gates, system recovery checklist, and OEM-specific background execution matrix for **DAYFLOW**, an offline-first personal productivity application for Android (API 26+).

---

## 1. Pre-Release Compliance Verification

Before any production build is approved for distribution, verify every requirement in the table below:

| Requirement | Target | Verification Command / Evidence | Status |
| :--- | :--- | :--- | :--- |
| **No Network Permission** | Zero `android.permission.INTERNET` | `Select-String "INTERNET" app\src\main\AndroidManifest.xml` returns 0 matches | PASSED |
| **No Foreground Services** | Zero `FOREGROUND_SERVICE` | `Select-String "FOREGROUND_SERVICE" app\src\main\AndroidManifest.xml` returns 0 matches | PASSED |
| **No Repeating Alarms** | One-shot alarms only | `Select-String "setRepeating" app\src\main` returns 0 matches | PASSED |
| **Safe Room Migrations** | Zero destructive migrations | `Select-String "fallbackToDestructiveMigration" app\src\main` returns 0 matches | PASSED |
| **No Cloud / Firebase** | Pure offline operation | `Select-String "firebase" app\build.gradle.kts` returns 0 matches | PASSED |
| **No Mock / Fake in Main** | Production code clean | `\b(TODO\|mock\|fake)\b` regex in `app/src/main` returns 0 matches | PASSED |
| **Disallowed Cloud Auto-Backup** | Prevent Google Drive sync | `android:allowBackup="false"` in `AndroidManifest.xml` | PASSED |
| **R8 Minification & Shrinking** | Code minified, resources shrunk | `isMinifyEnabled = true`, `isShrinkResources = true` in `build.gradle.kts` | PASSED |
| **Adaptive Launcher Icon** | Mint `#A5E6D6` on dark background | `res/mipmap-anydpi-v26/ic_launcher.xml` configured | PASSED |
| **Unit & Integration Tests** | 100% test suite passing | `./gradlew testDebugUnitTest` passing (254 tests) | PASSED |
| **Android Lint** | 0 errors | `./gradlew lintDebug` passing | PASSED |

---

## 2. Release Signing Configuration

Release builds are signed using `signingConfigs.release` in `app/build.gradle.kts`.

### Production Keystore Setup
Provide credentials via `keystore.properties` in the project root (automatically excluded in `.gitignore`) or environment variables:

```properties
# keystore.properties (Root directory)
storeFile=/path/to/dayflow-release.jks
storePassword=your_keystore_password
keyAlias=dayflow_release_key
keyPassword=your_key_password
```

Or via environment variables:
- `DAYFLOW_KEYSTORE_PATH`
- `DAYFLOW_KEYSTORE_PASSWORD`
- `DAYFLOW_KEY_ALIAS`
- `DAYFLOW_KEY_PASSWORD`

*Note: In the absence of production keys during automated local verification, Gradle falls back to the debug signing configuration to permit full R8 and bytecode compilation tests.*

---

## 3. OEM Background Execution & Battery Optimization Matrix

Android device manufacturers implement non-standard aggressive background process limiters that can delay or terminate exact alarms and scheduled WorkManager backups. Dayflow mitigates these through graceful fallbacks and user disclosure.

### 3.1 Xiaomi / MIUI / HyperOS
* **Symptoms:** Delayed notifications, cancelled WorkManager jobs after 24–48 hours, missed alarms when the app is swiped from Recent Apps.
* **Mitigations & Required User Actions:**
  1. Open **Settings** $\to$ **Apps** $\to$ **Manage apps** $\to$ **DayFlow**.
  2. Enable **Autostart** (toggle ON).
  3. Under **Battery saver**, select **No restrictions** (instead of "Battery saver (recommended)").
  4. In the app drawer or Recent Apps, long-press DayFlow and tap the **Lock** icon to keep the process protected.

### 3.2 Samsung OneUI (Android 12–15)
* **Symptoms:** App placed into "Sleeping apps" or "Deep sleeping apps" after several days of inactivity, preventing alarm delivery.
* **Mitigations & Required User Actions:**
  1. Open **Settings** $\to$ **Apps** $\to$ **DayFlow** $\to$ **Battery**.
  2. Select **Unrestricted** (default is often "Optimized").
  3. Open **Settings** $\to$ **Battery** $\to$ **Background usage limits**.
  4. Ensure DayFlow is **NOT** listed in *Sleeping apps* or *Deep sleeping apps*.
  5. Add DayFlow to **Never sleeping apps**.

### 3.3 OnePlus / OPPO / Realme (OxygenOS / ColorOS)
* **Symptoms:** Alarms suppressed during nighttime deep sleep (Sleep Standby Optimization).
* **Mitigations & Required User Actions:**
  1. Open **Settings** $\to$ **Battery** $\to$ **More settings** $\to$ **App battery management** $\to$ **DayFlow**.
  2. Turn ON **Allow background activity** and **Allow auto-launch**.
  3. Disable **Sleep standby optimization** if reminders must ring reliably during sleep hours.

### 3.4 Huawei (EMUI / HarmonyOS)
* **Symptoms:** PowerGenie terminates background processes immediately upon screen lock.
* **Mitigations & Required User Actions:**
  1. Open **Settings** $\to$ **Battery** $\to$ **App launch** $\to$ **DayFlow**.
  2. Toggle **Manage automatically** to **OFF** (switch to **Manage manually**).
  3. Ensure all three toggles are enabled: **Auto-launch**, **Secondary launch**, and **Run in background**.

### 3.5 Google Pixel / Stock Android (AOSP)
* **Symptoms:** Standard Doze mode delays alarms unless exact alarm permission is granted.
* **Mitigations:**
  1. Ensure `SCHEDULE_EXACT_ALARM` is granted: **Settings** $\to$ **Apps** $\to$ **Special app access** $\to$ **Alarms & reminders** $\to$ **DayFlow** $\to$ **Allowed**.
  2. For critical reliability: **Settings** $\to$ **Apps** $\to$ **DayFlow** $\to$ **App battery usage** $\to$ **Unrestricted**.

---

## 4. System Recovery & Lifecycle Checklist

| Scenario | Trigger Mechanism | Expected System Response | Verification Method |
| :--- | :--- | :--- | :--- |
| **Device Reboot** | `BOOT_COMPLETED` | `BootReceiver` invokes `RescheduleAllUseCase()`. All future active reminders and daily briefing/review alarms are re-armed. | `BootReceiverTest.kt` passes. Verify via `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`. |
| **Manual Time Change** | `TIME_SET` | `TimeChangeReceiver` re-computes next occurrences for all active schedules. | `TimeChangeReceiverTest.kt` passes. Verify via `adb shell am broadcast -a android.intent.action.TIME_SET`. |
| **Timezone Change** | `TIMEZONE_CHANGED` | Occurrence anchors are re-anchored to the new ZoneId without corrupting local date keys. | `TimeChangeReceiverTest.kt` passes. |
| **Exact Alarm Permission Revocation** | `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` | `ExactAlarmPermissionReceiver` refreshes `SchedulingCapability` Flow to `APPROXIMATE_FALLBACK`. Alarms continue firing via `setAndAllowWhileIdle`. | `ExactAlarmPermissionReceiverTest.kt` passes. |
| **App Update** | `MY_PACKAGE_REPLACED` | `PackageReplacedReceiver` re-registers all alarms and WorkManager tasks. | `PackageReplacedReceiverTest.kt` passes. |
| **Process Death** | OS memory reclamation | App state restores from SQLite database. Unsaved form drafts survive via `SavedStateHandle`. | Tested via `Don't keep activities` in Developer Options. |

---

## 5. Local Data Safety & Migration Verification

1. **Auto-Backup Ban:**
   `android:allowBackup="false"` is mandatory. Android's default cloud backup to Google Drive would violate the 100% offline security boundary and could overwrite or corrupt SQLite databases with stale cloud snapshots.
2. **Room Migration Safety:**
   - Any schema changes must be accompanied by an explicit `Migration(from, to)`.
   - `fallbackToDestructiveMigration` is permanently prohibited by architecture and git pre-commit checks.
   - Tested using `MigrationTest.kt` verifying data preservation from version 1 to future versions.
3. **Backup Engine Health:**
   - WorkManager runs `DailyBackupWorker` every 24 hours.
   - Retention policy enforces a strict maximum of 7 snapshots; older snapshots are purged only after the new snapshot is verified.
   - Manual SAF export generates self-contained ZIP archives containing `data.json` and a SHA-256 `manifest.json`.
   - SAF import verifies checksum, limits decompression size (zip bomb mitigation), validates schema compatibility, and executes database replacement inside an atomic transaction.

---

## 6. Accessibility & Performance Verification

- [x] **48dp Minimum Touch Target:** All buttons, chips, FABs, and icon buttons adhere to Material 3 48dp minimum interactive target size.
- [x] **Font Scaling (200%):** UI uses scalable `sp` units and fluid weights to prevent clipping or truncation when large system fonts are active.
- [x] **TalkBack Semantics:** All charts (`WeeklyBarChart`, `LineChart`, `MultiProgressRing`, `CalendarHeatmap`, `DailyTimeline`) provide descriptive text summaries for screen readers via `semantics { contentDescription = "..." }`.
- [x] **Color Contrast:** All text and critical UI elements exceed WCAG AA contrast ratio (4.5:1 for body text, 3:1 for large text and icons) on both Dark (`#121212`) and Light (`#FAFAFA`) backgrounds.
- [x] **Reduced Motion:** Screen navigation uses instant transitions (`EnterTransition.None` / `ExitTransition.None`) to respect accessibility preferences.
- [x] **Stable Lazy Keys:** Every item in `LazyColumn` and `LazyRow` throughout the app provides an explicit, stable `key = { it.id }` parameter.
- [x] **R8 Code & Resource Shrinking:** Verified via `./gradlew assembleRelease`.

# DAYFLOW — SECURITY.md

## 1. Security & Privacy Philosophy
DAYFLOW is designed as a local-only, zero-telemetry personal productivity application. User privacy and operational integrity are paramount: no personal data, tasks, habits, or reminder schedules ever leave the user's physical device.

---

## 2. Zero-Network Policy
- **No Internet Permission:** `android.permission.INTERNET` is strictly forbidden and absent from `AndroidManifest.xml`.
- **Zero Remote Dependencies:** No external analytics SDKs, cloud synchronization APIs, third-party trackers, or crash-reporting servers are linked or bundled.
- **Physical Boundary:** All computation, database queries, and notifications execute completely on-device.

---

## 3. Alarm Scheduling & System Power Management

### A. Google Play Exact Alarm Policy (`SCHEDULE_EXACT_ALARM` vs `USE_EXACT_ALARM`)
Android 12 (API 31+) and Android 13/14 (API 33/34+) introduce stringent system governance around exact alarms:
- **`USE_EXACT_ALARM` Policy Restriction:** Google Play developer policies explicitly restrict `USE_EXACT_ALARM` exclusively to pure Alarm Clock and Timer applications whose core function is sounding wake-up alarms. Productivity, habit, and general reminder apps using `USE_EXACT_ALARM` risk policy violation and store rejection.
- **`SCHEDULE_EXACT_ALARM` Compliance:** DAYFLOW requests `SCHEDULE_EXACT_ALARM`. On API 33+, this permission can be revoked by the user or system power managers.
- **Transparent Inexact Fallback:** DAYFLOW never assumes exact alarm permissions are present:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
  } else {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
  }
  ```
- **Defensive Exception Handling:** Any call to schedule exact alarms is guarded against `SecurityException`, automatically failing over to `setAndAllowWhileIdle`.
- **State Disclosure:** The scheduling engine observes system capability via `SchedulingCapability` (`EXACT`, `APPROXIMATE_FALLBACK`, `NOTIFICATIONS_DENIED`) and discloses approximate status to the user in the UI with a clear option to grant exact permissions in Android Settings.

### B. Android Doze Mode & Deep Idle
- DAYFLOW uses `setExactAndAllowWhileIdle` and `setAndAllowWhileIdle`. These APIs allow alarms to wake the application CPU even when the device enters Android deep Doze mode, ensuring critical deadlines fire on schedule.

### C. OEM Battery Optimization & Background Execution
Aggressive OEM battery management solutions (e.g. Samsung One UI, Xiaomi MIUI/HyperOS, Huawei EMUI/HarmonyOS, OnePlus OxygenOS) frequently kill background processes, delay alarms, or disable `BOOT_COMPLETED` receivers unless explicit exemptions are granted:
- **Auto-Start & Background Execution:** On certain devices, users must enable "Auto-start" or set battery usage to "Unrestricted" in app info.
- **Disclosure & User Guidance:** The application provides user-facing settings explanations guiding users to disable OEM battery optimizations if reminders are observed to be delayed.
- **No Rogue Services:** DAYFLOW never runs a continuous foreground service or sticky background service to bypass battery optimizations. All scheduling uses one-shot `AlarmManager` alarms and short-lived `goAsync()` BroadcastReceivers.

---

## 4. Inter-Process Communication (IPC) & Component Security

### A. Explicit Intents & Target Components
- All internal system triggers (`AlarmReceiver`, `NotificationActionReceiver`) use explicit component intents:
  ```kotlin
  val intent = Intent(context, AlarmReceiver::class.java).apply { ... }
  ```
- Receivers handling internal application actions are marked `android:exported="false"` in `AndroidManifest.xml` unless required by the system (e.g., `BootReceiver` and `TimeChangeReceiver` are exported to receive system broadcasts but require explicit system action filters).

### B. PendingIntent Immutability & Request Codes
- All `PendingIntent` instances specify `FLAG_IMMUTABLE` to prevent third-party intent hijacking or modification:
  ```kotlin
  val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
  ```
- Every scheduled occurrence and notification action derives a deterministic, collision-resistant 31-bit positive request code:
  ```kotlin
  val requestCode = (occurrenceId.hashCode() and 0x7FFFFFFF)
  ```

---

## 5. Data Storage & Cryptographic Verification

### A. Local Storage Encapsulation
- The SQLite database (`dayflow.db`) and DataStore preferences are stored in Android application-private internal storage (`/data/user/0/com.dayflow.app/databases/`), protected by the Linux UID sandbox.

### B. Backup Archive Integrity & Safety Protections
- Local backups created via WorkManager or exported via Storage Access Framework (SAF) are packaged as ZIP archives containing `data.json` and `manifest.json`.
- **Cryptographic SHA-256 Verification:** `BackupEngine` validates `data.json` against `manifest.dataSha256` prior to parsing. If hashes mismatch, restore aborts immediately.
- **Zip-Bomb Defenses:**
  - Maximum uncompressed size is capped at 50 MB (`MAX_UNCOMPRESSED_SIZE_BYTES`).
  - Maximum compression ratio is capped at 100:1.
  - Archive entry count is limited to 10 entries maximum.
  - Decompression is performed with bounded stream readers; exceeding limits aborts immediately without exhausting device RAM or flash storage.
- **Path Traversal Guards:** All file operations validate that target files reside strictly within `files/backups` directory:
  ```kotlin
  if (file.canonicalFile.parentFile != backupsDir.canonicalFile) return false
  ```
- **Storage Access Framework (SAF) Sandboxing:** Manual export and import use SAF `CreateDocument` and `OpenDocument` with scoped URIs. No broad external storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`) are requested or granted.
- **Pre-Restore Safety Snapshot & Rollback:** The live database is never modified before validation succeeds. A safety snapshot of the live database is created prior to restore; the restore executes in a single Room transaction, rolling back completely on any SQLite error.
- **Honest User Disclosure:** The UI explicitly discloses to the user that app-private snapshots do not survive app uninstallation or device loss, guiding users to export archives externally.

---

## 6. Log Sanitization & Sensitive Information
- **Zero Sensitive Data in Logcat:** Titles, descriptions, notes, and user personal identifiers are NEVER printed to Android system logs (`android.util.Log`).
- Diagnostic logs in debug builds record only masked IDs and status codes.
- No secrets or credentials are hardcoded or tracked in source control.

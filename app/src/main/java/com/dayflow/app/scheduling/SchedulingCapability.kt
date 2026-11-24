package com.dayflow.app.scheduling

/**
 * Represents the current scheduling capability of the device environment.
 * Exposed via Flow for transparent UI disclosure to the user.
 */
enum class SchedulingCapability {
    /**
     * Exact alarm capability is granted and notifications are enabled.
     * Alarms fire with millisecond precision at the requested wall-clock instant.
     */
    EXACT,

    /**
     * Notification permission is granted, but exact alarm permission is absent or revoked.
     * Alarms fall back gracefully to inexact delivery (setAndAllowWhileIdle), which may
     * be subject to Android OS Doze and batching delays of several minutes.
     */
    APPROXIMATE_FALLBACK,

    /**
     * Notification permission (POST_NOTIFICATIONS) is denied.
     * Notifications cannot be posted to the system tray.
     */
    NOTIFICATIONS_DENIED
}

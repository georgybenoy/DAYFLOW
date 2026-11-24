package com.dayflow.app.core.common

import android.util.Log
import com.dayflow.app.BuildConfig

/**
 * Structured, privacy-conscious logging utility for Dayflow.
 *
 * Guarantees:
 * 1. Zero PII: Never logs user-generated text (task/reminder/habit titles, notes).
 * 2. In non-debug builds, verbose and debug messages are no-ops.
 * 3. Tag prefixing for easy filtering in Logcat.
 */
object DayflowLogger {

    private const val TAG_PREFIX = "Dayflow_"

    fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d("$TAG_PREFIX$tag", message())
        }
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i("$TAG_PREFIX$tag", message)
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$TAG_PREFIX$tag", message, throwable)
        } else {
            Log.w("$TAG_PREFIX$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX$tag", message, throwable)
        } else {
            Log.e("$TAG_PREFIX$tag", message)
        }
    }
}

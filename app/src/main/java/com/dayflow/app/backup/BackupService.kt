package com.dayflow.app.backup

import com.dayflow.app.backup.model.BackupManifest
import com.dayflow.app.backup.model.BackupSnapshotInfo
import com.dayflow.app.backup.model.RestoreResult
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface BackupService {

    /**
     * Creates an automated or manual local snapshot ZIP in app-private storage.
     * Enforces atomic writing via temp file, fsync, verification, and retention policy.
     */
    suspend fun createBackupSnapshot(): Result<BackupSnapshotInfo>

    /**
     * Restores application database and settings from an app-private snapshot file.
     * Executes full 10-step validation and atomic Room transaction.
     */
    suspend fun restoreFromSnapshot(snapshotFile: File): RestoreResult

    /**
     * Streams current application data into an [outputStream] (Storage Access Framework export).
     */
    suspend fun exportBackup(outputStream: OutputStream): Result<BackupManifest>

    /**
     * Restores application data from an external [inputStream] (Storage Access Framework import).
     */
    suspend fun importBackup(inputStream: InputStream, sizeHint: Long = -1): RestoreResult

    /**
     * Returns list of existing verified snapshot ZIP archives in app-private storage, sorted newest first.
     */
    suspend fun getSnapshots(): List<BackupSnapshotInfo>

    /**
     * Deletes a specific snapshot file from app-private storage.
     */
    suspend fun deleteSnapshot(filename: String): Boolean

    /**
     * Cleans up any orphaned temporary files left over from interrupted backup writes.
     */
    suspend fun cleanupTempFiles()
}
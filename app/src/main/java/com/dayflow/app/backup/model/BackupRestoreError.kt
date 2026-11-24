package com.dayflow.app.backup.model

import java.io.File
import java.time.Instant

sealed class BackupRestoreError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {
    data class InvalidZipArchive(val details: String) :
        BackupRestoreError("Invalid or corrupted ZIP archive: $details")

    data class ZipBombDetected(val uncompressedSize: Long, val limit: Long) :
        BackupRestoreError("Uncompressed archive size ($uncompressedSize bytes) exceeds security limit ($limit bytes)")

    data class ChecksumMismatch(val expected: String, val actual: String) :
        BackupRestoreError("Integrity check failed: Expected SHA-256 $expected, but got $actual")

    data class UnsupportedSchemaVersion(val version: Int, val maxSupported: Int) :
        BackupRestoreError("Backup schema version $version is newer than supported version $maxSupported")

    data class InvalidRecordData(val recordType: String, val recordId: String, val reason: String) :
        BackupRestoreError("Invalid record in $recordType [$recordId]: $reason")

    data class BrokenReferenceConstraint(val details: String) :
        BackupRestoreError("Referential integrity violation: $details")

    data class SafetySnapshotFailed(val reason: String) :
        BackupRestoreError("Pre-restore safety snapshot failed: $reason")

    data class DatabaseTransactionFailed(val causeException: Throwable) :
        BackupRestoreError("Database transaction failed and was rolled back: ${causeException.message}", causeException)

    data class IoError(val messageText: String) :
        BackupRestoreError("I/O error during backup operation: $messageText")
}

sealed class RestoreResult {
    data class Success(
        val snapshotDate: Instant,
        val totalRestoredRecords: Int,
        val counts: Map<String, Int>
    ) : RestoreResult()

    data class Failure(
        val error: BackupRestoreError
    ) : RestoreResult()
}

data class BackupSnapshotInfo(
    val filename: String,
    val file: File,
    val sizeBytes: Long,
    val createdAt: Instant,
    val recordsCount: Int? = null
)
package com.fittrack.app.data.backup

import com.fittrack.app.data.preferences.UserPreferencesRepository
import com.fittrack.app.data.remote.DriveApiService
import com.fittrack.app.data.remote.DriveFile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_PREFIX = "fittrack-backup-"
private const val MAX_BACKUPS_KEPT = 5

@Singleton
class DriveBackupRepository @Inject constructor(
    private val authManager: DriveAuthManager,
    private val driveApi: DriveApiService,
    private val backupManager: BackupManager,
    private val preferencesRepository: UserPreferencesRepository
) {

    /** Exporta o banco e envia ao appDataFolder; mantém só os [MAX_BACKUPS_KEPT] mais recentes. */
    suspend fun backupNow(): DriveFile {
        val auth = authManager.authorizationHeader()
        val bytes = backupManager.exportToBytes()
        val timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        val name = "$BACKUP_PREFIX$timestamp.zip"

        val metadata = """{"name":"$name","parents":["appDataFolder"]}"""
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(bytes.toRequestBody("application/zip".toMediaType()))
            .build()

        val uploaded = driveApi.uploadFile(auth, body)
        pruneOldBackups(auth)
        preferencesRepository.setLastDriveBackupAt(System.currentTimeMillis())
        return uploaded
    }

    /** Backups no Drive, do mais recente para o mais antigo. */
    suspend fun listBackups(): List<DriveFile> =
        driveApi.listFiles(authManager.authorizationHeader())
            .files.filter { it.name.startsWith(BACKUP_PREFIX) }

    /** Baixa o backup mais recente e o aplica com o [mode]; null se não houver backup. */
    suspend fun restoreLatest(mode: RestoreMode): RestoreSummary? {
        val latest = listBackups().firstOrNull() ?: return null
        val auth = authManager.authorizationHeader()
        return driveApi.downloadFile(auth, latest.id).byteStream().use { stream ->
            backupManager.restore(backupManager.parseBackup(stream), mode)
        }
    }

    private suspend fun pruneOldBackups(auth: String) {
        val backups = driveApi.listFiles(auth).files.filter { it.name.startsWith(BACKUP_PREFIX) }
        backups.drop(MAX_BACKUPS_KEPT).forEach { driveApi.deleteFile(auth, it.id) }
    }
}

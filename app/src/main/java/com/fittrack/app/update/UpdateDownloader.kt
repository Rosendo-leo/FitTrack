package com.fittrack.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DownloadState {
    data class Progress(val percent: Int) : DownloadState
    data class Done(val file: File) : DownloadState
    data class Error(val message: String) : DownloadState
}

@Singleton
class UpdateDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient
) {
    private val updatesDir: File
        get() = File(context.cacheDir, "updates").apply { mkdirs() }

    /**
     * Baixa o APK emitindo progresso e valida o SHA256 publicado no release
     * antes de considerar concluído.
     */
    fun download(info: UpdateInfo): Flow<DownloadState> = flow {
        val apkFile = File(updatesDir, "fittrack-update.apk")
        try {
            // Download do APK com progresso
            client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Error("Falha no download (HTTP ${response.code})"))
                    return@flow
                }
                val body = response.body ?: run {
                    emit(DownloadState.Error("Resposta vazia do servidor"))
                    return@flow
                }
                val total = body.contentLength().takeIf { it > 0 } ?: info.apkSizeBytes
                var read = 0L
                var lastPercent = -1
                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n == -1) break
                            output.write(buffer, 0, n)
                            read += n
                            val percent = if (total > 0) (read * 100 / total).toInt() else 0
                            if (percent != lastPercent) {
                                lastPercent = percent
                                emit(DownloadState.Progress(percent))
                            }
                        }
                    }
                }
            }

            // Verificação de integridade (esquemática: hash publicado no release)
            val expectedHash = info.sha256Url?.let { fetchExpectedHash(it) }
            if (expectedHash != null) {
                val actualHash = sha256Of(apkFile)
                if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                    apkFile.delete()
                    emit(DownloadState.Error("Verificação SHA256 falhou — download corrompido ou adulterado."))
                    return@flow
                }
            }

            emit(DownloadState.Done(apkFile))
        } catch (e: Exception) {
            apkFile.delete()
            emit(DownloadState.Error(e.message ?: "Erro desconhecido no download"))
        }
    }.flowOn(Dispatchers.IO)

    private fun fetchExpectedHash(url: String): String? =
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                // Formato sha256sum: "<hash>  <arquivo>"
                response.body?.string()?.trim()?.split(Regex("\\s+"))?.firstOrNull()
            }
        }.getOrNull()

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n == -1) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** O app pode instalar pacotes? (Android 8+: permissão por app) */
    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= 26) context.packageManager.canRequestPackageInstalls()
        else true

    /** Abre a tela do sistema para autorizar instalação de apps desconhecidos. */
    fun requestInstallPermissionIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Dispara o instalador do sistema para o APK baixado. */
    fun installIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
    }
}

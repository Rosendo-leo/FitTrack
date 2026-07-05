package com.fittrack.app.update

import com.fittrack.app.BuildConfig
import com.fittrack.app.data.remote.GitHubApiService
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val sha256Url: String?
)

@Singleton
class UpdateChecker @Inject constructor(
    private val api: GitHubApiService
) {
    companion object {
        const val REPO_OWNER = "Rosendo-leo"
        const val REPO_NAME = "FitTrack"
    }

    /** Retorna os dados do update se houver versão mais nova no GitHub; null caso contrário. */
    suspend fun check(): UpdateInfo? {
        val release = runCatching { api.latestRelease(REPO_OWNER, REPO_NAME) }
            .getOrNull() ?: return null

        val remote = parseVersion(release.tagName) ?: return null
        val local = parseVersion(BuildConfig.VERSION_NAME) ?: return null
        if (compareVersions(remote, local) <= 0) return null

        val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return null
        val sha = release.assets.firstOrNull { it.name.endsWith(".sha256") }

        return UpdateInfo(
            versionName = release.tagName.removePrefix("v"),
            releaseNotes = release.body?.take(2000) ?: "Sem notas de versão.",
            apkUrl = apk.downloadUrl,
            apkSizeBytes = apk.size,
            sha256Url = sha?.downloadUrl
        )
    }

    /** "v1.2.3" ou "1.2.3" → [1, 2, 3]; null se não parsear. */
    internal fun parseVersion(raw: String): List<Int>? {
        val parts = raw.trim().removePrefix("v").substringBefore('-').split('.')
        val numbers = parts.mapNotNull { it.toIntOrNull() }
        return numbers.takeIf { it.isNotEmpty() && it.size == parts.size }
    }

    internal fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = (a.getOrElse(i) { 0 }).compareTo(b.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}

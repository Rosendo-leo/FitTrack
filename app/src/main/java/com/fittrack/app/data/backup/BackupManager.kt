package com.fittrack.app.data.backup

import androidx.room.withTransaction
import com.fittrack.app.BuildConfig
import com.fittrack.app.data.local.AppDatabase
import com.fittrack.app.data.local.dao.BackupDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val JSON_ENTRY_NAME = "fittrack-backup.json"

class BackupFormatException(message: String) : Exception(message)

@Singleton
class BackupManager @Inject constructor(
    private val database: AppDatabase,
    private val backupDao: BackupDao,
    private val json: Json
) {

    /** Snapshot completo do banco. */
    suspend fun collectBackup(): BackupData = database.withTransaction {
        BackupData(
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = System.currentTimeMillis(),
            templates = backupDao.allTemplates().map(BackupTemplate::from),
            exercises = backupDao.allExercises().map(BackupExercise::from),
            sessions = backupDao.allSessions().map(BackupSession::from),
            sets = backupDao.allSets().map(BackupSet::from),
            bodyMetrics = backupDao.allBodyMetrics().map(BackupBodyMetric::from),
            cardioSessions = backupDao.allCardioSessions().map(BackupCardio::from)
        )
    }

    /** Escreve o backup como ZIP (contendo o JSON) em [output]. */
    suspend fun exportToZip(output: OutputStream) {
        val data = collectBackup()
        withContext(Dispatchers.IO) {
            ZipOutputStream(output.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(JSON_ENTRY_NAME))
                zip.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
                zip.closeEntry()
            }
        }
    }

    /** Backup como bytes de um ZIP (para upload no Drive). */
    suspend fun exportToBytes(): ByteArray {
        val buffer = ByteArrayOutputStream()
        exportToZip(buffer)
        return buffer.toByteArray()
    }

    /**
     * Lê um backup de [input] — aceita o ZIP exportado ou o JSON puro.
     * Lança [BackupFormatException] se o conteúdo não for um backup válido.
     */
    suspend fun parseBackup(input: InputStream): BackupData = withContext(Dispatchers.IO) {
        val stream = BufferedInputStream(input)
        stream.mark(4)
        val header = ByteArray(4)
        val read = stream.read(header)
        stream.reset()

        val jsonText = if (read >= 4 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()) {
            readJsonFromZip(stream)
        } else {
            stream.readBytes().decodeToString()
        }
        val data = try {
            json.decodeFromString(BackupData.serializer(), jsonText)
        } catch (e: Exception) {
            throw BackupFormatException("Arquivo não é um backup válido do FitTrack.")
        }
        if (data.schemaVersion > BACKUP_SCHEMA_VERSION) {
            throw BackupFormatException(
                "Backup criado por uma versão mais nova do app " +
                    "(schema ${data.schemaVersion} > $BACKUP_SCHEMA_VERSION). Atualize o FitTrack."
            )
        }
        data
    }

    private fun readJsonFromZip(stream: InputStream): String {
        ZipInputStream(stream).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".json")) {
                    return zip.readBytes().decodeToString()
                }
                entry = zip.nextEntry
            }
        }
        throw BackupFormatException("ZIP não contém um backup do FitTrack.")
    }

    /** Aplica [data] no banco conforme o [mode]; tudo em uma única transação. */
    suspend fun restore(data: BackupData, mode: RestoreMode): RestoreSummary =
        database.withTransaction {
            when (mode) {
                RestoreMode.REPLACE -> replaceAll(data)
                RestoreMode.MERGE -> mergeInto(data)
            }
        }

    private suspend fun replaceAll(data: BackupData): RestoreSummary {
        backupDao.clearSets()
        backupDao.clearSessions()
        backupDao.clearExercises()
        backupDao.clearTemplates()
        backupDao.clearBodyMetrics()
        backupDao.clearCardioSessions()

        data.templates.forEach { backupDao.insertTemplate(it.toEntity()) }
        data.exercises.forEach { backupDao.insertExercise(it.toEntity()) }
        data.sessions.forEach { backupDao.insertSession(it.toEntity()) }
        data.sets.forEach { backupDao.insertSet(it.toEntity()) }
        data.bodyMetrics.forEach { backupDao.insertBodyMetric(it.toEntity()) }
        data.cardioSessions.forEach { backupDao.insertCardioSession(it.toEntity()) }

        return RestoreSummary(
            templates = data.templates.size,
            sessions = data.sessions.size,
            bodyMetrics = data.bodyMetrics.size,
            cardioSessions = data.cardioSessions.size
        )
    }

    /**
     * Merge idempotente: templates são casados por nome (novos são criados),
     * sessões deduplicadas por startedAt, métricas por data e cardio por data+tipo.
     */
    private suspend fun mergeInto(data: BackupData): RestoreSummary {
        val exercisesByTemplate = data.exercises.groupBy { it.templateId }

        // backupTemplateId -> localTemplateId
        val templateIdMap = mutableMapOf<Long, Long>()
        // backupExerciseId -> localExerciseId
        val exerciseIdMap = mutableMapOf<Long, Long>()
        var newTemplates = 0

        for (template in data.templates) {
            val backupExercises = exercisesByTemplate[template.id].orEmpty()
            val existingId = backupDao.templateIdByName(template.name, template.isPreset)
            if (existingId != null) {
                templateIdMap[template.id] = existingId
                // Casa exercícios por nome; cria os que não existirem localmente.
                val localByName = backupDao.exercisesOf(existingId).associateBy { it.name }
                for (exercise in backupExercises) {
                    val local = localByName[exercise.name]
                    exerciseIdMap[exercise.id] = local?.id
                        ?: backupDao.insertExercise(exercise.toEntity(id = 0, templateId = existingId))
                }
            } else {
                val newId = backupDao.insertTemplate(template.toEntity(id = 0))
                templateIdMap[template.id] = newId
                newTemplates++
                for (exercise in backupExercises) {
                    exerciseIdMap[exercise.id] =
                        backupDao.insertExercise(exercise.toEntity(id = 0, templateId = newId))
                }
            }
        }

        val setsBySession = data.sets.groupBy { it.sessionId }
        var newSessions = 0
        for (session in data.sessions) {
            if (backupDao.sessionExistsAt(session.startedAt)) continue
            val newSessionId = backupDao.insertSession(
                session.toEntity(id = 0, templateId = session.templateId?.let(templateIdMap::get))
            )
            newSessions++
            for (set in setsBySession[session.id].orEmpty()) {
                val localExerciseId = exerciseIdMap[set.exerciseId] ?: continue
                backupDao.insertSet(set.toEntity(id = 0, sessionId = newSessionId, exerciseId = localExerciseId))
            }
        }

        var newMetrics = 0
        for (metric in data.bodyMetrics) {
            if (backupDao.bodyMetricExistsAt(metric.date)) continue
            backupDao.insertBodyMetric(metric.toEntity(id = 0))
            newMetrics++
        }

        var newCardio = 0
        for (cardio in data.cardioSessions) {
            if (backupDao.cardioExistsAt(cardio.date, cardio.type.name)) continue
            backupDao.insertCardioSession(cardio.toEntity(id = 0))
            newCardio++
        }

        return RestoreSummary(
            templates = newTemplates,
            sessions = newSessions,
            bodyMetrics = newMetrics,
            cardioSessions = newCardio
        )
    }
}

package com.fittrack.app.data.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.fittrack.app.BuildConfig
import com.fittrack.app.data.repository.WorkoutRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

class SharedWorkoutFormatException(message: String) : Exception(message)

/** Treino grande demais para caber num QR code de forma escaneável. */
class SharedWorkoutTooLargeException(message: String) : Exception(message)

@Singleton
class WorkoutShareManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workoutRepository: WorkoutRepository,
    private val json: Json
) {
    private val sharedDir: File
        get() = File(context.cacheDir, "shared").apply { mkdirs() }

    /** Monta o DTO exportável de um treino, ou null se ele não existir mais. */
    suspend fun collectSharedWorkout(templateId: Long): SharedWorkout? {
        val template = workoutRepository.getTemplate(templateId) ?: return null
        val exercises = workoutRepository.getExercises(templateId)
        return SharedWorkout(
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = System.currentTimeMillis(),
            template = SharedTemplate.from(template),
            exercises = exercises.map(SharedExercise::from)
        )
    }

    /** Escreve o treino como JSON num arquivo temporário do cache, pronto para compartilhar. */
    suspend fun writeToCacheFile(workout: SharedWorkout): File = withContext(Dispatchers.IO) {
        val safeName = workout.template.name
            .replace(Regex("[^A-Za-z0-9-_ ]"), "")
            .trim()
            .ifBlank { "treino" }
        val file = File(sharedDir, "$safeName.fittrack-workout.json")
        file.writeText(json.encodeToString(SharedWorkout.serializer(), workout))
        file
    }

    /** Uri de compartilhamento (FileProvider) para um arquivo já escrito em [sharedDir]. */
    fun shareIntent(file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Compartilhar treino")
    }

    /**
     * Gera um QR code (preto e branco, [sizePx]×[sizePx]) com o treino codificado como JSON —
     * não depende de rede nem de servidor. Só é útil para compartilhar entre dois FitTrack:
     * um leitor de QR genérico mostraria o JSON bruto, não um treino importável.
     */
    suspend fun generateQrBitmap(workout: SharedWorkout, sizePx: Int = 800): Bitmap =
        withContext(Dispatchers.Default) {
            val text = json.encodeToString(SharedWorkout.serializer(), workout)
            val matrix = try {
                QRCodeWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    sizePx,
                    sizePx,
                    mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L)
                )
            } catch (e: Exception) {
                throw SharedWorkoutTooLargeException(
                    "Esse treino tem exercícios/notas demais para caber num QR code. Use \"Compartilhar arquivo\"."
                )
            }
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        }

    /**
     * Lê um treino compartilhado de [input].
     * Lança [SharedWorkoutFormatException] se o conteúdo não for um treino válido.
     */
    suspend fun parseSharedWorkout(input: InputStream): SharedWorkout = withContext(Dispatchers.IO) {
        val text = input.use { it.readBytes().decodeToString() }
        val data = try {
            json.decodeFromString(SharedWorkout.serializer(), text)
        } catch (e: Exception) {
            throw SharedWorkoutFormatException("Arquivo não é um treino válido do FitTrack.")
        }
        if (data.schemaVersion > SHARED_WORKOUT_SCHEMA_VERSION) {
            throw SharedWorkoutFormatException(
                "Este treino foi exportado por uma versão mais nova do FitTrack. Atualize o app."
            )
        }
        data
    }

    /** Insere o treino recebido como um novo item em "Meus treinos". Retorna o id criado. */
    suspend fun importAsNewTemplate(data: SharedWorkout): Long =
        workoutRepository.importTemplate(
            template = data.template.toEntity(),
            exercises = data.exercises.map { it.toEntity(templateId = 0) }
        )
}

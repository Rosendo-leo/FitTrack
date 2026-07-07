package com.fittrack.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Dias seguidos de treino até [today] (inclusive), contando a partir de hoje ou
 * de ontem se ainda não houver treino hoje. [trainedDays] são as datas com sessão finalizada.
 */
fun currentStreak(trainedDays: Set<LocalDate>, today: LocalDate): Int {
    var streak = 0
    var cursor = if (today in trainedDays) today else today.minusDays(1)
    while (cursor in trainedDays) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/** Segunda a domingo (índices 0..6) indicando se houve treino, para a semana de [today]. */
fun weekTrainedFlags(trainedDays: Set<LocalDate>, today: LocalDate): List<Boolean> {
    val monday = today.with(DayOfWeek.MONDAY)
    return (0..6).map { monday.plusDays(it.toLong()) in trainedDays }
}

/** 1RM estimado pela fórmula de Epley. Para 1 rep, é a própria carga. */
fun epley1Rm(weightKg: Float, reps: Int): Float =
    if (reps <= 1) weightKg else weightKg * (1f + reps / 30f)

/**
 * Melhor 1RM estimado por sessão, em ordem cronológica.
 * Entrada: (data da sessão, carga, reps) das séries válidas.
 */
fun strengthProgression(samples: List<Triple<Long, Float, Int>>): List<Pair<Long, Float>> =
    samples
        .groupBy { it.first }
        .map { (date, sets) -> date to sets.maxOf { epley1Rm(it.second, it.third) } }
        .sortedBy { it.first }

/** Início (segunda-feira, 00:00) da semana que contém o instante dado. */
fun weekStartMillis(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone).toInstant().toEpochMilli()

/**
 * Volume somado por semana, em ordem cronológica.
 * Entrada: (instante da sessão, volume da sessão).
 */
fun weeklyVolume(
    sessions: List<Pair<Long, Float>>,
    zone: ZoneId = ZoneId.systemDefault()
): List<Pair<Long, Float>> =
    sessions
        .groupBy { weekStartMillis(it.first, zone) }
        .map { (weekStart, list) -> weekStart to list.fold(0f) { acc, s -> acc + s.second } }
        .sortedBy { it.first }

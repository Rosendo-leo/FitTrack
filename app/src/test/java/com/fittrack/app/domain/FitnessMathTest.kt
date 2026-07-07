package com.fittrack.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

private fun date(iso: String): Long =
    LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

class FitnessMathTest {

    // ── 1RM (Epley) ──

    @Test
    fun `1 rep retorna a propria carga`() {
        assertEquals(100f, epley1Rm(100f, 1), 0.001f)
    }

    @Test
    fun `epley aumenta com mais reps`() {
        // 100kg x 10 reps -> 100 * (1 + 10/30) = 133.33
        assertEquals(133.33f, epley1Rm(100f, 10), 0.01f)
    }

    // ── Progressão de força ──

    @Test
    fun `strengthProgression usa a melhor serie por sessao em ordem cronologica`() {
        val samples = listOf(
            Triple(2000L, 80f, 8),   // sessão 2: 1RM = 101.33
            Triple(1000L, 100f, 5),  // sessão 1: 1RM = 116.67 (melhor da sessão)
            Triple(1000L, 90f, 5)    // sessão 1: 1RM = 105
        )
        val result = strengthProgression(samples)
        assertEquals(listOf(1000L, 2000L), result.map { it.first })
        assertEquals(116.67f, result[0].second, 0.01f)
        assertEquals(101.33f, result[1].second, 0.01f)
    }

    // ── Volume semanal ──

    @Test
    fun `weekStartMillis retorna a segunda-feira da semana`() {
        // 2024-01-10 é uma quarta-feira; a segunda daquela semana é 2024-01-08
        val wednesday = date("2024-01-10")
        val monday = date("2024-01-08")
        assertEquals(monday, weekStartMillis(wednesday, ZoneOffset.UTC))
    }

    @Test
    fun `weeklyVolume soma sessoes da mesma semana`() {
        val monday = date("2024-01-08")
        val wednesday = date("2024-01-10")
        val nextMonday = date("2024-01-15")
        val result = weeklyVolume(
            listOf(monday to 100f, wednesday to 50f, nextMonday to 200f),
            ZoneOffset.UTC
        )
        assertEquals(listOf(monday to 150f, nextMonday to 200f), result)
    }

    // ── Streak ──

    @Test
    fun `streak zero quando nao treinou hoje nem ontem`() {
        val trained = setOf(LocalDate.parse("2024-01-01"))
        assertEquals(0, currentStreak(trained, LocalDate.parse("2024-01-05")))
    }

    @Test
    fun `streak conta dias consecutivos terminando hoje`() {
        val today = LocalDate.parse("2024-01-10")
        val trained = setOf(
            today, today.minusDays(1), today.minusDays(2)
        )
        assertEquals(3, currentStreak(trained, today))
    }

    @Test
    fun `streak continua valido se ainda nao treinou hoje mas treinou ontem`() {
        val today = LocalDate.parse("2024-01-10")
        val trained = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, currentStreak(trained, today))
    }

    // ── Dias da semana treinados ──

    @Test
    fun `weekTrainedFlags marca segunda a domingo da semana atual`() {
        // 2024-01-10 é quarta; semana vai de 08 (seg) a 14 (dom)
        val today = LocalDate.parse("2024-01-10")
        val trained = setOf(LocalDate.parse("2024-01-08"), LocalDate.parse("2024-01-10"))
        val flags = weekTrainedFlags(trained, today)
        assertEquals(listOf(true, false, true, false, false, false, false), flags)
    }
}

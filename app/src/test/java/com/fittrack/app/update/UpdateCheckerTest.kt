package com.fittrack.app.update

import com.fittrack.app.data.remote.GitHubApiService
import com.fittrack.app.data.remote.GitHubRelease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private val checker = UpdateChecker(api = object : GitHubApiService {
        override suspend fun latestRelease(owner: String, repo: String): GitHubRelease =
            error("não usado nos testes de parsing")
    })

    @Test
    fun `parseVersion aceita prefixo v e sufixo de pre-release`() {
        assertEquals(listOf(1, 2, 3), checker.parseVersion("v1.2.3"))
        assertEquals(listOf(1, 2, 3), checker.parseVersion("1.2.3"))
        assertEquals(listOf(1, 0), checker.parseVersion("v1.0-beta"))
    }

    @Test
    fun `parseVersion rejeita entrada invalida`() {
        assertNull(checker.parseVersion("abc"))
        assertNull(checker.parseVersion("1.x.3"))
        assertNull(checker.parseVersion(""))
    }

    @Test
    fun `compareVersions ordena semver`() {
        assertTrue(checker.compareVersions(listOf(1, 0, 1), listOf(1, 0, 0)) > 0)
        assertTrue(checker.compareVersions(listOf(0, 9), listOf(1, 0)) < 0)
        assertEquals(0, checker.compareVersions(listOf(1, 0), listOf(1, 0, 0)))
        assertTrue(checker.compareVersions(listOf(2), listOf(1, 9, 9)) > 0)
    }
}

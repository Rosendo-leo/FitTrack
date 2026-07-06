package com.fittrack.app.ui.common

import com.fittrack.app.data.preferences.DistanceUnit
import com.fittrack.app.data.preferences.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class UnitsTest {

    @Before
    fun fixLocale() {
        // format() usa o locale padrão; fixa para o teste ser determinístico no CI
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `kg nao converte`() {
        assertEquals(82.5f, WeightUnit.KG.fromKg(82.5f), 0.001f)
        assertEquals(82.5f, WeightUnit.KG.toKg(82.5f), 0.001f)
    }

    @Test
    fun `kg para lb e volta`() {
        val lb = WeightUnit.LB.fromKg(100f)
        assertEquals(220.462f, lb, 0.01f)
        assertEquals(100f, WeightUnit.LB.toKg(lb), 0.001f)
    }

    @Test
    fun `formato de peso usa sufixo da unidade`() {
        assertEquals("82.5 kg", WeightUnit.KG.format(82.5f))
        assertEquals("100 kg", WeightUnit.KG.format(100.4f, decimals = 0))
        assertEquals("181.9 lb", WeightUnit.LB.format(82.5f))
    }

    @Test
    fun `km nao converte`() {
        assertEquals(5f, DistanceUnit.KM.fromKm(5f), 0.001f)
        assertEquals(5f, DistanceUnit.KM.toKm(5f), 0.001f)
    }

    @Test
    fun `km para mi e volta`() {
        val mi = DistanceUnit.MI.fromKm(10f)
        assertEquals(6.2137f, mi, 0.001f)
        assertEquals(10f, DistanceUnit.MI.toKm(mi), 0.001f)
    }

    @Test
    fun `formato de distancia usa sufixo da unidade`() {
        assertEquals("10.0 km", DistanceUnit.KM.format(10f))
        assertEquals("6.2 mi", DistanceUnit.MI.format(10f))
    }
}

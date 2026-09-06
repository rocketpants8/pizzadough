package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultsTest {
    @Test
    fun firstLaunchRecipeMatchesSpec() {
        val r = Defaults.recipe
        assertEquals(6, r.ballCount)
        assertEquals(250.0, r.ballWeightGrams, 0.0)
        assertEquals(75.0, r.hydrationPercent, 0.0)
        assertEquals(2.8, r.saltPercent, 0.0)
        assertEquals(YeastType.IDY, r.yeastType)
        assertEquals(true, r.usePreferment)
        assertEquals(20.0, r.prefermentFlourPercent, 0.0)
        assertEquals(45.0, r.prefermentHydrationPercent, 0.0)
        assertEquals(20.0, r.prefermentHours, 0.0)
        assertEquals(24.0, r.prefermentTempC, 0.0)
        assertEquals(4.0, r.finalHours, 0.0)
        assertEquals(24.0, r.finalTempC, 0.0)
        assertNull(r.yeastOverridePercent)
        assertNull(r.prefermentYeastOverridePercent)
        assertFalse(r.recipeExpanded)
    }
}

package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeSummaryTest {
    @Test
    fun defaultSummary() {
        assertEquals(
            "75% · 24°C · 20h biga / 4h dough",
            RecipeSummary.format(Defaults.recipe),
        )
    }

    @Test
    fun differentTemps() {
        val r = Defaults.recipe.copy(prefermentTempC = 24.0, finalTempC = 22.0)
        assertEquals(
            "75% · 20h@24°C biga / 4h@22°C dough",
            RecipeSummary.format(r),
        )
    }

    @Test
    fun poolishSummary() {
        assertEquals(
            "75% · 24°C · 20h poolish / 4h dough",
            RecipeSummary.format(Defaults.recipe.withPrefermentType(PrefermentType.POOLISH)),
        )
    }

    @Test
    fun noPreferment() {
        assertEquals(
            "75% · 24°C · 4h dough",
            RecipeSummary.format(Defaults.recipe.copy(usePreferment = false)),
        )
    }
}

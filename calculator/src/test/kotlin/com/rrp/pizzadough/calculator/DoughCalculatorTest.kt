package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoughCalculatorTest {
    @Test
    fun totalDoughEqualsBallsTimesWeight() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        assertEquals(1500.0, result.totalDoughGrams, 0.05)
        assertEquals(250.0, result.singleBallGrams, 0.05)
    }

    @Test
    fun bigaFlourIsTwentyPercentOfTotalFlour() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        val totalFlour = result.totals.first { it.name == "Flour" }.grams
        val bigaFlour = result.preferment.first { it.name == "Flour" }.grams
        assertEquals(totalFlour * 0.20, bigaFlour, 0.05)
    }

    @Test
    fun remainingFlourAndWaterAreNonNegative() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        result.finalDough.filter { it.name.startsWith("Flour") || it.name.startsWith("Water") }
            .forEach { assertTrue(it.grams >= -0.0001) }
    }

    @Test
    fun extraYeastNeverNegative() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        assertTrue(result.extraYeastGrams >= 0.0)
    }

    @Test
    fun ingredientsSumToTotalDough() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        val flour = result.totals.first { it.name == "Flour" }.grams
        val water = result.totals.first { it.name == "Water" }.grams
        val yeast = result.totals.first { it.name == "Yeast" }.grams
        val salt = result.finalDough.first { it.name == "Salt" }.grams
        assertEquals(result.totalDoughGrams, flour + water + salt + yeast, 0.05)
    }

    @Test
    fun noPrefermentPutsEverythingInFinalDough() {
        val result = DoughCalculator.calculate(Defaults.recipe.copy(usePreferment = false))
        assertTrue(result.preferment.isEmpty())
        assertEquals("Flour", result.finalDough[0].name)
    }
}

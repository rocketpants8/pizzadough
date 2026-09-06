package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeValidatorTest {
    @Test
    fun defaultsAreValid() {
        assertTrue(RecipeValidator.validate(Defaults.recipe).isEmpty())
    }

    @Test
    fun zeroBallsIsInvalid() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(ballCount = 0))
        assertTrue(errors.any { it.field == "ballCount" })
    }

    @Test
    fun emptyHydrationIsInvalid() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(hydrationPercent = 0.0))
        assertTrue(errors.any { it.field == "hydrationPercent" })
    }

    @Test
    fun yeastOverrideZeroIsAllowed() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(yeastOverridePercent = 0.0))
        assertTrue(errors.none { it.field == "yeastOverridePercent" })
    }

    @Test
    fun prefermentFlourOverOneHundredIsInvalid() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(prefermentFlourPercent = 101.0))
        assertTrue(errors.any { it.field == "prefermentFlourPercent" })
    }
}

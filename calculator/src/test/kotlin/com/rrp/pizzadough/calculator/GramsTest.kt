package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class GramsTest {
    @Test
    fun massRoundsToNearestGram() {
        assertEquals(247.0, Grams.mass(246.6), 0.0)
        assertEquals(246.0, Grams.mass(246.4), 0.0)
    }

    @Test
    fun yeastRoundsToThreeDecimals() {
        assertEquals(0.123, Grams.yeast(0.1234), 1e-9)
        assertEquals(0.124, Grams.yeast(0.1236), 1e-9)
    }
}

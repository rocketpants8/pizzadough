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
    fun yeastRoundsToTwoDecimals() {
        assertEquals(0.12, Grams.yeast(0.123), 1e-9)
        assertEquals(0.13, Grams.yeast(0.126), 1e-9)
    }
}

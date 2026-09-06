package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YeastPredictorTest {
    @Test
    fun pizzaBlabTwentyHoursAtTwentyFourCIsPointZeroOneSix() {
        assertEquals(0.016, YeastPredictor.idyPercent(24.0, 20.0), 1e-9)
    }

    @Test
    fun pizzaBlabFourHoursAtTwentyFourCIsPointOneSevenSix() {
        assertEquals(0.176, YeastPredictor.idyPercent(24.0, 4.0), 1e-9)
    }

    @Test
    fun pizzaBlabTwentyHoursAtTwentyCIsPointZeroThreeSeven() {
        assertEquals(0.037, YeastPredictor.idyPercent(20.0, 20.0), 1e-9)
    }

    @Test
    fun shorterWarmerScheduleUsesMoreYeastThanLongBiga() {
        val fourHour = YeastPredictor.idyPercent(24.0, 4.0)
        val twentyHour = YeastPredictor.idyPercent(24.0, 20.0)
        assertTrue(fourHour > twentyHour)
    }

    @Test
    fun freshIsThreePointOneTwoFiveTimesIdy() {
        val idy = YeastPredictor.percent(YeastType.IDY, 24.0, 4.0)
        val fresh = YeastPredictor.percent(YeastType.FRESH, 24.0, 4.0)
        assertEquals(idy * 3.125, fresh, 1e-9)
    }

    @Test
    fun adyIsOnePointThreeThreeTimesIdy() {
        val idy = YeastPredictor.percent(YeastType.IDY, 24.0, 4.0)
        val ady = YeastPredictor.percent(YeastType.ADY, 24.0, 4.0)
        assertEquals(idy * 1.33, ady, 1e-9)
    }
}

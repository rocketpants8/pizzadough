package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YeastPredictorTest {
    @Test
    fun anchorTwentyHoursAtTwentyCIsPointZeroFourEight() {
        assertEquals(0.048, YeastPredictor.idyPercent(20.0, 20.0), 1e-9)
    }

    @Test
    fun shorterWarmerScheduleUsesMoreYeastThanLongBiga() {
        val fourHour = YeastPredictor.idyPercent(24.0, 4.0)
        val twentyHour = YeastPredictor.idyPercent(24.0, 20.0)
        assertTrue(fourHour > twentyHour)
    }

    @Test
    fun freshIsThreeTimesIdy() {
        val idy = YeastPredictor.percent(YeastType.IDY, 24.0, 4.0)
        val fresh = YeastPredictor.percent(YeastType.FRESH, 24.0, 4.0)
        assertEquals(idy * 3.0, fresh, 1e-9)
    }

    @Test
    fun adyIsOnePointTwoFiveTimesIdy() {
        val idy = YeastPredictor.percent(YeastType.IDY, 24.0, 4.0)
        val ady = YeastPredictor.percent(YeastType.ADY, 24.0, 4.0)
        assertEquals(idy * 1.25, ady, 1e-9)
    }
}

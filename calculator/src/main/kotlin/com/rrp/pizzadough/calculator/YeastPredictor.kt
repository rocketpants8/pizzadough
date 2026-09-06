package com.rrp.pizzadough.calculator

import kotlin.math.pow

object YeastPredictor {
    fun idyPercent(tempC: Double, hours: Double): Double {
        val t = tempC.coerceIn(1.0, 40.0)
        val h = hours.coerceIn(1.0, 96.0)
        return 0.048 * (20.0 / h) * 2.0.pow((20.0 - t) / 10.0)
    }

    fun multiplier(type: YeastType): Double = when (type) {
        YeastType.IDY -> 1.0
        YeastType.ADY -> 1.25
        YeastType.FRESH -> 3.0
    }

    fun percent(type: YeastType, tempC: Double, hours: Double): Double =
        idyPercent(tempC, hours) * multiplier(type)
}

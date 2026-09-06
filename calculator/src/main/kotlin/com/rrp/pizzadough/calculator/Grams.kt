package com.rrp.pizzadough.calculator

import kotlin.math.round

object Grams {
    fun mass(grams: Double): Double = round(grams)
    fun yeast(grams: Double): Double = round(grams * 1000.0) / 1000.0
}

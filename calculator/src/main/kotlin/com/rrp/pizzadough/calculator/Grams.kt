package com.rrp.pizzadough.calculator

import kotlin.math.round

object Grams {
    fun mass(grams: Double): Double = round(grams)
    fun yeast(grams: Double): Double = round(grams * 100.0) / 100.0
}

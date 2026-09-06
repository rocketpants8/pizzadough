package com.rrp.pizzadough.calculator

/**
 * Instant dry yeast baker's % from PizzaBlab's quan-calc tables (TXCraig1),
 * bilinearly interpolated for temperatures/hours between their dropdowns.
 */
object YeastPredictor {
    fun idyPercent(tempC: Double, hours: Double): Double {
        val t = tempC.coerceIn(1.0, 40.0)
        val h = hours.coerceIn(1.0, 96.0)
        return if (t < 14.0) {
            interpolate(YeastTables.fridgeTempsC, YeastTables.fridgeHours, YeastTables.fridgeIdy, t, h)
        } else {
            interpolate(YeastTables.roomTempsC, YeastTables.roomHours, YeastTables.roomIdy, t, h)
        }
    }

    fun multiplier(type: YeastType): Double = when (type) {
        YeastType.IDY -> 1.0
        YeastType.ADY -> 1.33
        YeastType.FRESH -> 3.125
    }

    fun percent(type: YeastType, tempC: Double, hours: Double): Double =
        idyPercent(tempC, hours) * multiplier(type)

    internal fun interpolate(
        temps: DoubleArray,
        hoursAxis: DoubleArray,
        grid: Array<DoubleArray>,
        tempC: Double,
        hours: Double,
    ): Double {
        val t = tempC.coerceIn(temps.first(), temps.last())
        val h = hours.coerceIn(hoursAxis.first(), hoursAxis.last())
        val ti = indexAtOrBefore(temps, t)
        val hi = indexAtOrBefore(hoursAxis, h)
        val ti2 = (ti + 1).coerceAtMost(temps.lastIndex)
        val hi2 = (hi + 1).coerceAtMost(hoursAxis.lastIndex)

        fun cell(i: Int, j: Int): Double? = grid[i][j].takeUnless { it.isNaN() }

        val exact = cell(ti, hi)
        if (t == temps[ti] && h == hoursAxis[hi] && exact != null) return exact

        val t1 = temps[ti]
        val t2 = temps[ti2]
        val h1 = hoursAxis[hi]
        val h2 = hoursAxis[hi2]
        val q11 = cell(ti, hi)
        val q12 = cell(ti, hi2)
        val q21 = cell(ti2, hi)
        val q22 = cell(ti2, hi2)
        val valid = listOfNotNull(q11, q12, q21, q22)
        if (valid.isEmpty()) {
            return nearest(temps, hoursAxis, grid, t, h)
        }
        if (t1 == t2 && h1 == h2) return valid.first()
        if (t1 == t2) {
            val a = q11 ?: q21
            val b = q12 ?: q22
            return when {
                a != null && b != null && h1 != h2 -> lerp(h, h1, h2, a, b)
                else -> (a ?: b)!!
            }
        }
        if (h1 == h2) {
            val a = q11 ?: q12
            val b = q21 ?: q22
            return when {
                a != null && b != null -> lerp(t, t1, t2, a, b)
                else -> (a ?: b)!!
            }
        }
        if (q11 != null && q12 != null && q21 != null && q22 != null) {
            val r1 = lerp(h, h1, h2, q11, q12)
            val r2 = lerp(h, h1, h2, q21, q22)
            return lerp(t, t1, t2, r1, r2)
        }
        return nearest(temps, hoursAxis, grid, t, h)
    }

    private fun indexAtOrBefore(axis: DoubleArray, value: Double): Int {
        var i = 0
        while (i < axis.lastIndex && axis[i + 1] <= value) i++
        return i
    }

    private fun lerp(x: Double, x1: Double, x2: Double, y1: Double, y2: Double): Double {
        if (x1 == x2) return y1
        return y1 + (y2 - y1) * (x - x1) / (x2 - x1)
    }

    private fun nearest(
        temps: DoubleArray,
        hoursAxis: DoubleArray,
        grid: Array<DoubleArray>,
        tempC: Double,
        hours: Double,
    ): Double {
        var best = Double.NaN
        var bestDist = Double.POSITIVE_INFINITY
        for (i in temps.indices) {
            for (j in hoursAxis.indices) {
                val v = grid[i][j]
                if (v.isNaN()) continue
                val dist = kotlin.math.abs(temps[i] - tempC) + kotlin.math.abs(hoursAxis[j] - hours)
                if (dist < bestDist) {
                    bestDist = dist
                    best = v
                }
            }
        }
        return best
    }
}

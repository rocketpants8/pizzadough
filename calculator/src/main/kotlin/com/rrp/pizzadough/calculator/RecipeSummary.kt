package com.rrp.pizzadough.calculator

object RecipeSummary {
    fun format(recipe: Recipe): String {
        val hyd = "${recipe.hydrationPercent.stripZeros()}%"
        return if (!recipe.usePreferment) {
            "$hyd · ${recipe.finalTempC.stripZeros()}°C · ${recipe.finalHours.stripZeros()}h dough"
        } else if (recipe.prefermentTempC == recipe.finalTempC) {
            "$hyd · ${recipe.prefermentTempC.stripZeros()}°C · ${recipe.prefermentHours.stripZeros()}h biga / ${recipe.finalHours.stripZeros()}h dough"
        } else {
            "$hyd · ${recipe.prefermentHours.stripZeros()}h@${recipe.prefermentTempC.stripZeros()}°C biga / ${recipe.finalHours.stripZeros()}h@${recipe.finalTempC.stripZeros()}°C dough"
        }
    }

    private fun Double.stripZeros(): String {
        val asLong = toLong()
        return if (this == asLong.toDouble()) asLong.toString() else toString()
    }
}

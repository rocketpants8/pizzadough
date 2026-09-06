package com.rrp.pizzadough.calculator

object Defaults {
    val recipe = Recipe(
        ballCount = 6,
        ballWeightGrams = 250.0,
        hydrationPercent = 75.0,
        saltPercent = 2.8,
        yeastType = YeastType.IDY,
        usePreferment = true,
        prefermentFlourPercent = 20.0,
        prefermentHydrationPercent = 45.0,
        prefermentHours = 20.0,
        prefermentTempC = 24.0,
        finalHours = 4.0,
        finalTempC = 24.0,
        yeastOverridePercent = null,
        prefermentYeastOverridePercent = null,
        recipeExpanded = false,
    )
}

package com.rrp.pizzadough.calculator

data class Recipe(
    val ballCount: Int,
    val ballWeightGrams: Double,
    val hydrationPercent: Double,
    val saltPercent: Double,
    val yeastType: YeastType,
    val usePreferment: Boolean,
    val prefermentType: PrefermentType,
    val prefermentFlourPercent: Double,
    val prefermentHydrationPercent: Double,
    val prefermentHours: Double,
    val prefermentTempC: Double,
    val finalHours: Double,
    val finalTempC: Double,
    val yeastOverridePercent: Double?,
    val prefermentYeastOverridePercent: Double?,
    val recipeExpanded: Boolean,
) {
    fun withPrefermentType(type: PrefermentType): Recipe =
        if (type == prefermentType) {
            this
        } else {
            copy(prefermentType = type, prefermentHydrationPercent = type.standardHydrationPercent)
        }
}

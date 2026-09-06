package com.rrp.pizzadough.calculator

data class IngredientLine(
    val name: String,
    val grams: Double,
    val isYeast: Boolean = false,
)

data class DoughResult(
    val preferment: List<IngredientLine>,
    val finalDough: List<IngredientLine>,
    val totals: List<IngredientLine>,
    val totalDoughGrams: Double,
    val singleBallGrams: Double,
    val autoYeastPercent: Double,
    val autoPrefermentYeastPercent: Double,
    val extraYeastGrams: Double,
)

object DoughCalculator {
    fun calculate(recipe: Recipe): DoughResult {
        val totalDough = recipe.ballCount * recipe.ballWeightGrams
        val autoPref = YeastPredictor.percent(
            recipe.yeastType, recipe.prefermentTempC, recipe.prefermentHours,
        )
        val autoFinal = YeastPredictor.percent(
            recipe.yeastType, recipe.finalTempC, recipe.finalHours,
        )
        val prefYeastPct = recipe.prefermentYeastOverridePercent ?: autoPref
        val finalPct = recipe.yeastOverridePercent ?: autoFinal
        val prefFlourPct = if (recipe.usePreferment) recipe.prefermentFlourPercent else 0.0
        val yeastFraction = maxOf(
            if (recipe.usePreferment) prefYeastPct / 100.0 * prefFlourPct / 100.0 else 0.0,
            finalPct / 100.0,
        )
        val flour = totalDough / (1.0 + recipe.hydrationPercent / 100.0 +
            recipe.saltPercent / 100.0 + yeastFraction)
        val water = flour * recipe.hydrationPercent / 100.0
        val salt = flour * recipe.saltPercent / 100.0
        val prefFlour = flour * prefFlourPct / 100.0
        val prefWater = prefFlour * recipe.prefermentHydrationPercent / 100.0
        val prefYeast = if (recipe.usePreferment) prefFlour * prefYeastPct / 100.0 else 0.0
        val totalYeast = flour * yeastFraction
        val extraYeast = maxOf(0.0, totalYeast - prefYeast)
        val remainingFlour = maxOf(0.0, flour - prefFlour)
        val remainingWater = maxOf(0.0, water - prefWater)

        val preferment = if (recipe.usePreferment) listOf(
            IngredientLine("Flour", prefFlour),
            IngredientLine("Water", prefWater),
            IngredientLine("Yeast", prefYeast, isYeast = true),
        ) else emptyList()

        val flourName = if (recipe.usePreferment) "Flour (remaining)" else "Flour"
        val waterName = if (recipe.usePreferment) "Water (remaining)" else "Water"
        val finalDough = listOf(
            IngredientLine(flourName, remainingFlour),
            IngredientLine(waterName, remainingWater),
            IngredientLine("Salt", salt),
            IngredientLine("Yeast", extraYeast, isYeast = true),
        )
        val totals = listOf(
            IngredientLine("Flour", flour),
            IngredientLine("Water", water),
            IngredientLine("Yeast", totalYeast, isYeast = true),
            IngredientLine("Total dough", totalDough),
            IngredientLine("Single ball", recipe.ballWeightGrams),
        )
        return DoughResult(
            preferment = preferment,
            finalDough = finalDough,
            totals = totals,
            totalDoughGrams = totalDough,
            singleBallGrams = recipe.ballWeightGrams,
            autoYeastPercent = autoFinal,
            autoPrefermentYeastPercent = autoPref,
            extraYeastGrams = extraYeast,
        )
    }
}

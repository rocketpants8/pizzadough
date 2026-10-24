package com.rrp.pizzadough.calculator

data class FieldError(val field: String, val message: String)

object RecipeValidator {
    fun validate(recipe: Recipe): List<FieldError> = buildList {
        fun gt0(field: String, value: Double, label: String) {
            if (value <= 0.0 || value.isNaN()) add(FieldError(field, "$label must be greater than 0"))
        }
        if (recipe.ballCount < 1) add(FieldError("ballCount", "Need at least 1 dough ball"))
        gt0("ballWeightGrams", recipe.ballWeightGrams, "Ball weight")
        gt0("hydrationPercent", recipe.hydrationPercent, "Hydration")
        gt0("saltPercent", recipe.saltPercent, "Salt")
        gt0("finalHours", recipe.finalHours, "Final dough hours")
        if (recipe.finalTempC !in 1.0..40.0) {
            add(FieldError("finalTempC", "Temperature must be between 1 and 40°C"))
        }
        if (recipe.usePreferment) {
            val pref = recipe.prefermentType.label
            if (recipe.prefermentFlourPercent <= 0.0 || recipe.prefermentFlourPercent > 100.0) {
                add(FieldError("prefermentFlourPercent", "$pref flour must be greater than 0% and at most 100% of total flour"))
            }
            gt0("prefermentHydrationPercent", recipe.prefermentHydrationPercent, "$pref hydration")
            gt0("prefermentHours", recipe.prefermentHours, "$pref hours")
            if (recipe.prefermentTempC !in 1.0..40.0) {
                add(FieldError("prefermentTempC", "Temperature must be between 1 and 40°C"))
            }
            val prefWaterVsDough =
                recipe.prefermentFlourPercent / 100.0 * recipe.prefermentHydrationPercent
            if (prefWaterVsDough > recipe.hydrationPercent) {
                add(
                    FieldError(
                        "prefermentHydrationPercent",
                        "$pref uses more water than the dough hydration allows",
                    ),
                )
            }
        }
        recipe.yeastOverridePercent?.let {
            if (it < 0.0 || it.isNaN()) add(FieldError("yeastOverridePercent", "Yeast % cannot be negative"))
        }
        recipe.prefermentYeastOverridePercent?.let {
            if (it < 0.0 || it.isNaN()) {
                add(FieldError("prefermentYeastOverridePercent", "Yeast % cannot be negative"))
            }
        }
    }
}

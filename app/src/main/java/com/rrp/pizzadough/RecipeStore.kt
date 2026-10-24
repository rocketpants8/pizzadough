package com.rrp.pizzadough

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rrp.pizzadough.calculator.Defaults
import com.rrp.pizzadough.calculator.PrefermentType
import com.rrp.pizzadough.calculator.Recipe
import com.rrp.pizzadough.calculator.RecipeValidator
import com.rrp.pizzadough.calculator.YeastType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recipeDataStore by preferencesDataStore(name = "recipe")

class RecipeStore(context: Context) {
    private val dataStore = context.applicationContext.recipeDataStore

    val recipes: Flow<Recipe> = dataStore.data.map { preferences ->
        val defaults = Defaults.recipe
        val recipe = Recipe(
            ballCount = preferences[Keys.ballCount] ?: defaults.ballCount,
            ballWeightGrams = preferences[Keys.ballWeightGrams] ?: defaults.ballWeightGrams,
            hydrationPercent = preferences[Keys.hydrationPercent] ?: defaults.hydrationPercent,
            saltPercent = preferences[Keys.saltPercent] ?: defaults.saltPercent,
            yeastType = preferences[Keys.yeastType]
                ?.let { stored -> YeastType.entries.find { it.name == stored } }
                ?: defaults.yeastType,
            usePreferment = preferences[Keys.usePreferment] ?: defaults.usePreferment,
            prefermentType = preferences[Keys.prefermentType]
                ?.let { stored -> PrefermentType.entries.find { it.name == stored } }
                ?: defaults.prefermentType,
            prefermentFlourPercent = preferences[Keys.prefermentFlourPercent]
                ?: defaults.prefermentFlourPercent,
            prefermentHydrationPercent = preferences[Keys.prefermentHydrationPercent]
                ?: defaults.prefermentHydrationPercent,
            prefermentHours = preferences[Keys.prefermentHours] ?: defaults.prefermentHours,
            prefermentTempC = preferences[Keys.prefermentTempC] ?: defaults.prefermentTempC,
            finalHours = preferences[Keys.finalHours] ?: defaults.finalHours,
            finalTempC = preferences[Keys.finalTempC] ?: defaults.finalTempC,
            yeastOverridePercent = preferences[Keys.yeastOverridePercent]
                ?.takeUnless(Double::isNaN),
            prefermentYeastOverridePercent = preferences[Keys.prefermentYeastOverridePercent]
                ?.takeUnless(Double::isNaN),
            recipeExpanded = preferences[Keys.recipeExpanded] ?: defaults.recipeExpanded,
        )

        recipe.takeIf { RecipeValidator.validate(it).isEmpty() } ?: defaults
    }

    suspend fun save(recipe: Recipe) {
        if (RecipeValidator.validate(recipe).isNotEmpty()) return

        dataStore.edit { preferences ->
            preferences[Keys.ballCount] = recipe.ballCount
            preferences[Keys.ballWeightGrams] = recipe.ballWeightGrams
            preferences[Keys.hydrationPercent] = recipe.hydrationPercent
            preferences[Keys.saltPercent] = recipe.saltPercent
            preferences[Keys.yeastType] = recipe.yeastType.name
            preferences[Keys.usePreferment] = recipe.usePreferment
            preferences[Keys.prefermentType] = recipe.prefermentType.name
            preferences[Keys.prefermentFlourPercent] = recipe.prefermentFlourPercent
            preferences[Keys.prefermentHydrationPercent] = recipe.prefermentHydrationPercent
            preferences[Keys.prefermentHours] = recipe.prefermentHours
            preferences[Keys.prefermentTempC] = recipe.prefermentTempC
            preferences[Keys.finalHours] = recipe.finalHours
            preferences[Keys.finalTempC] = recipe.finalTempC
            preferences[Keys.yeastOverridePercent] =
                recipe.yeastOverridePercent ?: Double.NaN
            preferences[Keys.prefermentYeastOverridePercent] =
                recipe.prefermentYeastOverridePercent ?: Double.NaN
            preferences[Keys.recipeExpanded] = recipe.recipeExpanded
        }
    }

    private object Keys {
        val ballCount = intPreferencesKey("ballCount")
        val ballWeightGrams = doublePreferencesKey("ballWeightGrams")
        val hydrationPercent = doublePreferencesKey("hydrationPercent")
        val saltPercent = doublePreferencesKey("saltPercent")
        val yeastType = stringPreferencesKey("yeastType")
        val usePreferment = booleanPreferencesKey("usePreferment")
        val prefermentType = stringPreferencesKey("prefermentType")
        val prefermentFlourPercent = doublePreferencesKey("prefermentFlourPercent")
        val prefermentHydrationPercent = doublePreferencesKey("prefermentHydrationPercent")
        val prefermentHours = doublePreferencesKey("prefermentHours")
        val prefermentTempC = doublePreferencesKey("prefermentTempC")
        val finalHours = doublePreferencesKey("finalHours")
        val finalTempC = doublePreferencesKey("finalTempC")
        val yeastOverridePercent = doublePreferencesKey("yeastOverridePercent")
        val prefermentYeastOverridePercent =
            doublePreferencesKey("prefermentYeastOverridePercent")
        val recipeExpanded = booleanPreferencesKey("recipeExpanded")
    }
}

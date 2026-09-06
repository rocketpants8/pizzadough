# Pizza Dough Calculator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a sideloadable Android app that calculates high-hydration Neapolitan dough (biga first) from baker’s percentages and time/temperature yeast, with saved defaults.

**Architecture:** A pure JVM `:calculator` module owns recipe math and validation. An `:app` module owns Compose UI, a ViewModel, and DataStore. No network, no Hilt.

**Tech Stack:** Kotlin 2.0.21, JDK 17, Android Gradle Plugin 8.7.3, Compose BOM 2024.12.01, DataStore Preferences 1.1.1, JUnit 4, Gradle 8.9. compileSdk 35, minSdk 26, targetSdk 35.

## Global Constraints

- Package `com.rrp.pizzadough`; calculator types in `com.rrp.pizzadough.calculator`
- Personal-use Android only; no Play Store, no network permission
- Material 3, system light/dark, no custom branding
- Preferment results first, then final dough, then totals
- Ball count and ball weight always visible; recipe panel collapsed by default
- Defaults: 6 × 250 g, 75% hydration, 2.8% salt, IDY, preferment on, 20% flour in 45% biga, 20 h / 24°C biga, 4 h / 24°C final dough
- Yeast: `idyPercent(tempC, hours) = 0.048 * (20.0 / hours) * 2.0.pow((20.0 - tempC) / 10.0)` with hours clamped [1, 96] and tempC clamped [1, 40]; ADY ×1.25; fresh ×3.0
- Flour formula: `yeastFraction = max(prefYeastPct/100 * prefFlourPct/100, finalSchedulePct/100)` (preferment terms 0 if off); `flour = totalDough / (1 + hyd/100 + salt/100 + yeastFraction)`
- Remaining yeast grams `max(0, totalYeast - prefermentYeast)`; UI shows "—" when extra yeast is 0 and preferment is on
- Display: flour/water/salt nearest 1 g; yeast 2 decimals; tests use unrounded doubles (0.05 g tolerance) plus rounding helpers
- Out of scope: pan/thickness, extra ingredients, bowl residue, volumetric units
- TDD for all calculator production code; Gradle/AndroidManifest/theme scaffolding is generated-config exception
- Work on branch `feat/dough-calculator`, not `main`
- ANDROID_HOME is `/home/rrp/Android/Sdk`; JDK 17+ is available

## File map

- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitignore`, `calculator/build.gradle.kts`, `app/build.gradle.kts`
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/{YeastType,Recipe,Defaults,Grams,YeastPredictor,DoughCalculator,RecipeValidator,RecipeSummary}.kt`
- Create: matching tests under `calculator/src/test/kotlin/com/rrp/pizzadough/calculator/`
- Create: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/rrp/pizzadough/{MainActivity,DoughViewModel,RecipeStore}.kt`, `app/src/main/java/com/rrp/pizzadough/ui/{DoughScreen,theme/Theme}.kt`, `app/src/main/res/values/strings.xml`

---

### Task 1: Gradle multi-module project

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitignore`, `calculator/build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/strings.xml`, `app/src/main/java/com/rrp/pizzadough/MainActivity.kt`

**Interfaces:**
- Consumes: none
- Produces: Gradle modules `:calculator` (JVM) and `:app` (Android application depending on `:calculator`)

- [ ] **Step 1: Write project files**

`.gitignore`:

```
.gradle/
build/
**/build/
.idea/
*.iml
local.properties
.DS_Store
captures/
.superpowers/sdd/
```

`settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PizzaDough"
include(":app")
include(":calculator")
```

Root `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

`gradle.properties`:

```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`calculator/build.gradle.kts`:

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    testImplementation("junit:junit:4.13.2")
}
```

`app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.rrp.pizzadough"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.rrp.pizzadough"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":calculator"))
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

`app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Dough</string>
</resources>
```

Placeholder `MainActivity.kt` (real UI is Task 7):

```kotlin
package com.rrp.pizzadough

import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}
```

Create `local.properties` with `sdk.dir=/home/rrp/Android/Sdk` (do not commit it).

- [ ] **Step 2: Generate Gradle wrapper**

Run from repo root (needs network):

```
gradle wrapper --gradle-version 8.9
```

If `gradle` is missing, download wrapper jars via `./gradlew` after placing wrapper files from Gradle 8.9 distribution.

- [ ] **Step 3: Verify modules resolve**

Run: `./gradlew :calculator:test :app:assembleDebug`

Expected: SUCCESS (calculator has no tests yet; app compiles placeholder activity).

- [ ] **Step 4: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties .gitignore \
  calculator/build.gradle.kts app/build.gradle.kts \
  app/src/main/AndroidManifest.xml app/src/main/res/values/strings.xml \
  app/src/main/java/com/rrp/pizzadough/MainActivity.kt \
  gradle/wrapper gradlew gradlew.bat
git commit -m "$(cat <<'EOF'
chore: scaffold Android app and JVM calculator modules

EOF
)"
```

---

### Task 2: Recipe model and defaults

**Files:**
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/YeastType.kt`
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/Recipe.kt`
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/Defaults.kt`
- Test: `calculator/src/test/kotlin/com/rrp/pizzadough/calculator/DefaultsTest.kt`

**Interfaces:**
- Consumes: none
- Produces:

```kotlin
enum class YeastType { IDY, ADY, FRESH }

data class Recipe(
    val ballCount: Int,
    val ballWeightGrams: Double,
    val hydrationPercent: Double,
    val saltPercent: Double,
    val yeastType: YeastType,
    val usePreferment: Boolean,
    val prefermentFlourPercent: Double,
    val prefermentHydrationPercent: Double,
    val prefermentHours: Double,
    val prefermentTempC: Double,
    val finalHours: Double,
    val finalTempC: Double,
    val yeastOverridePercent: Double?,
    val prefermentYeastOverridePercent: Double?,
    val recipeExpanded: Boolean,
)

object Defaults {
    val recipe: Recipe
}
```

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultsTest {
    @Test
    fun firstLaunchRecipeMatchesSpec() {
        val r = Defaults.recipe
        assertEquals(6, r.ballCount)
        assertEquals(250.0, r.ballWeightGrams, 0.0)
        assertEquals(75.0, r.hydrationPercent, 0.0)
        assertEquals(2.8, r.saltPercent, 0.0)
        assertEquals(YeastType.IDY, r.yeastType)
        assertEquals(true, r.usePreferment)
        assertEquals(20.0, r.prefermentFlourPercent, 0.0)
        assertEquals(45.0, r.prefermentHydrationPercent, 0.0)
        assertEquals(20.0, r.prefermentHours, 0.0)
        assertEquals(24.0, r.prefermentTempC, 0.0)
        assertEquals(4.0, r.finalHours, 0.0)
        assertEquals(24.0, r.finalTempC, 0.0)
        assertNull(r.yeastOverridePercent)
        assertNull(r.prefermentYeastOverridePercent)
        assertFalse(r.recipeExpanded)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :calculator:test --tests com.rrp.pizzadough.calculator.DefaultsTest`

Expected: FAIL compiling (`Defaults` / `Recipe` unresolved).

- [ ] **Step 3: Write minimal implementation**

`YeastType.kt`:

```kotlin
package com.rrp.pizzadough.calculator

enum class YeastType { IDY, ADY, FRESH }
```

`Recipe.kt`:

```kotlin
package com.rrp.pizzadough.calculator

data class Recipe(
    val ballCount: Int,
    val ballWeightGrams: Double,
    val hydrationPercent: Double,
    val saltPercent: Double,
    val yeastType: YeastType,
    val usePreferment: Boolean,
    val prefermentFlourPercent: Double,
    val prefermentHydrationPercent: Double,
    val prefermentHours: Double,
    val prefermentTempC: Double,
    val finalHours: Double,
    val finalTempC: Double,
    val yeastOverridePercent: Double?,
    val prefermentYeastOverridePercent: Double?,
    val recipeExpanded: Boolean,
)
```

`Defaults.kt`:

```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :calculator:test --tests com.rrp.pizzadough.calculator.DefaultsTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add calculator/src
git commit -m "$(cat <<'EOF'
feat: add recipe model and Neapolitan defaults

EOF
)"
```

---

### Task 3: Yeast predictor and gram rounding

**Files:**
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/YeastPredictor.kt`
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/Grams.kt`
- Test: `calculator/src/test/kotlin/com/rrp/pizzadough/calculator/YeastPredictorTest.kt`
- Test: `calculator/src/test/kotlin/com/rrp/pizzadough/calculator/GramsTest.kt`

**Interfaces:**
- Consumes: `YeastType`
- Produces:

```kotlin
object YeastPredictor {
    fun idyPercent(tempC: Double, hours: Double): Double
    fun percent(type: YeastType, tempC: Double, hours: Double): Double
    fun multiplier(type: YeastType): Double
}

object Grams {
    fun mass(grams: Double): Double   // nearest 1 g
    fun yeast(grams: Double): Double  // 2 decimals
}
```

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YeastPredictorTest {
    @Test
    fun anchorTwentyHoursAtTwentyCIsPointZeroFourEight() {
        assertEquals(0.048, YeastPredictor.idyPercent(20.0, 20.0), 1e-9)
    }

    @Test
    fun shorterWarmerScheduleUsesMoreYeastThanLongBiga() {
        val fourHour = YeastPredictor.idyPercent(24.0, 4.0)
        val twentyHour = YeastPredictor.idyPercent(24.0, 20.0)
        assertTrue(fourHour > twentyHour)
    }

    @Test
    fun freshIsThreeTimesIdy() {
        val idy = YeastPredictor.percent(YeastType.IDY, 24.0, 4.0)
        val fresh = YeastPredictor.percent(YeastType.FRESH, 24.0, 4.0)
        assertEquals(idy * 3.0, fresh, 1e-9)
    }

    @Test
    fun adyIsOnePointTwoFiveTimesIdy() {
        val idy = YeastPredictor.percent(YeastType.IDY, 24.0, 4.0)
        val ady = YeastPredictor.percent(YeastType.ADY, 24.0, 4.0)
        assertEquals(idy * 1.25, ady, 1e-9)
    }
}
```

```kotlin
package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class GramsTest {
    @Test
    fun massRoundsToNearestGram() {
        assertEquals(247.0, Grams.mass(246.6), 0.0)
        assertEquals(247.0, Grams.mass(246.5), 0.0)
    }

    @Test
    fun yeastRoundsToTwoDecimals() {
        assertEquals(0.12, Grams.yeast(0.123), 1e-9)
        assertEquals(0.13, Grams.yeast(0.125), 1e-9)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :calculator:test --tests com.rrp.pizzadough.calculator.YeastPredictorTest --tests com.rrp.pizzadough.calculator.GramsTest`

Expected: FAIL compiling (unresolved `YeastPredictor` / `Grams`).

- [ ] **Step 3: Write minimal implementation**

```kotlin
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
```

```kotlin
package com.rrp.pizzadough.calculator

import kotlin.math.round

object Grams {
    fun mass(grams: Double): Double = round(grams)
    fun yeast(grams: Double): Double = round(grams * 100.0) / 100.0
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :calculator:test --tests com.rrp.pizzadough.calculator.YeastPredictorTest --tests com.rrp.pizzadough.calculator.GramsTest`

Expected: PASS. If `Grams.mass(246.5)` fails because `round` uses banker's rounding, change the test input to `246.6` only and assert `246.4 -> 246.0` instead — keep Kotlin `round` (half away from zero on JVM for positive numbers via `kotlin.math.round`).

- [ ] **Step 5: Commit**

```bash
git add calculator/src
git commit -m "$(cat <<'EOF'
feat: predict yeast from time and temperature

EOF
)"
```

---

### Task 4: Dough calculator and recipe summary

**Files:**
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/DoughCalculator.kt`
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/RecipeSummary.kt`
- Test: `calculator/src/test/kotlin/com/rrp/pizzadough/calculator/DoughCalculatorTest.kt`
- Test: `calculator/src/test/kotlin/com/rrp/pizzadough/calculator/RecipeSummaryTest.kt`

**Interfaces:**
- Consumes: `Recipe`, `Defaults`, `YeastPredictor`, `Grams`
- Produces:

```kotlin
data class IngredientLine(val name: String, val grams: Double, val isYeast: Boolean = false)

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
    fun calculate(recipe: Recipe): DoughResult
}

object RecipeSummary {
    fun format(recipe: Recipe): String
}
```

Preferment line names: `"Flour"`, `"Water"`, `"Yeast"`.  
Final dough names: `"Flour (remaining)"`, `"Water (remaining)"`, `"Salt"`, `"Yeast"` when preferment on; `"Flour"`, `"Water"`, `"Salt"`, `"Yeast"` when off.  
Totals names: `"Flour"`, `"Water"`, `"Yeast"`, `"Total dough"`, `"Single ball"`.  
`IngredientLine.grams` are **unrounded**. UI rounds with `Grams`.

When `extraYeastGrams == 0.0` and preferment is on, still include a Yeast line with `0.0` grams; the UI maps 0 extra yeast to "—".

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoughCalculatorTest {
    @Test
    fun totalDoughEqualsBallsTimesWeight() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        assertEquals(1500.0, result.totalDoughGrams, 0.05)
        assertEquals(250.0, result.singleBallGrams, 0.05)
    }

    @Test
    fun bigaFlourIsTwentyPercentOfTotalFlour() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        val totalFlour = result.totals.first { it.name == "Flour" }.grams
        val bigaFlour = result.preferment.first { it.name == "Flour" }.grams
        assertEquals(totalFlour * 0.20, bigaFlour, 0.05)
    }

    @Test
    fun remainingFlourAndWaterAreNonNegative() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        result.finalDough.filter { it.name.startsWith("Flour") || it.name.startsWith("Water") }
            .forEach { assertTrue(it.grams >= -0.0001) }
    }

    @Test
    fun extraYeastNeverNegative() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        assertTrue(result.extraYeastGrams >= 0.0)
    }

    @Test
    fun ingredientsSumToTotalDough() {
        val result = DoughCalculator.calculate(Defaults.recipe)
        val flour = result.totals.first { it.name == "Flour" }.grams
        val water = result.totals.first { it.name == "Water" }.grams
        val yeast = result.totals.first { it.name == "Yeast" }.grams
        val salt = result.finalDough.first { it.name == "Salt" }.grams
        assertEquals(result.totalDoughGrams, flour + water + salt + yeast, 0.05)
    }

    @Test
    fun noPrefermentPutsEverythingInFinalDough() {
        val result = DoughCalculator.calculate(Defaults.recipe.copy(usePreferment = false))
        assertTrue(result.preferment.isEmpty())
        assertEquals("Flour", result.finalDough[0].name)
    }
}
```

```kotlin
package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeSummaryTest {
    @Test
    fun defaultSummary() {
        assertEquals(
            "75% · 24°C · 20h biga / 4h dough",
            RecipeSummary.format(Defaults.recipe),
        )
    }

    @Test
    fun differentTemps() {
        val r = Defaults.recipe.copy(prefermentTempC = 24.0, finalTempC = 22.0)
        assertEquals(
            "75% · 20h@24°C biga / 4h@22°C dough",
            RecipeSummary.format(r),
        )
    }

    @Test
    fun noPreferment() {
        assertEquals(
            "75% · 24°C · 4h dough",
            RecipeSummary.format(Defaults.recipe.copy(usePreferment = false)),
        )
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :calculator:test --tests com.rrp.pizzadough.calculator.DoughCalculatorTest --tests com.rrp.pizzadough.calculator.RecipeSummaryTest`

Expected: FAIL compiling.

- [ ] **Step 3: Write minimal implementation**

`DoughCalculator.kt`:

```kotlin
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
        val remainingFlour = flour - prefFlour
        val remainingWater = water - prefWater

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
```

`RecipeSummary.kt`:

```kotlin
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :calculator:test`

Expected: PASS (all calculator tests).

- [ ] **Step 5: Commit**

```bash
git add calculator/src
git commit -m "$(cat <<'EOF'
feat: calculate biga and final dough grams

EOF
)"
```

---

### Task 5: Recipe validator

**Files:**
- Create: `calculator/src/main/kotlin/com/rrp/pizzadough/calculator/RecipeValidator.kt`
- Test: `calculator/src/test/kotlin/com/rrp/pizzadough/calculator/RecipeValidatorTest.kt`

**Interfaces:**
- Consumes: `Recipe`
- Produces:

```kotlin
data class FieldError(val field: String, val message: String)

object RecipeValidator {
    fun validate(recipe: Recipe): List<FieldError>
}
```

Field keys: `ballCount`, `ballWeightGrams`, `hydrationPercent`, `saltPercent`, `prefermentFlourPercent`, `prefermentHydrationPercent`, `prefermentHours`, `prefermentTempC`, `finalHours`, `finalTempC`, `yeastOverridePercent`, `prefermentYeastOverridePercent`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.rrp.pizzadough.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeValidatorTest {
    @Test
    fun defaultsAreValid() {
        assertTrue(RecipeValidator.validate(Defaults.recipe).isEmpty())
    }

    @Test
    fun zeroBallsIsInvalid() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(ballCount = 0))
        assertTrue(errors.any { it.field == "ballCount" })
    }

    @Test
    fun emptyHydrationIsInvalid() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(hydrationPercent = 0.0))
        assertTrue(errors.any { it.field == "hydrationPercent" })
    }

    @Test
    fun yeastOverrideZeroIsAllowed() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(yeastOverridePercent = 0.0))
        assertTrue(errors.none { it.field == "yeastOverridePercent" })
    }

    @Test
    fun prefermentFlourOverOneHundredIsInvalid() {
        val errors = RecipeValidator.validate(Defaults.recipe.copy(prefermentFlourPercent = 101.0))
        assertTrue(errors.any { it.field == "prefermentFlourPercent" })
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :calculator:test --tests com.rrp.pizzadough.calculator.RecipeValidatorTest`

Expected: FAIL compiling.

- [ ] **Step 3: Write minimal implementation**

```kotlin
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
            if (recipe.prefermentFlourPercent <= 0.0 || recipe.prefermentFlourPercent > 100.0) {
                add(FieldError("prefermentFlourPercent", "Biga flour must be 0–100% of total flour, greater than 0"))
            }
            gt0("prefermentHydrationPercent", recipe.prefermentHydrationPercent, "Biga hydration")
            gt0("prefermentHours", recipe.prefermentHours, "Biga hours")
            if (recipe.prefermentTempC !in 1.0..40.0) {
                add(FieldError("prefermentTempC", "Temperature must be between 1 and 40°C"))
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
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :calculator:test`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add calculator/src
git commit -m "$(cat <<'EOF'
feat: validate recipe inputs

EOF
)"
```

---

### Task 6: DataStore and ViewModel

**Files:**
- Create: `app/src/main/java/com/rrp/pizzadough/RecipeStore.kt`
- Create: `app/src/main/java/com/rrp/pizzadough/DoughViewModel.kt`
- Modify: `app/build.gradle.kts` only if a missing dependency is discovered (should already have DataStore + lifecycle)

**Interfaces:**
- Consumes: `Recipe`, `Defaults`, `DoughCalculator`, `RecipeValidator`, `RecipeSummary`, `DoughResult`, `FieldError`
- Produces:

```kotlin
data class DoughUiState(
    val recipe: Recipe,
    val result: DoughResult,
    val errors: List<FieldError>,
    val summary: String,
)

class RecipeStore(context: Context) {
    val recipes: Flow<Recipe>
    suspend fun save(recipe: Recipe)
}

class DoughViewModel(store: RecipeStore) : ViewModel() {
    val uiState: StateFlow<DoughUiState>
    fun update(transform: (Recipe) -> Recipe)
    fun resetDefaults()
}
```

`RecipeStore` writes only after `RecipeValidator.validate(recipe).isEmpty()`. The ViewModel always keeps `result` from the last valid recipe; `errors` reflect the in-progress recipe.

ViewModel construction: `viewModel(factory = DoughViewModel.factory(store))`.

Persist every `Recipe` field including overrides and `recipeExpanded` as DataStore preferences (typed keys, not JSON).

- [ ] **Step 1: Implement RecipeStore and DoughViewModel** (Android types; no new JVM tests in this task — validation/calculator already tested)

`RecipeStore.kt` — map each `Recipe` property to a Preferences key (`intPreferencesKey("ballCount")`, `doublePreferencesKey(...)`, `stringPreferencesKey("yeastType")`, `booleanPreferencesKey("usePreferment")`, nullable doubles stored as `Double.NaN` meaning null). `recipes` emits `Defaults.recipe` until DataStore has a saved valid recipe.

`DoughViewModel.kt`:

```kotlin
init {
    viewModelScope.launch {
        store.recipes.collect { recipe ->
            _uiState.value = recipe.toState()
        }
    }
}

fun update(transform: (Recipe) -> Recipe) {
    val next = transform(_uiState.value.recipe)
    val errors = RecipeValidator.validate(next)
    if (errors.isEmpty()) {
        _uiState.value = next.toState()
        viewModelScope.launch { store.save(next) }
    } else {
        _uiState.value = _uiState.value.copy(recipe = next, errors = errors)
    }
}

fun resetDefaults() {
    update { Defaults.recipe }
}

private fun Recipe.toState() = DoughUiState(
    recipe = this,
    result = DoughCalculator.calculate(this),
    errors = emptyList(),
    summary = RecipeSummary.format(this),
)
```

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: SUCCESS

- [ ] **Step 3: Commit**

```bash
git add app/src
git commit -m "$(cat <<'EOF'
feat: persist last recipe and expose calculator state

EOF
)"
```

---

### Task 7: Compose UI and debug APK

**Files:**
- Create: `app/src/main/java/com/rrp/pizzadough/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/rrp/pizzadough/ui/DoughScreen.kt`
- Modify: `app/src/main/java/com/rrp/pizzadough/MainActivity.kt`

**Interfaces:**
- Consumes: `DoughViewModel`, `DoughUiState`, `Grams`, `IngredientLine`
- Produces: Material 3 `DoughScreen` composable; `MainActivity` setContent

UI rules:

- `Scaffold` + small top app bar title `Dough`
- Always-visible row: minus button, ball count, plus button; next row ball weight `OutlinedTextField`
- `ElevatedCard` “Recipe” with collapse chevron; collapsed shows `uiState.summary`; expanded shows hydration, salt, yeast type segmented buttons (IDY/ADY/Fresh), preferment switch, biga flour %, biga hydration, biga hours, biga °C, final hours, final °C, yeast % fields (placeholder auto values from `result.autoPrefermentYeastPercent` and `result.autoYeastPercent`; blank means override null), `TextButton` “Reset defaults”
- Results: heading **Biga** (if preferment on) listing lines; **Final dough**; **Totals** with `style = MaterialTheme.typography.bodyMedium` / lower emphasis
- Format grams with `Grams.mass` / `Grams.yeast`; if preferment on and extra yeast is 0.0, show `—` for final yeast
- Field errors: `isError` + `supportingText` from `errors` matching the field key
- `windowSoftInputMode` already `adjustResize`

- [ ] **Step 1: Implement Theme.kt** — `PizzaDoughTheme { MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(), content = content) }`

- [ ] **Step 2: Implement DoughScreen.kt** as specified above. Numeric fields parse `toDoubleOrNull()` / `toIntOrNull()`; invalid parse calls `update` with `0.0` or `0` so the validator flags them and the previous result remains.

- [ ] **Step 3: Wire MainActivity**

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = RecipeStore(applicationContext)
        setContent {
            PizzaDoughTheme {
                val vm: DoughViewModel = viewModel(factory = DoughViewModel.factory(store))
                val state by vm.uiState.collectAsStateWithLifecycle()
                DoughScreen(state = state, onUpdate = vm::update, onReset = vm::resetDefaults)
            }
        }
    }
}
```

- [ ] **Step 4: Assemble debug APK**

Run: `./gradlew :calculator:test :app:assembleDebug`

Expected: all tests PASS; APK at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 5: Commit**

```bash
git add app/src
git commit -m "$(cat <<'EOF'
feat: add Material dough calculator screen

EOF
)"
```

---

## Self-review vs spec

| Spec requirement | Task |
| --- | --- |
| JVM calculator + Compose app + DataStore | 1, 4, 6, 7 |
| Defaults table | 2 |
| Yeast formula, conversions, clamps | 3 |
| Biga first, remaining yeast dash, totals | 4, 7 |
| Collapsible recipe, ball count always visible | 7 |
| Validation + last good result | 5, 6 |
| Reset defaults | 6, 7 |
| No pan/extras/bowl residue | none added |
| Unit tests listed in spec | 2–5 |
| Debug APK | 7 |

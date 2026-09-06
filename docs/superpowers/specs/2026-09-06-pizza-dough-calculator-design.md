# Pizza Dough Calculator — Design

**Date:** 2026-09-06  
**Status:** Approved  
**Platform:** Android (personal use, sideload APK)

## Goal

A native Android app that calculates pizza dough in baker’s percentages, including a biga preferment and yeast predicted from fermentation time and temperature. First-launch defaults match high-hydration Neapolitan the owner actually makes. Changing dough-ball count is the everyday action; other recipe settings persist until changed.

This is inspired by [PizzaBlab’s pizza dough calculator](https://www.pizzablab.com/calculators/pizza-dough-calculator/) (baker’s math, preferment split, time/temp yeast). It does **not** copy PizzaBlab’s website, scripts, or UI.

## Defaults (first launch and “Reset defaults”)

| Setting | Value |
| --- | --- |
| Dough balls | 6 |
| Ball weight | 250 g |
| Hydration | 75% |
| Salt | 2.8% |
| Yeast type | Instant dry yeast (IDY) |
| Preferment | On (biga) |
| Flour in preferment | 20% of total flour |
| Preferment hydration | 45% |
| Preferment time / temp | 20 h at 24°C |
| Final dough time / temp | 4 h at 24°C |
| Yeast % | Auto (from time/temp), overridable |
| Recipe panel | Collapsed after first visit (`recipeExpanded = false`) |

Temperature stays on the main recipe form (not buried) because Sydney kitchens often run ~24°C or warmer in summer.

## Out of scope

- Pan size / thickness factor
- Extra ingredients (sugar, oil, milk, gluten, malt, “other”)
- Bowl-residue compensation
- Teaspoon / tablespoon columns
- Accounts, network, Play Store
- iOS

## Architecture

Kotlin + Jetpack Compose, one screen, no accounts, no network.

```
UI (Compose) → ViewModel → calculator (pure JVM) + DataStore preferences
```

1. **`calculator` JVM module** — pure functions and data classes. Unit-tested on the JVM with no Android runtime.
2. **Preferences** — last-used `Recipe` in DataStore. Every valid change is saved. Recipe-panel expanded state is saved too.
3. **UI** — `ViewModel` holds inputs, runs the calculator on every valid change, exposes last valid `DoughResult`.

## Screen

Material 3, follow system light/dark, no custom branding.

**Always visible**

- App title
- Dough-ball count as a large minus / plus (integer ≥ 1)
- Ball weight in grams

**Collapsible “Recipe” panel** (collapsed by default after first visit)

When collapsed: one-line summary, e.g. `75% · 24°C · 20h biga / 4h dough`.

When expanded:

- Hydration %, salt %
- Yeast type (IDY / ADY / fresh) — IDY is the default; types are switchable
- Preferment on/off
- % of total flour in preferment, preferment hydration %
- Preferment hours and temperature °C
- Final-dough hours and temperature °C
- Yeast % for preferment and for the dough (auto-filled, editable override)
- “Reset defaults”

**Results (mix order)**

1. **Biga** — flour, water, yeast (grams). Hidden when preferment is off.
2. **Final dough** — remaining flour, remaining water, salt, remaining yeast (or an em dash if no extra yeast).
3. **Totals** — smaller, at the bottom: total flour, water, yeast, total dough, single ball. Total dough must equal balls × ball weight (before display rounding).

## Calculator math

### Totals

- `totalDoughGrams = ballCount * ballWeightGrams`
- Baker’s percentages: flour = 100%, water = hydration%, salt = salt%, yeast = yeast% of **total flour**
- `totalPercent = 100 + hydration + salt + yeast`
- `flour = totalDoughGrams / (totalPercent / 100)`
- `water = flour * hydration / 100`
- `salt = flour * saltPercent / 100`
- `yeastGrams = flour * yeastPercent / 100`

Yeast in the dough is `max(preferment yeast grams, final-schedule yeast grams)`, so a strong biga never forces negative final yeast and never shrinks total yeast below what the biga already contains.

Solve flour so flour + water + salt + yeast = total dough:

```
yeastFraction = max(
  prefermentYeastPercent/100 * prefermentFlourPercent/100,  // 0 if preferment off
  finalSchedulePercent/100
)
flour = totalDoughGrams / (1 + hydration/100 + salt/100 + yeastFraction)
```

### Preferment (biga)

When preferment is on:

- `prefermentFlour = flour * prefermentFlourPercent / 100`
- `prefermentWater = prefermentFlour * prefermentHydrationPercent / 100`
- Preferment yeast baker’s % is predicted from preferment hours + temp (or override)
- `prefermentYeastGrams = prefermentFlour * prefermentYeastPercent / 100`

### Final dough

- Remaining flour / water = totals minus preferment
- All salt is in the final dough
- Remaining yeast grams = `max(0, totalYeastGrams - prefermentYeastGrams)`
- If remaining yeast is 0, the UI shows "—" (no extra yeast), not 0.00

When preferment is off, the preferment block is omitted and all flour, water, salt, and yeast go in final dough.

Remaining flour and water are never negative. Preferment flour percent is clamped to (0, 100] when preferment is on.

### Yeast model

Auto yeast uses PizzaBlab’s TXCraig1 lookup tables (room-temp 14–30°C and cold 4–9°C), interpolated between their dropdown points. Exact table values include 20 h at 24°C → **0.016% IDY** and 4 h at 24°C → **0.176% IDY**.

- Warmer or longer → less yeast. Cooler or shorter → more yeast.
- `hours` is clamped to [1, 96], `tempC` to [1, 40] before evaluation.
- Below 14°C the cold-ferment (Lehmann) table is used.

Yeast type conversions from IDY (PizzaBlab):

| Type | Multiplier |
| --- | --- |
| Instant dry (IDY) | 1.0 |
| Active dry (ADY) | 1.33 |
| Fresh / cake | 3.125 |

**Preferment yeast %** = `idyPercent(prefermentTempC, prefermentHours) * multiplier` (or override), applied to preferment flour.

**Final-schedule yeast % of total flour** = `idyPercent(finalTempC, finalHours) * multiplier` (or override).  
`totalYeastGrams = flour * that percent / 100`.  
Then subtract preferment yeast grams as above.

Overrides replace the auto % for that stage only. Clearing the override returns to auto.

### Display rounding

- Flour, water, salt: nearest 1 g
- Yeast: 3 decimal grams
- Internals stay `Double`; rounding is display-only
- Tests assert unrounded math (tolerance 0.05 g) plus rounding helpers

## Persistence

DataStore serializes the full `Recipe` (including overrides, yeast type, panel expanded flag). Invalid keystrokes are **not** written. Last valid recipe remains on disk.

“Reset defaults” writes `Defaults.recipe` and recomputes.

## Validation

Keep showing the last valid `DoughResult`. Highlight the offending field.

| Field | Rule |
| --- | --- |
| Ball count | Integer ≥ 1 |
| Ball weight, hydration, salt, hours | > 0 |
| Preferment flour % (when on) | > 0 and ≤ 100 |
| Preferment hydration % (when on) | > 0 |
| Temperatures | ≥ 1 and ≤ 40 |
| Yeast override % | ≥ 0 (0 is allowed) |

Empty / unparsable values are invalid.

## Testing

JVM unit tests only for v1 (no instrumented UI tests):

- Total dough equals balls × weight
- Biga flour is the chosen % of total flour
- Remaining flour/water ≥ 0
- Extra yeast never negative
- Anchor yeast point: 20 h / 20°C IDY = 0.048%
- 4 h / 24°C IDY is greater than 20 h / 24°C IDY
- Rounding helpers
- Validator rejects 0 balls and empty hydration

## Delivery

Installable debug APK from this machine (`./gradlew :app:assembleDebug`). minSdk 26, compileSdk 35, Java 17.

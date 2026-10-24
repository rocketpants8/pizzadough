# Pizza Dough Calculator

A small native Android app for working out pizza dough in baker's percentages.

- Scale a recipe by number and weight of dough balls
- Set hydration and salt as a percentage of flour
- Optional **biga** or **poolish** preferment, with its own flour share and hydration
- Yeast amount predicted from fermentation time and temperature (IDY, ADY or fresh), with a manual override
- Recipe settings persist between launches; the dough-ball count is the everyday knob

Defaults are a high-hydration Neapolitan: 6 × 250 g balls, 75% hydration, 2.8% salt, 20% biga.

Inspired by [PizzaBlab's pizza dough calculator](https://www.pizzablab.com/calculators/pizza-dough-calculator/). This app doesn't use any of PizzaBlab's code or UI.

## Install

Download an APK from [Releases](../../releases) and sideload it (Android 8.0+). You may need to allow installs from unknown sources.

- `*-minifiedDebug.apk`: smaller, shrunk build. Recommended for everyday use.
- `*-debug.apk`: standard debug build.

Both are signed with a debug key, so installing a build from a different machine requires uninstalling the old one first.

## Build

Requires JDK 17 and the Android SDK (API 35).

```bash
./gradlew :calculator:test                      # unit tests
./gradlew assembleDebug assembleMinifiedDebug   # APKs in app/build/outputs/apk/
```

The calculation logic lives in the pure-Kotlin `calculator` module; `app` is the Jetpack Compose UI.

## License

[MIT](LICENSE)

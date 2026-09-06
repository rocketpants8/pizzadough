# Minified Debug APK — Design

**Date:** 2026-09-06  
**Status:** Approved  
**Platform:** Android (personal use, sideload APK)

## Goal

Produce a smaller, installable APK for sideloading without a release keystore. Daily debug builds stay fast and unminified. The new artifact is debug-signed so Android will install it.

## Decision

Add a Gradle build type named `minifiedDebug`. Do not minify the existing `debug` type. Do not change `release` (still unsigned, minify off).

## Build type

In `app/build.gradle.kts`, create `minifiedDebug` with `initWith(getByName("debug"))`, then:

| Setting | Value | Why |
| --- | --- | --- |
| `isMinifyEnabled` | `true` | R8 shrinks and obfuscates bytecode |
| `isShrinkResources` | `true` | Drop unused resources |
| `isDebuggable` | `false` | R8 can actually shrink; a debuggable minified APK stays larger |
| Signing | debug keystore (inherited) | Sideload without a release key |
| `applicationId` | unchanged (`com.rrp.pizzadough`) | Installing replaces the current app; no second icon |
| ProGuard | `proguard-android-optimize.txt` + `app/proguard-rules.pro` | Default optimize rules plus an app file for keep rules if assemble fails |

`debugImplementation` dependencies (Compose UI tooling) resolve through `matchingFallbacks += listOf("debug")`.

## How to build

```bash
./gradlew :app:assembleMinifiedDebug
```

Output: `app/build/outputs/apk/minifiedDebug/app-minifiedDebug.apk`

Unminified debug remains:

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## ProGuard / R8

Add `app/proguard-rules.pro`. Start empty except for a one-line comment. Compose and DataStore ship consumer keep rules.

If the first assemble crashes at runtime or R8 fails the build, add the minimum keep rules needed in that file. Do not pre-emptively copy a large template.

## Out of scope

- Release signing / keystore
- Turning minify on for `debug` or `release`
- ApplicationId suffix / side-by-side install next to debug
- Play Store AAB
- Code or UI changes in the app modules beyond Gradle and ProGuard files

## Success criteria

- `./gradlew :app:assembleMinifiedDebug` produces a signed APK at the path above
- `./gradlew :app:assembleDebug` still produces an unminified debug APK
- The minified APK is smaller than `app-debug.apk`
- The minified APK installs over the same applicationId using the debug key

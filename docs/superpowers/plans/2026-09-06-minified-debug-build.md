# Minified Debug APK Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a debug-signed, R8-minified APK for sideloading while leaving everyday `assembleDebug` unminified and fast.

**Architecture:** A custom `minifiedDebug` application build type inherits debug signing from `debug`, then turns on R8, resource shrinking, and `isDebuggable = false`. No app/UI code changes. ProGuard keep rules start empty; Compose and DataStore consumer rules are enough unless assemble fails.

**Tech Stack:** Kotlin 2.0.21, JDK 17, Android Gradle Plugin 8.7.3, Gradle 8.9, Compose BOM 2024.12.01, R8 (AGP default).

## Global Constraints

- Package / `applicationId` stays `com.rrp.pizzadough` (no suffix; install replaces the current app)
- Personal-use Android only; no Play Store, no release keystore
- Do not set `isMinifyEnabled` on `debug` or `release`
- `minifiedDebug`: `isMinifyEnabled = true`, `isShrinkResources = true`, `isDebuggable = false`, debug keystore via `initWith(debug)`
- ProGuard: `proguard-android-optimize.txt` + `app/proguard-rules.pro` (comment only unless R8 fails)
- `matchingFallbacks += listOf("debug")` so `debugImplementation` / debug library variants resolve
- Gradle/AndroidManifest scaffolding is a generated-config exception (no JVM unit test for the build type)
- Work on branch `feat/dough-calculator`, not `main`
- ANDROID_HOME is `/home/rrp/Android/Sdk`; JDK 17+ is available
- Out of scope: release signing, minify on `debug`/`release`, applicationId suffix, AAB, app module code/UI

## File map

- Modify: `app/build.gradle.kts` (add `minifiedDebug` build type; leave `release` minify off)
- Create: `app/proguard-rules.pro` (one-line comment only)

---

### Task 1: minifiedDebug build type

**Files:**
- Create: `app/proguard-rules.pro`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: existing `:app` Android application module; AGP default `debug` signing config and `debug`/`release` build types
- Produces: Gradle task `:app:assembleMinifiedDebug`; APK at `app/build/outputs/apk/minifiedDebug/app-minifiedDebug.apk` (debug-signed, package `com.rrp.pizzadough`)

- [ ] **Step 1: Add the ProGuard rules file**

Create `app/proguard-rules.pro` with exactly:

```
# App-specific R8 rules. Library consumer rules cover Compose and DataStore.
```

Do not add keep templates unless a later assemble step fails.

- [ ] **Step 2: Add the minifiedDebug build type**

Replace the `buildTypes` block in `app/build.gradle.kts` so the full file is:

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
        create("minifiedDebug") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            matchingFallbacks += listOf("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

Do not add `applicationIdSuffix`. Do not enable minify on `debug` or `release`.

- [ ] **Step 3: Assemble minifiedDebug**

From the repo root, with `ANDROID_HOME=/home/rrp/Android/Sdk`:

```bash
export ANDROID_HOME=/home/rrp/Android/Sdk
./gradlew :app:assembleMinifiedDebug
```

Expected: `BUILD SUCCESSFUL` and file exists at `app/build/outputs/apk/minifiedDebug/app-minifiedDebug.apk`.

If R8 fails the build, add only the keep rule named in the error to `app/proguard-rules.pro`, then re-run this same assemble command. Do not paste a generic Compose keep template.

- [ ] **Step 4: Assemble unminified debug and compare**

```bash
export ANDROID_HOME=/home/rrp/Android/Sdk
./gradlew :app:assembleDebug
ls -l app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/minifiedDebug/app-minifiedDebug.apk
```

Expected: `BUILD SUCCESSFUL`; both APKs exist; `app-minifiedDebug.apk` byte size is smaller than `app-debug.apk`.

- [ ] **Step 5: Verify package name and debug signing**

Use the newest build-tools on this machine:

```bash
export ANDROID_HOME=/home/rrp/Android/Sdk
BT="$(ls -1d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
"$BT/aapt" dump badging app/build/outputs/apk/minifiedDebug/app-minifiedDebug.apk | head -5
"$BT/apksigner" verify --print-certs app/build/outputs/apk/minifiedDebug/app-minifiedDebug.apk
```

Expected:
- `package: name='com.rrp.pizzadough'` (no suffix)
- `apksigner` reports the APK is verified
- Signer CN is `Android Debug` (debug keystore), not a release key

Device install is optional and not required to close this task.

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/proguard-rules.pro
git commit -m "build: add minifiedDebug APK variant"
```

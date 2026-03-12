# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug        # Build debug APK
./gradlew :composeApp:assembleRelease      # Build release APK
./gradlew :composeApp:build                # Full build + tests
./gradlew :composeApp:clean                # Clean build outputs

# Tests
./gradlew :composeApp:assembleUnitTest     # Assemble unit tests
./gradlew :composeApp:assembleAndroidTest  # Assemble instrumented tests

# iOS — compile Kotlin to framework (then open iosApp/iosApp.xcodeproj in Xcode)
./gradlew :composeApp:iosSimulatorArm64Binaries   # Simulator
./gradlew :composeApp:iosArm64Binaries            # Physical device
```

## Architecture

**Kotlin Multiplatform (KMP) + Compose Multiplatform** targeting Android and iOS from a single codebase.

**Module:** `composeApp` is the sole module with three source sets:
- `commonMain` — shared UI (`App.kt`), business logic, and platform abstractions
- `androidMain` — `MainActivity`, Android `Platform` implementation
- `iosMain` — `MainViewController`, iOS `Platform` implementation

**Platform abstraction pattern (`expect`/`actual`):**
- `commonMain/Platform.kt` declares `expect fun getPlatform(): Platform`
- `androidMain/Platform.android.kt` and `iosMain/Platform.ios.kt` provide `actual` implementations
- Use this same pattern for any new platform-specific functionality

**Entry points:**
- Android: `MainActivity.kt` → `setContent { App() }`
- iOS: `MainViewController.kt` → `ComposeUIViewController { App() }` (called from Swift in `iosApp/`)

All shared UI lives in `App.kt` as a `@Composable` function using Material3.

## Key Versions

| Tool | Version |
|------|---------|
| Kotlin | 2.3.0 |
| Compose Multiplatform | 1.10.0 |
| AGP | 8.11.2 |
| Min SDK | 24 |
| Target/Compile SDK | 36 |
| JVM target | 11 |

Version catalog: `gradle/libs.versions.toml`

## Known Issues

The current setup (KMP plugin + AGP in the same module) is deprecated and will break with AGP 9.0. The future fix is to split into a separate KMP library module and a standalone Android app module.

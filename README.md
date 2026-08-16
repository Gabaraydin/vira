# Vira

Vira is an offline Android workout tracker built around a repeating program
cycle instead of a weekly calendar. A program is an ordered list of training
and rest days; the cycle only advances when a workout is logged, so skipping a
few days never skips a program day.

The app declares no `INTERNET` permission and ships no analytics, crash
reporting, or ad SDKs. Everything stays on-device; backup is a manual JSON
export/import plus Android Auto Backup.

## Status

Early scaffold. The Compose UI, Hilt DI setup, and CI are in place; the data
layer, cycle engine, and screens are not built yet.

## Stack

- Kotlin, Jetpack Compose (Material 3), single Activity
- Room for persistence, Hilt for DI, DataStore for settings
- Coroutines/Flow, Navigation Compose
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)

## Building

```bash
./gradlew build
```

Requires JDK 17+ and an Android SDK with platform 37 installed.

## Testing

```bash
./gradlew test
```

## License

GPL-3.0.

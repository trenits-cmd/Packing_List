# Packing List

Eine einfache Android-Packlisten-App mit Jetpack Compose.

## Voraussetzungen

- JDK 17
- Android SDK mit Android 14 (API-Level 34)

Der Gradle Wrapper lädt Gradle 8.9 beim ersten Build automatisch herunter.

## Debug-APK bauen

Unter Linux oder macOS:

```sh
./gradlew assembleDebug
```

Unter Windows:

```bat
gradlew.bat assembleDebug
```

Die fertige APK liegt unter `app/build/outputs/apk/debug/app-debug.apk`.

## CI

Der GitHub-Actions-Workflow baut die Debug-APK bei Pushes auf `main` oder
`master`, bei Pull Requests und manuell. Er verwendet JDK 17 und Android SDK
API-Level 34. Die APK wird als `packing-list-debug-apk`-Artefakt hochgeladen.

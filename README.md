# Packliste

Eine lokale Android-Packlisten-App mit Jetpack Compose und Room-Datenbank.

## Funktionen

- **Reise auswählen:** Reiseart (Dienstreise oder Urlaub/privat) und Jahreszeit
  per Dropdown wählen, danach den Namen der Reise eingeben. Passende Reisen
  werden aus der Datenbank vorgeschlagen und mit einem Tipp geöffnet.
- **Reise anlegen:** Existiert der eingegebene Name noch nicht, kann die Reise
  direkt neu angelegt werden. Die Packliste wird automatisch aus dem
  Artikelkatalog für Reiseart und Jahreszeit zusammengestellt.
- **Artikelkatalog:** 95 Artikel aus `app/src/main/res/raw/articles.csv` mit
  Reiseart, Saison und Wichtigkeit (1 = sehr wichtig). Der Katalog wird beim
  ersten Start importiert.
- **Packen:** Jeder Artikel hat einen Schieberegler. Nach rechts schieben
  markiert den Artikel als gepackt – er wird grün und rutscht nach unten.
  Offene Artikel stehen oben und sind rot markiert. Sortiert wird nach
  Wichtigkeit.
- **Fortschritt:** Kopfkarte mit Anzahl gepackter Artikel und Fortschrittsbalken.
  Über „Alle Häkchen zurücksetzen“ startet die Liste wieder von vorn.
- **Artikelfotos:** Zu jedem Artikel kann in der Datenbank ein eigenes Foto
  ausgewählt werden. Es wird in den privaten App-Speicher kopiert und in der
  Packliste sowie im Katalog angezeigt.
- **Datenbank verwalten:** Reisen anlegen, bearbeiten und löschen; Artikel
  hinzufügen, bearbeiten, filtern, suchen und löschen.
- **Persistenz:** Reisen, Artikel, Fotos und Packstände liegen lokal in einer
  Room-Datenbank bzw. im privaten App-Speicher.
- **Darstellung:** Material-3-Design mit hellem und dunklem Farbschema.

## Technik

- Kotlin 1.9, Jetpack Compose (Material 3)
- Room 2.6 mit KSP, ViewModel + StateFlow
- Coil für die Anzeige lokaler Artikelfotos
- Android Photo Picker (`ActivityResultContracts.PickVisualMedia`)
- minSdk 24, targetSdk/compileSdk 34

## Projektstruktur

```
app/src/main/
├── res/raw/articles.csv     # Artikelkatalog
└── java/com/example/packinglist/
    ├── MainActivity.kt
    ├── data/                # Room-Entities, DAO, Datenbank, Repository, Fotos
    └── ui/                  # ViewModel, Navigation, Theme und Screens
        ├── components/
        ├── screens/
        └── theme/
```

## Voraussetzungen

- JDK 17 oder neuer
- Android SDK mit Android 14 (API-Level 34)
- Der Gradle Wrapper lädt Gradle 8.9 beim ersten Build automatisch herunter.

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

## Artikelkatalog ändern

Die Datei `app/src/main/res/raw/articles.csv` hat folgende Spalten:

```csv
Artikel,Reiseart,Saison,Wichtigkeit (1 sehr wichtig)
Gadgets,Urlaub,immer,3
A1 Bescheinigung,Dienstreise,immer,1
```

Erlaubte Werte für `Reiseart` sind `Urlaub` und `Dienstreise`. Für `Saison`
werden `immer`, `Frühling`, `Sommer`, `Herbst` und `Winter` unterstützt.

Die CSV ist die Quelle für den Import in die Datenbankversion 2. Nach dem
ersten Start werden Artikel direkt in der App verwaltet; spätere Änderungen
an der CSV werden nicht automatisch in eine bestehende Datenbank übernommen.

## CI

Der GitHub-Actions-Workflow baut die Debug-APK bei Pushes auf `main` oder
`master`, bei Pull Requests und manuell. Er verwendet JDK 17 und Android SDK
API-Level 34. Die APK wird als `packing-list-debug-apk`-Artefakt hochgeladen.

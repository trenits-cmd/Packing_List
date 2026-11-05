# Packing List

Eine lokale Android-Packlisten-App mit Jetpack Compose und Room-Datenbank.

## Funktionen

- **Reise auswählen:** Reiseart (Dienstreise oder Urlaub/privat) und Jahreszeit
  per Dropdown wählen, danach den Namen der Reise eingeben. Passende Reisen
  werden aus der Datenbank vorgeschlagen und mit einem Tipp geöffnet.
- **Reise anlegen:** Existiert der eingegebene Name noch nicht, kann die Reise
  direkt neu angelegt werden. Die Packliste wird automatisch aus dem
  Artikelkatalog für Reiseart und Jahreszeit zusammengestellt.
- **Reisezeitraum:** Start- und Enddatum per Kalender wählen (Standard: heute).
  Die App berechnet die Anzahl der Reisetage inklusive beider Grenztage.
- **Artikelkatalog:** 95 Artikel aus `app/src/main/res/raw/articles.csv` mit
  Reiseart, Saison und Wichtigkeit (1 = sehr wichtig). Der Katalog wird beim
  ersten Start importiert.
- **Packen:** Jeder Artikel hat einen Schieberegler. Nach rechts schieben
  markiert den Artikel als gepackt – er wird grün und rutscht nach unten.
  Offene Artikel stehen oben und sind rot markiert. Sortiert wird nach
  Wichtigkeit.
- **Fortschritt:** Kopfkarte mit Anzahl gepackter Artikel und Fortschrittsbalken.
  Über „Alle Häkchen zurücksetzen“ startet die Liste wieder von vorn.
- **Artikelfotos:** Fotos können direkt mit der Kamera aufgenommen oder aus
  der Galerie gewählt und anschließend zugeschnitten werden. Sie werden lokal
  gespeichert und beim Speichern automatisch nach GitHub hochgeladen, wenn die
  Verbindung eingerichtet ist. In Packliste und Katalog ist das Foto sichtbar.
- **Artikel vergrößern:** Ein Tipp auf einen Artikel klappt die Karte auf und
  zeigt Name und Foto größer.
- **GitHub-Speicher:** Nach dem Hinterlegen eines GitHub-Tokens über das
  Zahnrad oben rechts werden neue Fotos zusätzlich in das Repository
  hochgeladen. Bereits lokale Fotos lassen sich dort nachträglich hochladen.
- **Datenbank verwalten:** Reisen anlegen, bearbeiten und löschen; Artikel
  hinzufügen, bearbeiten, filtern, suchen und löschen.
- **Alte Reisen löschen:** In der Reiseauswahl oder direkt in der geöffneten
  Packliste über das Papierkorb-Symbol – mit Sicherheitsabfrage.
- **Persistenz:** Reisen, Artikel, Fotos und Packstände liegen lokal in einer
  Room-Datenbank bzw. im privaten App-Speicher.
- **Darstellung:** Material-3-Design mit hellem und dunklem Farbschema.

## Technik

- Kotlin 1.9, Jetpack Compose (Material 3)
- Room 2.6 mit KSP, ViewModel + StateFlow
- Coil für die Anzeige lokaler und entfernter Artikelfotos
- Android Photo Picker und Kamera-Aufnahme über `FileProvider`
- Bildzuschnitt mit UCrop
- GitHub Contents API über OkHttp
- Verschlüsselte Token-Speicherung mit `EncryptedSharedPreferences`
- `java.time` mit Core-Library-Desugaring
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

## GitHub-Fotos einrichten

1. In der App oben rechts auf das **Zahnrad** tippen.
2. Ein Fine-grained Personal Access Token erstellen:
   - Repository-Zugriff: nur `trenits-cmd/Packing_List`
   - Berechtigung: **Contents – Read and write**
3. Token zusammen mit Owner, Repository, Branch und Zielordner eintragen und
   speichern.
4. Mit „Verbindung testen“ prüfen, ob die Zugangsdaten funktionieren.
5. Neue Artikelfotos werden ab dann automatisch hochgeladen. Vorhandene lokale
   Fotos lassen sich über „Lokale Fotos jetzt hochladen“ nachziehen.

> **Achtung:** Das Repository ist öffentlich. Hochgeladene Fotos sind damit
> öffentlich im Internet sichtbar. Das Token wird verschlüsselt auf dem Gerät
> gespeichert und gehört nicht ins Repository.

Über „GitHub-Verbindung trennen“ kann der Zugang wieder entfernt werden;
bereits hochgeladene Dateien bleiben im Repository und müssen dort gelöscht
werden.

## Artikelkatalog ändern

Die Datei `app/src/main/res/raw/articles.csv` hat folgende Spalten:

```csv
Artikel,Reiseart,Saison,Wichtigkeit (1 sehr wichtig)
Gadgets,Urlaub,immer,3
A1 Bescheinigung,Dienstreise,immer,1
```

Erlaubte Werte für `Reiseart` sind `Urlaub` und `Dienstreise`. Für `Saison`
werden `immer`, `Frühling`, `Sommer`, `Herbst` und `Winter` unterstützt.

Die CSV ist die Quelle für den Import in die Datenbank. Nach dem ersten Start
werden Artikel direkt in der App verwaltet; spätere Änderungen an der CSV
werden nicht automatisch in eine bestehende Datenbank übernommen.

## CI

Der GitHub-Actions-Workflow baut die Debug-APK bei Pushes auf `main` oder
`master`, bei Pull Requests und manuell. Er verwendet JDK 17 und Android SDK
API-Level 34. Die APK wird als `packing-list-debug-apk`-Artefakt hochgeladen.

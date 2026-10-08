# TeddyPlayer

Kinderfreundliche Android-App für einen [TeddyCloud](https://github.com/toniebox-reverse-engineering/teddycloud)-Server:
alle Tonies als große Cover-Kacheln, Antippen spielt ab, und dabei wird der Tonie wie bei der Toniebox
heruntergeladen. Danach geht er auch offline.

## Bedienung
- **Grid**: Tonie antippen → abspielen. Badge oben rechts: Pfeil = lädt, Häkchen = offline verfügbar.
  Ohne Server sind nicht geladene Tonies grau und wackeln beim Antippen.
- **Player**: ⏮ / ⏯ / ⏭ springen zwischen Kapiteln; die Punkte zeigen die Kapitel (antippbar).
  Beim nächsten Abspielen geht es an der letzten Stelle weiter.
- **Elternbereich**: Zahnrad oben rechts **3 Sekunden gedrückt halten**, dann eine Einmaleins-Aufgabe lösen.
  Dort lassen sich die Server-Adresse, „Titel anzeigen“ und die Downloads verwalten.
- **Ersteinrichtung**: Beim ersten Start fragt ein Dialog nach der Server-Adresse (vorausgefüllt: `http://tc`).

## Technik
Kotlin, Jetpack Compose, MVI (`core/mvi/MviViewModel` + reine Reducer), Koin, Ktor, Room, DataStore,
Media3 (ExoPlayer, MediaSessionService, DownloadManager mit gemeinsamem `SimpleCache`), Coil 3.
minSdk 29. Läuft auf Handys (Hochformat) und Tablets (Hoch- und Querformat, adaptives Grid und Player).

```
./gradlew :app:testDebugUnitTest   # Unit-Tests
./gradlew :app:installDebug        # auf Gerät/Emulator installieren
```

### Hinweis Emulator auf macOS
Der Android-Emulator erreicht unter macOS das Heimnetz oft nicht ("No route to host",
fehlende Berechtigung „Lokales Netzwerk"). Entweder dem Emulator in den Systemeinstellungen
unter *Datenschutz & Sicherheit → Lokales Netzwerk* den Zugriff erlauben, oder einen Port-Forward
nutzen (`adb reverse tcp:8089 tcp:8089` + lokaler Proxy) und im Elternbereich `127.0.0.1:8089` eintragen.
Auf einem echten Handy im WLAN ist das nicht nötig.

## Testdaten
`app/src/test/resources/tag_index.json` ist eine echte `getTagIndex`-Antwort eines TeddyCloud-Servers
mit anonymisierten Tag-IDs/UIDs (Titel, Kapitel und Cover-URLs sind öffentliche Tonie-Metadaten).

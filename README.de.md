# TeddyPlayer

*[English version](README.md)*

Ein kinderfreundlicher Android-Player für deinen eigenen [TeddyCloud](https://github.com/toniebox-reverse-engineering/teddycloud)-Server.

Alle Hörspiele auf deinem TeddyCloud-Server erscheinen als große Cover-Kacheln. Antippen, und es geht los. Beim
Abspielen wird das Hörspiel heruntergeladen und funktioniert danach auch offline (im Auto, im Urlaub, …).
Die Bedienung kommt ohne Text aus, damit auch Kinder, die noch nicht lesen können, die App allein nutzen können.

> **Hinweis:** TeddyPlayer ist ein inoffizielles Hobbyprojekt und steht in keiner Verbindung zu Herstellern von
> Audioboxen oder Hörfiguren. Du brauchst einen eigenen TeddyCloud-Server mit deinen eigenen Inhalten.

## Funktionen

- **Hörspiel-Übersicht** – alle Hörspiele mit Audio auf dem Server, Dubletten zusammengefasst, System-Sounds und
  Streams ausgeblendet. Zuletzt gehörte stehen vorne.
- **Einmal gehört, offline dabei** – streamt sofort und lädt parallel die komplette Datei in einen gemeinsamen Cache.
  Danach spielt das Hörspiel ohne Netz. Unterbrochene Downloads laufen automatisch weiter.
- **Offline-Modus** – ohne Verbindung zum Server sind nicht geladene Hörspiele grau und wackeln beim Antippen.
- **Einfacher Player** – riesige Knöpfe für Play/Pause und Kapitel, Kapitel als Punkte (antippbar).
  Beim nächsten Mal geht es an der letzten Stelle weiter. Läuft im Hintergrund und über Sperrbildschirm/Benachrichtigung.
- **Titel optional** – standardmäßig aus (nur Symbole), für Kinder, die lesen können, zuschaltbar.
- **Elternbereich** – Zahnrad 3 Sekunden gedrückt halten, dann eine Einmaleins-Aufgabe lösen. Dort lassen sich
  Server-Adresse, Titel-Anzeige und Downloads verwalten (Speicherverbrauch, einzelne oder alle löschen).
- **Ersteinrichtung** – fragt nach der Server-Adresse (vorausgefüllt mit `http://tc`) und ob Titel angezeigt werden.
- **Handy und Tablet** – Handys im Hochformat; Tablets drehen frei, mit adaptivem Grid und zweispaltigem Player
  im Querformat.

## Voraussetzungen

- Ein laufender [TeddyCloud](https://github.com/toniebox-reverse-engineering/teddycloud)-Server im Heimnetz mit
  Audio-Inhalten (die App nutzt `GET /api/getTagIndex` und die Content-Download-Endpunkte)
- Android 10 (API 29) oder neuer
- Zum Bauen: JDK 17+ und das Android SDK (z. B. über Android Studio)

## Download

Die aktuelle `TeddyPlayer-x.y.z.apk` gibt es auf der [Releases](../../releases/latest)-Seite. Einfach auf dem
Android-Gerät herunterladen und installieren (ggf. die Installation aus Browser oder Dateimanager erlauben).

## Selbst bauen

```bash
git clone <dieses Repository>
cd TeddyPlayer
./gradlew :app:installDebug
```

Beim ersten Start die Adresse des TeddyCloud-Servers eingeben, z. B. `http://tc` oder `http://192.168.1.50`.
Ein angehängtes `/web` (Adresse der TeddyCloud-Weboberfläche) wird automatisch entfernt.
Die App spricht im Heimnetz per HTTP mit TeddyCloud.

## Releases

Releases baut [GitHub Actions](.github/workflows/release.yml): Ein Tag wie `v1.2.3` startet die Tests, baut eine
signierte Release-APK (`versionName` 1.2.3, `versionCode` 10203) und veröffentlicht sie als GitHub-Release.

Der Workflow braucht diese Repository-Secrets: `KEYSTORE_BASE64` (Keystore, Base64-kodiert), `KEYSTORE_PASSWORD`,
`KEY_ALIAS` und `KEY_PASSWORD`.
Ein lokales `./gradlew :app:assembleRelease` ohne die `TEDDYPLAYER_*`-Umgebungsvariablen wird mit dem Debug-Schlüssel signiert.

## Architektur

- **Kotlin + Jetpack Compose**, ein `app`-Modul
- **MVI**: jeder Screen hat einen Contract (State / Intent / Result / Effect), ein ViewModel auf Basis von
  [`MviViewModel`](app/src/main/java/xyz/weilandt/teddyapp/core/mvi/MviViewModel.kt) und einen reinen Reducer
- **Koin** für Dependency Injection
- **Ktor** + kotlinx.serialization für die TeddyCloud-API
- **Room** (Hörspiel-Cache, Hörpositionen) und **DataStore** (Einstellungen)
- **Media3**: ExoPlayer in einem `MediaSessionService`; `DownloadManager` und Player teilen sich einen
  `SimpleCache` mit der Hörspiel-ID als Schlüssel – das macht „jetzt streamen, offline behalten“ möglich
- **Coil 3** für Cover, auf der Festplatte gecacht für offline
- Jede UI-Komponente hat Compose-Previews für alle Zustände (Handy und Tablet)

```
app/src/main/java/xyz/weilandt/teddyapp/
├── core/mvi        MVI-Basisklasse
├── domain          Modelle (Hörspiel, Kapitel, …) und Repository-Interfaces
├── data            TeddyCloud-API, Mapping/Filter, Room, DataStore, Netzwerk-Monitor
├── playback        Media3-Service, Controller, Downloads, gemeinsamer Cache
├── di              Koin-Module
└── ui              Übersicht, Player, Elternbereich, Ersteinrichtung, Komponenten, Theme
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Die Unit-Tests decken Reducer und ViewModels, das Mapping und Filtern des Tag-Index, die Kapitel-Logik und das
Repository (mit Ktor-`MockEngine`) ab.

`app/src/test/resources/tag_index.json` ist eine echte `getTagIndex`-Antwort eines TeddyCloud-Servers mit
anonymisierten Tag-IDs/UIDs. Titel, Kapitel und Cover-URLs sind öffentlich verfügbare Metadaten.

## Hinweise zur Entwicklung

**Android-Emulator unter macOS:** Der Emulator erreicht Geräte im Heimnetz oft nicht („No route to host“), weil
Android Studio die Berechtigung *Lokales Netzwerk* fehlt. Entweder unter *Systemeinstellungen → Datenschutz &
Sicherheit → Lokales Netzwerk* erlauben und den Emulator per Cold Boot neu starten, oder einen Port weiterleiten
(`adb reverse tcp:8089 tcp:8089` plus kleiner lokaler TCP-Proxy zum Server) und `127.0.0.1:8089` als
Server-Adresse eintragen. Ein echtes Gerät im WLAN braucht das nicht.

**NFC:** Hörspiele durch Dranhalten der Figur ans Handy zu starten wurde ausprobiert und wieder entfernt – Handys
erkennen die Figuren überhaupt nicht (eine Bankkarte wurde am selben Handy erkannt, die Figur nie).

## Danksagung

- [TeddyCloud](https://github.com/toniebox-reverse-engineering/teddycloud) und die
  [TeddyCloud-Community](https://github.com/toniebox-reverse-engineering) – ohne sie gäbe es diese App nicht
- [Media3](https://developer.android.com/media/media3), [Koin](https://insert-koin.io),
  [Ktor](https://ktor.io), [Coil](https://coil-kt.github.io/coil/)

## Lizenz

[MIT](LICENSE)

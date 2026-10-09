# TeddyPlayer

*[Deutsche Version](README.de.md)*

A kid-friendly Android player for your own [TeddyCloud](https://github.com/toniebox-reverse-engineering/teddycloud) server.

All Tonies stored on your TeddyCloud server show up as big cover tiles. Tap one and it plays. Just like on the
Toniebox, the audio is downloaded while it plays, so it also works offline afterwards (in the car, on holiday, …).
The interface works with icons only, so children who can't read yet can use it on their own.

> **Note:** TeddyPlayer is an unofficial hobby project. It is not affiliated with, endorsed by or connected to
> Boxine GmbH / tonies®. "Toniebox" and "Tonies" are trademarks of their respective owners.
> You need your own TeddyCloud server with your own content.

## Features

- **Tonie grid** – every Tonie with audio on the server, duplicates merged, system sounds and streams hidden.
  Recently played Tonies come first.
- **Plays like a Toniebox** – streams immediately and downloads the full file in parallel into a shared cache.
  Once downloaded, a Tonie plays without network. Interrupted downloads resume automatically.
- **Offline mode** – without a connection to the server, Tonies that aren't downloaded are greyed out and
  shake when tapped.
- **Simple player** – huge play/pause and chapter buttons, chapters shown as dots (tap to jump).
  Playback resumes where it stopped. Works in the background and from the lock screen / notification.
- **Optional titles** – off by default (icons only); can be switched on for children who can read.
- **Parent area** – hold the gear icon for 3 seconds, then solve a small multiplication task. There you can change
  the server address, toggle titles and manage downloads (storage usage, delete single or all downloads).
- **First-run setup** – asks for the server address (pre-filled with `http://tc`) and whether to show titles.
- **Phones and tablets** – phones stay in portrait; tablets rotate freely with an adaptive grid and a
  two-column player in landscape.

## Requirements

- A running [TeddyCloud](https://github.com/toniebox-reverse-engineering/teddycloud) server in your home network
  with audio content (the app uses `GET /api/getTagIndex` and the content download endpoints)
- Android 10 (API 29) or newer
- To build: JDK 17+ and the Android SDK (e.g. via Android Studio)

## Getting started

```bash
git clone <this repository>
cd TeddyPlayer
./gradlew :app:installDebug
```

On first launch, enter the address of your TeddyCloud server, e.g. `http://tc` or `http://192.168.1.50`.
A `/web` suffix (the URL of the TeddyCloud web UI) is stripped automatically.
The app talks to TeddyCloud via plain HTTP inside your home network.

Release build:

```bash
./gradlew :app:assembleRelease
```

The release build is currently signed with the debug key – configure your own signing config in
`app/build.gradle.kts` before distributing it.

## Architecture

- **Kotlin + Jetpack Compose**, single `app` module
- **MVI**: every screen has a contract (State / Intent / Result / Effect), a ViewModel based on
  [`MviViewModel`](app/src/main/java/xyz/weilandt/teddyapp/core/mvi/MviViewModel.kt) and a pure reducer
- **Koin** for dependency injection
- **Ktor** + kotlinx.serialization for the TeddyCloud API
- **Room** (tonie cache, playback positions) and **DataStore** (settings)
- **Media3**: ExoPlayer in a `MediaSessionService`; a `DownloadManager` and the player share one `SimpleCache`
  keyed by the tonie ID, which is what makes "stream now, keep it offline" work
- **Coil 3** for covers, cached on disk for offline use
- Every UI component has Compose previews for each of its states (phone and tablet)

```
app/src/main/java/xyz/weilandt/teddyapp/
├── core/mvi        MVI base class
├── domain          models (Tonie, chapters, …) and repository interfaces
├── data            TeddyCloud API, mapping/filtering, Room, DataStore, network monitor
├── playback        Media3 service, controller, downloads, shared cache
├── di              Koin modules
└── ui              library grid, player, parent area, first-run setup, components, theme
```

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

Unit tests cover the reducers and ViewModels, the tag index mapping and filtering, chapter logic and the
repository (with a Ktor `MockEngine`).

`app/src/test/resources/tag_index.json` is a real `getTagIndex` response from a TeddyCloud server with
anonymized tag IDs/UIDs. Titles, chapters and cover URLs are public Tonie metadata.

## Development notes

**Android emulator on macOS:** the emulator often can't reach devices in your home network
("No route to host"), because Android Studio lacks the *Local Network* permission. Either allow it under
*System Settings → Privacy & Security → Local Network* and cold-boot the emulator, or forward a port
(`adb reverse tcp:8089 tcp:8089` plus a small local TCP proxy to your server) and enter `127.0.0.1:8089` as
server address. A real device in your Wi-Fi doesn't need this.

**NFC:** starting Tonies by holding a figure against the phone was tried and removed again – phones don't
detect the figures at all (a bank card was detected on the same phone, the figure never).

## Credits

- [TeddyCloud](https://github.com/toniebox-reverse-engineering/teddycloud) and the
  [toniebox-reverse-engineering](https://github.com/toniebox-reverse-engineering) community – without them
  this app wouldn't exist
- [Media3](https://developer.android.com/media/media3), [Koin](https://insert-koin.io),
  [Ktor](https://ktor.io), [Coil](https://coil-kt.github.io/coil/)

## License

[MIT](LICENSE)

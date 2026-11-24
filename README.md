# Hohe (ሆሄ)

**An offline-first Amharic and English keyboard for Android.**
Typing stays on the device. There are no ads, no trackers and no network permission.

[![License: Apache 2.0](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
![Status](https://img.shields.io/badge/status-pre--release-orange)
![Min SDK](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF)

> Hohe is pre-release (version 0.1.0). Read [Known limitations](#known-limitations) before you rely on it.

## Contents

- [Why Hohe](#why-hohe)
- [Features](#features)
- [Privacy](#privacy)
- [Install](#install)
- [Build from source](#build-from-source)
- [Development](#development)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Data and licences](#data-and-licences)
- [Contributing](#contributing)
- [Known limitations](#known-limitations)
- [Licence](#licence)

## Why Hohe

Many Amharic keyboards depend on online services or include tracking. Hohe is for people who want fluent Ge'ez-script typing that works without a connection and keeps what they type private.

The Amharic dictionary, the English word list, the emoji data and the personal word list all run on the phone.

## Features

| Area | What it does |
|---|---|
| **Amharic typing** | Ge'ez-script input with a converter, a 100,000-word frequency dictionary, ranked suggestions and Ethiopic punctuation. |
| **English typing** | Word suggestions, next-word prediction and proximity-based correction. Words are never replaced automatically. |
| **Emoji** | An emoji panel, an emoji row and suggestions in English and Amharic. |
| **Clipboard** | A clipboard panel. Items expire after 24 hours unless pinned. Items marked sensitive are never saved. |
| **Layout** | Canvas-drawn keys, long-press alternatives, swipe to delete, key repeat, space slide and shift handling. |
| **Themes** | Selectable themes, colour themes, keyboard height, text size, key borders and a high-contrast mode. |
| **Field awareness** | Password, numeric and no-suggestion fields do not learn or suggest words. Private (incognito) fields behave the same way. |
| **Voice typing** | Uses the phone's speech recognition service. Hohe receives only the recognised text. |
| **Settings app** | Onboarding, settings, search, saved Amharic words, clear data, help and about screens. Amharic and English interface. |

## Privacy

- **No network permission.** The app declares only `VIBRATE` and `RECORD_AUDIO`. Voice typing uses the system speech service, which has its own policy.
- **Data stays local.** Saved words, clipboard items and recent emoji are stored on the device. You can clear them in Settings.
- **Backup.** Settings and saved Amharic words are included in Android backup.
- **No analytics, advertising or crash-reporting SDKs.**

## Install

Hohe is not yet on an app store. Build a debug APK from source (below) and install it on a device running Android 8.0 or later.

To enable Hohe as your keyboard:

1. Open **Settings → System → Languages & input → On-screen keyboard** (the path varies by manufacturer).
2. Turn on **Hohe**.
3. Switch to Hohe with the keyboard switcher key.

## Build from source

**Requirements:** JDK 17, an Android SDK with compile SDK 37, and Windows, macOS or Linux.

```powershell
git clone https://github.com/Natnael-Dev/hohe.git
cd hohe
.\gradlew.bat assembleDebug        # Windows
./gradlew assembleDebug            # macOS / Linux
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Build outside cloud-synced folders such as OneDrive. Sync can lock files during a build.

## Development

| Task | Windows | macOS / Linux |
|---|---|---|
| Unit tests | `.\gradlew.bat test` | `./gradlew test` |
| Kotlin style | `.\gradlew.bat ktlintCheck` | `./gradlew ktlintCheck` |
| Android lint | `.\gradlew.bat lint` | `./gradlew lint` |
| Release build | `.\gradlew.bat assembleRelease` | `./gradlew assembleRelease` |
| Guard run | `powershell -File .\verify.ps1` | Windows script only |

`verify.ps1` runs the build, tests, lint, the permission allow-list check, the forbidden-SDK scan and the APK size check.

To rebuild the Amharic dictionary, you need the upstream source dumps listed in PROVENANCE.md. They are not stored in this repository.

```powershell
.\gradlew.bat :tools:dictbuilder:run --args="amharic"
```

Inputs, dump dates and checksums are in [`tools/dictbuilder/sources/amharic/PROVENANCE.md`](tools/dictbuilder/sources/amharic/PROVENANCE.md).

## Architecture

Hohe is a set of Gradle modules. Language logic is pure JVM code, so most of it is tested without a device.

| Module | Role |
|---|---|
| `:app` | Input method service, settings app and main activity. Depends on every module below. |
| `:core:keyboard` | Keyboard model, layouts, input state machines and the field-policy classifier. |
| `:lang:amharic` | Amharic converter and the binary dictionary reader (`AMWL` format). |
| `:lang:english` | English dictionary, suggestion engine and emoji catalogue. |
| `:data` | Local storage: DataStore settings and personal data. |
| `:feature:handwriting` | Placeholder for handwriting input. Deferred and not shipped. |
| `:tools:dictbuilder` | Command-line tool that builds the dictionary assets. |

Stack: Kotlin, Jetpack Compose, DataStore, the platform SQLite API, Kotlin coroutines, JUnit 4 and Robolectric.

## Project structure

```
app/                      Android app, IME service, settings UI, dictionary assets
core/keyboard/            Keyboard model and state machines (pure Kotlin)
lang/amharic/             Amharic converter, dictionary format, tests and golden files
lang/english/             English dictionary, suggestions and emoji data
data/                     Local storage
feature/handwriting/      Placeholder module
tools/dictbuilder/        Dictionary builder and its source notes
tools/desh-hash-scan.ps1  Guard script that checks the repo for copied proprietary files
verify.ps1                Repository guard run
```

## Data and licences

The source code is licensed under the **Apache License 2.0**. The bundled data keeps its own licences:

| File | Licence |
|---|---|
| Amharic word list (`am_words.bin`) | CC BY-SA 4.0 (share-alike) |
| English word list (`en_main.bin`) | Apache 2.0 (from AOSP) |
| Emoji data (`emoji.tsv`) | Unicode License v3 |

[DATA_LICENSES.md](DATA_LICENSES.md) has the attribution text and explains what share-alike means for redistribution.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) first. It covers the privacy rules, the test and style checks, and the commit format.

- Bugs and feature requests: use the issue templates.
- Security issues: follow [SECURITY.md](SECURITY.md). Do not open a public issue.
- Questions: see [SUPPORT.md](SUPPORT.md).

Everyone taking part is bound by the [Code of Conduct](CODE_OF_CONDUCT.md).

## Known limitations

- **Pre-release.** Version 0.1.0 has not been published to an app store.
- **Voice typing** works through the system service. The on-device run is not yet fully verified.
- **Handwriting** is deferred and not shipped.
- **Inline autofill suggestions** are not implemented.
- **Photo theme** is not implemented.
- **Performance and accessibility passes** are planned but not complete.
- **Release signing and store listing** are not set up.

## Licence

Copyright 2026 Hohe contributors. Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

Bundled data licences are listed in [DATA_LICENSES.md](DATA_LICENSES.md).

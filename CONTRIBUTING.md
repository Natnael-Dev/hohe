# Contributing to Hohe

Thanks for your interest in Hohe. This guide covers setup, the rules a change must meet, and how to send it.

## Ground rules

- Hohe is **offline-first and privacy-first**. Do not add network calls, analytics,
  advertising or crash-reporting SDKs. The app currently declares only the `VIBRATE`
  and `RECORD_AUDIO` permissions. A change that adds a permission needs a clear reason in the pull request.
- Typed text must never leave the device. Do not log typed content, even in debug builds.
- Keep each pull request focused on one change.

## Development setup

Requirements: JDK 17, the Android SDK (compile SDK 37, min SDK 26), and Android Studio or the command-line tools.

```powershell
git clone https://github.com/Natnael-Dev/hohe.git
cd hohe
.\gradlew.bat assembleDebug      # Windows
./gradlew assembleDebug          # macOS / Linux
```

Build outside cloud-synced folders such as OneDrive. Sync locks files during Gradle builds.

## Running the checks

```powershell
.\gradlew.bat test               # unit tests (JVM)
.\gradlew.bat ktlintCheck        # Kotlin style
.\gradlew.bat lint               # Android lint
```

`verify.ps1` runs the build, tests, lint and privacy guard checks together. Run it before you open a pull request.

## Coding standards

- Kotlin, formatted by ktlint. Settings are in [.editorconfig](.editorconfig).
- UTF-8 and LF line endings. Batch files use CRLF, per [.gitattributes](.gitattributes).
- Every behaviour change needs a unit test. Keyboard logic lives in pure JVM modules
  (`core/keyboard`, `lang/*`) so it can be tested without a device.
- Do not commit generated files, keystores, `local.properties`, or anything from `build/`.
  See [.gitignore](.gitignore).

## Data files

Changes under `app/src/main/assets/dict/` must update the matching provenance note and
[DATA_LICENSES.md](DATA_LICENSES.md). New data must have a licence that allows redistribution
in the app. Explain the source in the pull request.

## Commits and branches

- Use [Conventional Commits](https://www.conventionalcommits.org/), for example
  `fix(amharic): keep dominant completion first`. Keep the subject at 72 characters or fewer.
- Branch from the default branch.
- Do not force-push a branch that has an open review.

## Submitting a pull request

1. For large features or behaviour changes, open an issue first.
2. Fill in the pull request template.
3. Run `verify.ps1`, or say in the pull request which check you could not run and why.
4. A maintainer reviews each pull request before merge.

## Bugs and security

- Bugs and feature requests: use the issue templates.
- Security vulnerabilities: do **not** open a public issue. Follow [SECURITY.md](SECURITY.md).

By contributing, you agree that your contribution is licensed under the Apache License 2.0
([LICENSE](LICENSE)). Data contributions keep the licence described in [DATA_LICENSES.md](DATA_LICENSES.md).

Please also follow the [Code of Conduct](CODE_OF_CONDUCT.md).

# English lexicon source

- **Source:** AOSP LatinIME `dictionaries/en_wordlist.combined.gz` (dictionary header `version=54`),
  fetched from `android.googlesource.com` `main` with `?format=TEXT` (base64) on 2026-11-24.
  SHA-256 of the `.gz`: see `source.properties`.
- **Licence:** Apache-2.0. Verified: the LatinIME repo's `Android.bp` declares
  `packages_inputmethods_LatinIME_license` with `SPDX-license-identifier-Apache-2.0` and
  `license_text: ["NOTICE"]`, and the repo `NOTICE` is the Apache License 2.0
  ("Copyright (c) 2008, The Android Open Source Project"). The app must ship this notice in its
  open-source licences screen.
- **Filtering:** entries flagged `possibly_offensive=true` or `flags=offensive`, `not_a_word=true`,
  `f=0`, and words longer than 39 chars are dropped; shortcut lines are ignored. The top 150 000
  by frequency are kept (frequency is AOSP's 0..255 log-scale `f`).
- **Raw download location:** `C:\dev\hohe-data\english\` (outside the repo).

Rebuild: `gradlew :tools:dictbuilder:run --args="english lexicon"`.

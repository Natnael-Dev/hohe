# Amharic word list: provenance and licences

Output: `app/src/main/assets/dict/am_words.bin` (format "AMWL" v1, reader in
`lang/amharic/src/main/kotlin/app/hohe/keyboard/amharic/dict/AmharicDictionaryFormat.kt`).

| Field | Value |
|---|---|
| Data version | `am-2026.11.24-wiki20261101-yimam2021` |
| Licence of the word list | CC BY-SA 4.0 (licence id `CC-BY-SA-4.0` in the file header) |
| Words | 100,000 (minimum combined frequency 55) |
| Size | 2,197,152 bytes |
| sha256 | `ff28f14c703d3bfd91aefc7ed6c6946278802ce578d0d102809b2637f3cf68d0` |
| Built | 2026-11-24 |

Attribution text (embedded in the file header and to be shown in Settings → About → Open-source licences):

> Word frequencies derived from Amharic Wikipedia (contributors, CC BY-SA 4.0, https://am.wikipedia.org) and the Amharic corpus of Yimam et al. 2021 (CC BY 4.0, https://doi.org/10.17632/dtywyf3sth.1). This word list is licensed CC BY-SA 4.0.

Desh data (`native_words.db`, the decoded TSV and anything under `evidence/`) was **not** used as input.

## Sources

### 1. Amharic Wikipedia

- URL: https://dumps.wikimedia.org/amwiki/latest/amwiki-latest-pages-articles.xml.bz2
- Dump date: 2026-11-01 (`Last-Modified: Sun, 01 Nov 2026 09:31:29 GMT`), downloaded 2026-11-24
- Licence: CC BY-SA 4.0 (and GFDL), Wikimedia Terms of Use attribution
- `.xml.bz2` sha256 `ae28342a7a4dbfcbe9a26f67d0335e0e76fd66c621fc1e5369ae9535f0ab8908`, 9,206,822 bytes
- Decompressed with Python's `bz2` module (the JVM has no bzip2 decoder; no new Gradle dependency)
- `.xml` sha256 `b1b0e410a7ff753c8ed63fc86cb16244bfa978be5c54aa52585fbc533f1948f0`, 56,403,599 bytes
- Used: `<text>` of namespace-0 pages only, category/file link lines skipped
- Counted: 2,020,048 word tokens, 217,549 types

### 2. Yimam et al. 2021, Amharic corpus

- Dataset: "Amharic corpus", Seid Muhie Yimam, Mendeley Data, DOI 10.17632/dtywyf3sth.1, published 2021-11-08
- Paper: "Introducing Various Semantic Models for Amharic: Experimentation and Evaluation with Multiple Tasks and Datasets", Future Internet 13(11):275, 2021
- Licence: CC BY 4.0 (read from the Mendeley public API `data_licence` field on 2026-11-24)
- File: `am-model-corpus.tar.gz`, downloaded 2026-11-24 via the Mendeley public-files URL
- sha256 `0f8bca1727762658b27ff461919318f6470606ad2f71fe365c08f6f8c5f998b1` (matches the hash Mendeley publishes), 415,952,751 bytes
- Contents used: `train.txt`, `valid.txt`, `test.txt` (all three, read directly from the archive)
- Counted: 90,643,423 word tokens, 1,404,363 types held at the end (lossy counting pruned types with count ≤ 2 to bound memory)

Raw downloads live outside the repo in `C:\dev\hohe-data\amharic\` and are never committed.

## Build

```
gradlew :tools:dictbuilder:run --args="amharic --wiki <amwiki-latest-pages-articles.xml> --text <am-model-corpus.tar.gz> --out <repo>/app/src/main/assets/dict/am_words.bin --data-version am-2026.11.24-wiki20261101-yimam2021 --max-words 100000"
```

Pipeline (`tools/dictbuilder/src/main/kotlin/app/hohe/keyboard/tools/dictbuilder/amharic/`):

1. Tokenise: maximal runs of Ethiopic syllables U+1200..U+135A. Gemination marks U+135D..U+135F are dropped. Ethiopic punctuation, digits and ASCII separate words. Runs glued to letters of another script are discarded, and so are words with fidels outside the Amharic table (ቐ, ዸ, ጘ, ...) or longer than 20 characters.
2. Count per source (`WordCounter`, lossy counting above 1.5M types).
3. Combine: `freq = yimam + round(0.5 × wikipedia)`. Wikipedia is weighted down to reduce encyclopedic bias, per `01-discovery/amharic-data-sources.md`.
4. Drop words with `freq < 3`, then keep the 100,000 most frequent.
5. Index each word by its consonant skeleton (engine spec §3.2), quantise `q = round(64·ln(freq+1))`, and write with a CRC-32.

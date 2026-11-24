# Data licences

The source code in this repository is licensed under the Apache License 2.0
(see [LICENSE](LICENSE)). The data files below are **not** covered by that
licence. Each keeps its own licence, which travels with the file.

| File | Content | Licence | Attribution |
|---|---|---|---|
| `app/src/main/assets/dict/am_words.bin` | Amharic word frequencies (100,000 words) | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | Derived from Amharic Wikipedia (contributors, CC BY-SA 4.0) and the Amharic corpus of Yimam et al. 2021 (CC BY 4.0, https://doi.org/10.17632/dtywyf3sth.1) |
| `app/src/main/assets/dict/en_main.bin` | English word list | Apache License 2.0 | Android Open Source Project |
| `app/src/main/assets/dict/emoji.tsv` | Emoji names and keywords | [Unicode License v3](https://www.unicode.org/license.txt) | Unicode CLDR (en, am) and Unicode emoji data |

## What this means for redistribution

- `am_words.bin` is a **ShareAlike** work. A redistributed copy of that file,
  or a modified version of it, must stay under CC BY-SA 4.0 and must carry the
  attribution above. The app's about screen shows that attribution.
- How the dictionary was built, with source dump dates and checksums, is recorded in
  [`tools/dictbuilder/sources/amharic/PROVENANCE.md`](tools/dictbuilder/sources/amharic/PROVENANCE.md).
- The dictionary was built only from the public sources listed in that file.
  No proprietary material was used as input.

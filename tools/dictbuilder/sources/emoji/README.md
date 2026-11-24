# Emoji data sources

- `emoji-test.txt`, Emoji 18.0 (dated 2026-04-30), from `https://unicode.org/Public/emoji/latest/`.
  Provides the fully-qualified emoji, panel groups, order and the emoji version of each.
- CLDR `release-49` `common/annotations/{en,am}.xml` and `common/annotationsDerived/{en,am}.xml`,
  saved locally as `annotations_en.xml`, `annotations_am.xml`, `annotationsDerived_en.xml`,
  `annotationsDerived_am.xml`. Provide names and keywords in English and Amharic.
- **Licence:** Unicode License v3 (SPDX `Unicode-3.0`). The CLDR repo `LICENSE` restricts only the
  UTS #35 specification text; "all other content in this repo is released under UNICODE LICENSE V3",
  and each annotations file carries `SPDX-License-Identifier: Unicode-3.0`. The copyright and
  permission notice must appear in the app's open-source licences screen.
- **Filtering:** fully-qualified emoji only, the `Component` group dropped, skin-tone variants
  (U+1F3FB..U+1F3FF) dropped from the base grid.
- **Raw download location:** `C:\dev\hohe-data\emoji\` (outside the repo).

Rebuild: `gradlew :tools:dictbuilder:run --args="english emoji"`.

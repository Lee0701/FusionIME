# Stroke data

These files are copied unmodified from Conway Stroke Data,
<https://github.com/stroke-input/stroke-input-data>, at commit
`9cc6aab87b41a7ffe81734520f2ae01a0fc898dc`.

| File | License |
|---|---|
| `sequence-characters.txt` | Copyright 2021–2026 Conway, [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| `ranking-simplified.txt`, `ranking-traditional.txt` | Public domain ([CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/)) |
| `characters-simplified.txt`, `characters-traditional.txt`, `characters-unlikely.txt` | Public domain ([CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/)) |

`GenStrokeDict` converts them into the binary dictionaries in
`app/stroke/src/main/res/raw`. The conversion changes the format only:
stroke sequences and their characters are kept as they are, and the
rankings and character lists are combined into one table.

To regenerate the dictionaries, build the host module and run
`ee.oyatl.ime.fusion.stroke.host.GenStrokeDictKt` with this directory and
`app/stroke/src/main/res/raw` as arguments.

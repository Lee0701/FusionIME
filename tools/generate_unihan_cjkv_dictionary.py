#!/usr/bin/env python3
"""Generate FusionIME's compact CJKV character dictionary from Unihan."""

from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import json
import struct
import time
import unicodedata
import urllib.error
import urllib.request
import zipfile
from collections import defaultdict
from pathlib import Path
from xml.etree import ElementTree


UNICODE_VERSION = "18.0.0"
DEFAULT_URL = f"https://www.unicode.org/Public/{UNICODE_VERSION}/ucd/Unihan.zip"
KANJIDIC2_URL = "https://www.edrdg.org/pub/Nihongo/kanjidic2.xml.gz"
USER_AGENT = "FusionIME-Unihan-Dictionary-Generator/1.0 (+https://github.com/Lee0701/FusionIME)"
MAGIC = 0x434A4B56  # CJKV
FORMAT_VERSION = 4

FIELDS = (
    "japanese_on",
    "japanese_kun",
    "mandarin",
    "cantonese",
    "korean",
    "vietnamese",
    "simplified",
    "traditional",
    "japanese_new",
    "japanese_old",
    "compatibility",
    "sources",
    "cangjie",
    "four_corner",
    "korean_hun_eum",
    "radical_strokes",
    "total_strokes",
    "middle_chinese",
    "fanqie",
    "semantic_variants",
    "specialized_semantic_variants",
    "z_variants",
    "definition",
)

READING_PROPERTIES = {
    "kJapanese",
    "kJapaneseOn",
    "kJapaneseKun",
    "kMandarin",
    "kTGHZ2013",
    "kXHC1983",
    "kHanyuPinlu",
    "kHanyuPinyin",
    "kCantonese",
    "kHangul",
    "kVietnamese",
    "kTang",
    "kFanqie",
}

VARIANT_PROPERTIES = {
    "kSimplifiedVariant": "simplified",
    "kTraditionalVariant": "traditional",
    "kJapaneseNewVariant": "japanese_new",
    "kJapaneseOldVariant": "japanese_old",
    "kSemanticVariant": "semantic_variants",
    "kSpecializedSemanticVariant": "specialized_semantic_variants",
    "kZVariant": "z_variants",
}

IRG_SOURCE_PROPERTIES = {
    "kIRG_GSource": "G",
    "kIRG_HSource": "H",
    "kIRG_JSource": "J",
    "kIRG_KPSource": "KP",
    "kIRG_KSource": "K",
    "kIRG_MSource": "M",
    "kIRG_SSource": "S",
    "kIRG_TSource": "T",
    "kIRG_UKSource": "UK",
    "kIRG_USource": "U",
    "kIRG_VSource": "V",
}

SELECTED_PROPERTIES = READING_PROPERTIES | set(VARIANT_PROPERTIES) | set(IRG_SOURCE_PROPERTIES) | {
    "kCompatibilityVariant",
    "kCangjie",
    "kFourCornerCode",
    "kRSUnicode",
    "kTotalStrokes",
    "kDefinition",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--unihan-zip",
        type=Path,
        help="Use an existing Unihan.zip instead of downloading it.",
    )
    parser.add_argument("--url", default=DEFAULT_URL, help="Unihan.zip download URL.")
    parser.add_argument(
        "--kanjidic2-gzip",
        type=Path,
        help="Use an existing kanjidic2.xml.gz instead of downloading it.",
    )
    parser.add_argument("--kanjidic2-url", default=KANJIDIC2_URL, help="KANJIDIC2 download URL.")
    parser.add_argument(
        "--korean-dictionary",
        type=Path,
        default=Path("data/ko_dict_all.tsv"),
        help="Korean dictionary TSV used to derive one-character hun/eum labels.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("app/src/main/assets/unihan_cjkv.bin"),
        help="Output binary asset.",
    )
    parser.add_argument(
        "--metadata",
        type=Path,
        default=Path("app/src/main/assets/unihan_cjkv_metadata.json"),
        help="Output generation metadata.",
    )
    return parser.parse_args()


def download(url: str, attempts: int = 3) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    last_error: Exception | None = None
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read()
        except (OSError, urllib.error.URLError, urllib.error.HTTPError) as error:
            last_error = error
            if attempt + 1 < attempts:
                time.sleep(2**attempt)
    raise RuntimeError(f"Failed to download {url}: {last_error}")


def is_hiragana(value: str) -> bool:
    return any("\u3041" <= char <= "\u309f" for char in value)


def is_katakana(value: str) -> bool:
    return any("\u30a0" <= char <= "\u30ff" for char in value)


def unique(values: list[str]) -> list[str]:
    return list(dict.fromkeys(value for value in values if value))


def parse_variant_values(raw: str) -> str:
    characters: list[str] = []
    for value in raw.split():
        code_point_text = value.split("<", 1)[0]
        if not code_point_text.startswith("U+"):
            continue
        characters.append(chr(int(code_point_text[2:], 16)))
    return " ".join(unique(characters))


def parse_mandarin(props: dict[str, str]) -> str:
    """kMandarin holds one customary reading; the other readings of a polyphone (重 zhòng chóng)
    are in the dictionary-derived fields. Hanyu Da Zidian's kHanyuPinyin, which also lists
    archaic readings, is used only when no modern dictionary covers the character."""
    readings = props.get("kMandarin", "").split()
    modern = []
    for value in props.get("kTGHZ2013", "").split() + props.get("kXHC1983", "").split():
        modern += value.split(":", 1)[1].split(",")
    # kHanyuPinlu: zhòng(1823) chóng(329), already in descending frequency
    modern += [value.split("(", 1)[0] for value in props.get("kHanyuPinlu", "").split()]
    if not modern:
        for value in props.get("kHanyuPinyin", "").split():
            modern += value.split(":", 1)[1].split(",")
    return " ".join(unique(readings + modern))


ROMAJI_KANA = {
    **{v: k for k, v in {
        "あ": "a", "い": "i", "う": "u", "え": "e", "お": "o",
        "か": "ka", "き": "ki", "く": "ku", "け": "ke", "こ": "ko",
        "さ": "sa", "し": "shi", "す": "su", "せ": "se", "そ": "so",
        "た": "ta", "ち": "chi", "つ": "tsu", "て": "te", "と": "to",
        "な": "na", "に": "ni", "ぬ": "nu", "ね": "ne", "の": "no",
        "は": "ha", "ひ": "hi", "ふ": "fu", "へ": "he", "ほ": "ho",
        "ゔ": "vu",
        "ま": "ma", "み": "mi", "む": "mu", "め": "me", "も": "mo",
        "や": "ya", "ゆ": "yu", "よ": "yo",
        "ら": "ra", "り": "ri", "る": "ru", "れ": "re", "ろ": "ro",
        "わ": "wa", "ゐ": "wi", "ゑ": "we", "を": "wo",
        "が": "ga", "ぎ": "gi", "ぐ": "gu", "げ": "ge", "ご": "go",
        "ざ": "za", "じ": "ji", "ず": "zu", "ぜ": "ze", "ぞ": "zo",
        "だ": "da", "ぢ": "di", "づ": "du", "で": "de", "ど": "do",
        "ば": "ba", "び": "bi", "ぶ": "bu", "べ": "be", "ぼ": "bo",
        "ぱ": "pa", "ぴ": "pi", "ぷ": "pu", "ぺ": "pe", "ぽ": "po",
        "きゃ": "kya", "きゅ": "kyu", "きょ": "kyo", "しゃ": "sha", "しゅ": "shu", "しょ": "sho",
        "ちゃ": "cha", "ちゅ": "chu", "ちょ": "cho", "にゃ": "nya", "にゅ": "nyu", "にょ": "nyo",
        "ひゃ": "hya", "ひゅ": "hyu", "ひょ": "hyo", "みゃ": "mya", "みゅ": "myu", "みょ": "myo",
        "りゃ": "rya", "りゅ": "ryu", "りょ": "ryo", "ぎゃ": "gya", "ぎゅ": "gyu", "ぎょ": "gyo",
        "じゃ": "ja", "じゅ": "ju", "じょ": "jo", "びゃ": "bya", "びゅ": "byu", "びょ": "byo",
        "ぴゃ": "pya", "ぴゅ": "pyu", "ぴょ": "pyo",
    }.items()},
}


# Spellings outside Hepburn that Unihan also uses (HANETSUKUROHU, SUHU).
ROMAJI_KANA.update({"hu": "ふ", "si": "し", "ti": "ち", "tu": "つ", "zi": "じ"})


def romaji_to_hiragana(value: str) -> str | None:
    """Unihan's kJapaneseKun/kJapaneseOn are in uppercase Hepburn (SAMATAGERARERU). Returns None
    for anything that does not parse, so a reading is never shown half converted."""
    text = value.lower()
    out = []
    i = 0
    while i < len(text):
        if text[i] == "n" and (i + 1 == len(text) or text[i + 1] not in "aiueoy"):
            out.append("ん")
            i += 1
            continue
        if i + 1 < len(text) and text[i] == text[i + 1] and text[i] not in "aiueon":
            out.append("っ")
            i += 1
            continue
        if text.startswith("tch", i):
            out.append("っ")
            i += 1
            continue
        for length in (3, 2, 1):
            kana = ROMAJI_KANA.get(text[i:i + length])
            if kana:
                out.append(kana)
                i += length
                break
        else:
            return None
    return "".join(out)


def hiragana_to_katakana(value: str) -> str:
    return "".join(chr(ord(c) + 0x60) if "\u3041" <= c <= "\u3096" else c for c in value)


def romaji_readings(raw: str, katakana: bool) -> str:
    readings = []
    for value in raw.split():
        kana = romaji_to_hiragana(value)
        if kana is None:
            continue
        readings.append(hiragana_to_katakana(kana) if katakana else kana)
    return " ".join(unique(readings))


def load_radical_forms(raw_entries: dict[int, dict[str, str]]) -> dict[str, str]:
    """Maps a kRSUnicode radical (46, 120', 212'') to the character that is that radical.

    A plain number is a Kangxi radical, whose compatibility character U+2F00.. normalizes to the
    unified ideograph. A radical with apostrophes is a simplified or other variant form; the
    character for it is the one Unihan lists under that radical with no residual strokes."""
    forms = {str(n): unicodedata.normalize("NFKC", chr(0x2F00 + n - 1)) for n in range(1, 215)}
    candidates: dict[str, list[int]] = defaultdict(list)
    for code_point, props in raw_entries.items():
        for value in props.get("kRSUnicode", "").split():
            radical, _, residual = value.partition(".")
            if "'" in radical and residual == "0":
                candidates[radical].append(code_point)
    for radical, code_points in candidates.items():
        # Prefer the URO (4E00..9FFF), which every CJK font covers, then the lowest code point.
        forms[radical] = chr(min(code_points, key=lambda cp: (not 0x4E00 <= cp <= 0x9FFF, cp)))
    return forms


def describe_radical_strokes(raw: str, radical_forms: dict[str, str]) -> str:
    items = []
    for value in raw.split():
        radical, _, residual = value.partition(".")
        items.append(f"{radical_forms.get(radical, radical)}.{residual}")
    return " ".join(items)


def parse_hangul_values(raw: str) -> str:
    readings = [value.split(":", 1)[0] for value in raw.split()]
    return " ".join(unique(readings))


def load_kanjidic_kun(source_bytes: bytes) -> dict[int, list[str]]:
    readings: dict[int, list[str]] = {}
    with gzip.GzipFile(fileobj=io.BytesIO(source_bytes)) as source:
        for _, element in ElementTree.iterparse(source, events=("end",)):
            if element.tag != "character":
                continue
            literal = element.findtext("literal")
            if not literal:
                element.clear()
                continue
            values = [
                reading.text
                for reading in element.findall("./reading_meaning/rmgroup/reading")
                if reading.get("r_type") == "ja_kun" and reading.text
            ]
            if values:
                readings[ord(literal)] = unique(values)
            element.clear()
    return readings


def add_okurigana_boundaries(values: list[str], kanjidic_values: list[str]) -> list[str]:
    marked_by_plain: dict[str, str] = {}
    for value in kanjidic_values:
        plain = value.replace(".", "").strip("-")
        if plain:
            marked_by_plain.setdefault(plain, value.replace(".", "-"))
    return [marked_by_plain.get(value, value) for value in values]


def load_korean_hun_eum(path: Path) -> dict[int, list[str]]:
    values: dict[int, list[tuple[int, str]]] = defaultdict(list)
    with path.open(encoding="utf-8") as source:
        for line in source:
            columns = line.rstrip("\n").split("\t")
            if len(columns) != 5:
                continue
            _, hanja, frequency, extra, _ = columns
            if len(hanja) != 1 or not extra.strip():
                continue
            values[ord(hanja)].append((int(frequency), extra.strip()))
    return {
        code_point: unique([label for _, label in sorted(labels, reverse=True)])
        for code_point, labels in values.items()
    }


def load_properties(archive: zipfile.ZipFile) -> dict[int, dict[str, str]]:
    raw_entries: dict[int, dict[str, str]] = defaultdict(dict)
    for filename in archive.namelist():
        if not filename.startswith("Unihan_") or not filename.endswith(".txt"):
            continue
        with archive.open(filename) as source:
            for byte_line in source:
                if byte_line.startswith(b"#") or not byte_line.strip():
                    continue
                code_text, prop, value = byte_line.decode("utf-8").rstrip("\n").split("\t", 2)
                if prop not in SELECTED_PROPERTIES:
                    continue
                raw_entries[int(code_text[2:], 16)][prop] = unicodedata.normalize("NFC", value)
    return raw_entries


def normalize_entries(
    raw_entries: dict[int, dict[str, str]],
    kanjidic_kun: dict[int, list[str]],
    korean_hun_eum: dict[int, list[str]],
) -> dict[int, dict[str, str]]:
    entries: dict[int, dict[str, str]] = {}
    radical_forms = load_radical_forms(raw_entries)
    for code_point, props in raw_entries.items():
        entry: dict[str, str] = {}
        japanese = props.get("kJapanese", "").split()
        japanese_on = unique(value for value in japanese if is_katakana(value))
        japanese_kun = unique(value for value in japanese if is_hiragana(value))
        entry["japanese_on"] = " ".join(japanese_on) or romaji_readings(
            props.get("kJapaneseOn", ""), katakana=True
        )
        kanjidic = kanjidic_kun.get(code_point, [])
        japanese_kun = add_okurigana_boundaries(japanese_kun, kanjidic)
        # Falls back to KANJIDIC2, then to Unihan's romanized kun converted to kana.
        entry["japanese_kun"] = (
            " ".join(japanese_kun)
            or " ".join(unique([value.replace(".", "-") for value in kanjidic]))
            or romaji_readings(props.get("kJapaneseKun", ""), katakana=False)
        )
        entry["mandarin"] = parse_mandarin(props)
        entry["cantonese"] = props.get("kCantonese", "")
        entry["korean"] = parse_hangul_values(props.get("kHangul", ""))
        entry["vietnamese"] = props.get("kVietnamese", "")
        for prop, field in VARIANT_PROPERTIES.items():
            entry[field] = parse_variant_values(props.get(prop, ""))
        entry["compatibility"] = parse_variant_values(props.get("kCompatibilityVariant", ""))
        entry["sources"] = " ".join(
            code for prop, code in IRG_SOURCE_PROPERTIES.items() if props.get(prop)
        )
        entry["cangjie"] = props.get("kCangjie", "")
        entry["four_corner"] = props.get("kFourCornerCode", "")
        entry["korean_hun_eum"] = " · ".join(korean_hun_eum.get(code_point, []))
        entry["radical_strokes"] = describe_radical_strokes(props.get("kRSUnicode", ""), radical_forms)
        entry["total_strokes"] = props.get("kTotalStrokes", "")
        entry["middle_chinese"] = props.get("kTang", "")
        entry["fanqie"] = props.get("kFanqie", "")
        entry["definition"] = props.get("kDefinition", "")
        entry = {field: value for field, value in entry.items() if value}
        if entry:
            entries[code_point] = entry
    return entries


def encode_record(entry: dict[str, str]) -> bytes:
    field_mask = 0
    payload = bytearray()
    for index, field in enumerate(FIELDS):
        value = entry.get(field)
        if not value:
            continue
        encoded = value.encode("utf-8")
        if len(encoded) > 0xFFFF:
            raise ValueError(f"Field {field} exceeds binary format limit")
        field_mask |= 1 << index
        payload += struct.pack(">H", len(encoded))
        payload += encoded
    return struct.pack(">I", field_mask) + payload


def encode_dictionary(entries: dict[int, dict[str, str]]) -> bytes:
    records = bytearray()
    index = bytearray()
    for code_point in sorted(entries):
        record = encode_record(entries[code_point])
        index += struct.pack(">II", code_point, len(records))
        records += record
    header = struct.pack(">III", MAGIC, FORMAT_VERSION, len(entries))
    return header + index + records


def validate_samples(entries: dict[int, dict[str, str]]) -> None:
    expected = {
        0x4E00: {"mandarin": "yī", "cantonese": "jat1", "korean": "일", "vietnamese": "nhất"},
        0x6F22: {"japanese_on": "カン", "mandarin": "hàn", "korean": "한"},
        0x66F8: {"simplified": "书"},
        0x5B66: {"traditional": "學", "japanese_old": "學"},
        0x98DF: {"japanese_kun": "た-べる"},
        0x91CD: {"mandarin": "chóng", "radical_strokes": "里.2"},
        0x4E02: {"japanese_kun": "さまた-げられる"},
        0x7E9F: {"radical_strokes": "纟.0"},
        0x91CE: {"definition": "open country, field; wilderness"},
        0x5C71: {
            "cangjie": "U",
            "four_corner": "2277.0",
            "korean_hun_eum": "뫼 산",
            "radical_strokes": "山.0",
            "total_strokes": "3",
            "middle_chinese": "*shrɛn",
            "fanqie": "所閒",
        },
        0x4E00: {"semantic_variants": "壹"},
        0xF900: {"compatibility": "豈", "sources": "K"},
    }
    for code_point, fields in expected.items():
        actual = entries.get(code_point, {})
        for field, value in fields.items():
            actual_value = actual.get(field, "")
            matches = value in actual_value if field in ("korean_hun_eum", "definition") else value in actual_value.split()
            if not matches:
                raise ValueError(f"Validation failed for U+{code_point:04X} {field}: {actual.get(field)!r}")


def main() -> None:
    args = parse_args()
    source_bytes = args.unihan_zip.read_bytes() if args.unihan_zip else download(args.url)
    kanjidic2_bytes = (
        args.kanjidic2_gzip.read_bytes()
        if args.kanjidic2_gzip
        else download(args.kanjidic2_url)
    )
    kanjidic_kun = load_kanjidic_kun(kanjidic2_bytes)
    korean_hun_eum = load_korean_hun_eum(args.korean_dictionary)
    with zipfile.ZipFile(io.BytesIO(source_bytes)) as archive:
        entries = normalize_entries(load_properties(archive), kanjidic_kun, korean_hun_eum)
    validate_samples(entries)

    output = encode_dictionary(entries)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(output)

    metadata = {
        "format": FORMAT_VERSION,
        "unicode_version": UNICODE_VERSION,
        "source_url": args.url,
        "source_sha256": hashlib.sha256(source_bytes).hexdigest(),
        "kanjidic2_url": args.kanjidic2_url,
        "kanjidic2_sha256": hashlib.sha256(kanjidic2_bytes).hexdigest(),
        "kanjidic2_license": "CC BY-SA 4.0",
        "korean_dictionary_path": str(args.korean_dictionary),
        "korean_dictionary_sha256": hashlib.sha256(args.korean_dictionary.read_bytes()).hexdigest(),
        "output_sha256": hashlib.sha256(output).hexdigest(),
        "entry_count": len(entries),
        "size_bytes": len(output),
        "fields": list(FIELDS),
    }
    args.metadata.parent.mkdir(parents=True, exist_ok=True)
    args.metadata.write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

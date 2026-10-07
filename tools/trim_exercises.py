#!/usr/bin/env python3
"""Trims the exercise catalog asset to what the app reads.

The upstream dataset (https://github.com/hasaneyldrm/exercises-dataset) ships instructions in
ten languages; the app seeds Spanish and keeps English for a future English catalog. Dropping
the rest takes the asset from ~17.5 MB to ~3 MB, which is parsed on first launch and shipped
in every APK. Re-run after updating the dataset:

    python3 tools/trim_exercises.py path/to/upstream/exercises.json \
        core/core-data/src/main/assets/exercises.json
"""
import json
import sys

KEEP_FIELDS = [
    "id", "name", "category", "body_part", "equipment", "instructions", "instruction_steps",
    "muscle_group", "secondary_muscles", "target", "image", "gif_url", "media_id",
]
KEEP_LANGUAGES = ("es", "en")


def trim(entry):
    out = {}
    for field in KEEP_FIELDS:
        if field not in entry:
            continue
        value = entry[field]
        if field in ("instructions", "instruction_steps") and isinstance(value, dict):
            value = {lang: value[lang] for lang in KEEP_LANGUAGES if lang in value}
        out[field] = value
    return out


def main(src, dst):
    with open(src, encoding="utf-8") as f:
        catalog = json.load(f)
    missing = [e.get("id") for e in catalog if "es" not in (e.get("instruction_steps") or e.get("instructions") or {})]
    if missing:
        sys.exit(f"{len(missing)} exercises have no Spanish instructions, e.g. {missing[:5]}")
    with open(dst, "w", encoding="utf-8") as f:
        json.dump([trim(e) for e in catalog], f, ensure_ascii=False, separators=(",", ":"))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])

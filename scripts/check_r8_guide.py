#!/usr/bin/env python3
"""Release-build gate for the Claim Guide (docs/Plan/rule_cards/16_guide_phase_plan.md, "R8 and release-build safety").

Green debug tests do not prove the minified app can read the Guide. It checks the `minifiedTest` build: the release R8
and resource-shrink config with the Guide reachable, which is what release becomes when LaunchFlags.GUIDE_ENABLED flips
in phase E9. Since the E9 flip release has the Guide too, and the same checks pass on it
(`--mapping .../mapping/release/mapping.txt --apk .../apk/release/composeApp-release.apk`). There is no Guide keep
rule: reachability plus kotlinx-serialization's bundled rules keep the models, and this gate proves it. After
`:composeApp:assembleMinifiedTest`, this fails unless:
  1. every @Serializable class in guide/model/GuideModels.kt survives R8 with its generated `$$serializer` and its
     `Companion.serializer()` (read from the R8 mapping; the model list comes from the source, not a copy), and
  2. the release APK carries the Guide bundle byte-identical to the source file.

Usage: check_r8_guide.py [--mapping PATH] [--apk PATH]   (defaults: the minifiedTest outputs of composeApp)
"""
import argparse
import glob
import os
import re
import sys
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
PACKAGE = "com.payslipmax.pdfparser.guide.model"
MODELS_SRC = os.path.join(ROOT, "shared/src/commonMain/kotlin/com/payslipmax/pdfparser/guide/model/GuideModels.kt")
BUNDLE_SRC = os.path.join(ROOT, "composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json")
BUNDLE_SUFFIX = "/files/guide/guide_bundle.json"
DEFAULT_MAPPING = os.path.join(ROOT, "composeApp/build/outputs/mapping/minifiedTest/mapping.txt")
DEFAULT_APK_GLOB = os.path.join(ROOT, "composeApp/build/outputs/apk/minifiedTest/*.apk")

SERIALIZABLE_CLASS = re.compile(r"@Serializable\s+(?:data\s+)?class\s+(\w+)")
CLASS_LINE = re.compile(r"^(\S+) -> \S+:$")
METHOD_NAME = re.compile(r"\s(\w+)\(")


def serializable_models(kotlin_source):
    return SERIALIZABLE_CLASS.findall(kotlin_source)


def _classes(mapping_text):
    """Original class name -> list of its member lines, from an R8 mapping file."""
    classes, current = {}, None
    for line in mapping_text.splitlines():
        if line.startswith("#"):
            continue
        m = CLASS_LINE.match(line)
        if m:
            current = classes.setdefault(m.group(1), [])
        elif current is not None and line.startswith(" "):
            current.append(line)
    return classes


def check_mapping(mapping_text, models):
    classes, problems = _classes(mapping_text), []
    for name in models:
        fqcn = f"{PACKAGE}.{name}"
        for cls in (fqcn, f"{fqcn}$$serializer"):
            if cls not in classes:
                problems.append(f"{cls} was removed by R8")
        companion = classes.get(f"{fqcn}$Companion")
        if companion is None or not any(METHOD_NAME.search(m) and METHOD_NAME.search(m).group(1) == "serializer" for m in companion):
            problems.append(f"{fqcn}.Companion.serializer() was removed by R8")
    return problems


def check_apk(apk_path, bundle_bytes):
    with zipfile.ZipFile(apk_path) as z:
        entries = [n for n in z.namelist() if n.startswith("assets/composeResources/") and n.endswith(BUNDLE_SUFFIX)]
        if len(entries) != 1:
            return [f"Guide bundle not in {os.path.basename(apk_path)} (found {len(entries)} copies)"]
        if z.read(entries[0]) != bundle_bytes:
            return [f"{entries[0]} differs from the source bundle"]
    return []


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--mapping", default=DEFAULT_MAPPING)
    parser.add_argument("--apk")
    args = parser.parse_args()
    apks = [args.apk] if args.apk else glob.glob(DEFAULT_APK_GLOB)
    if not os.path.exists(args.mapping) or len(apks) != 1:
        sys.exit(f"Run ./gradlew :composeApp:assembleMinifiedTest first (mapping: {args.mapping}, apks: {apks})")
    with open(MODELS_SRC, encoding="utf-8") as fh:
        models = serializable_models(fh.read())
    with open(args.mapping, encoding="utf-8") as fh:
        problems = check_mapping(fh.read(), models)
    with open(BUNDLE_SRC, "rb") as fh:
        problems += check_apk(apks[0], fh.read())
    for p in problems:
        print(f"❌ {p}")
    if problems:
        sys.exit(1)
    print(f"✅ Guide R8 gate: {len(models)} models and serializers kept; bundle shipped byte-identical")


if __name__ == "__main__":
    main()

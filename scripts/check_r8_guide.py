#!/usr/bin/env python3
"""Release-build gate for the Claim Guide (docs/Plan/rule_cards/16_guide_phase_plan.md, "R8 and release-build safety").

Green debug tests do not prove the minified app can read the Guide. This checks the build that ships, `release`
(R8 and resource shrinking, Guide reachable behind LaunchFlags.GUIDE_ENABLED). There is no Guide keep rule:
reachability plus kotlinx-serialization's bundled rules keep the models, and this gate proves it. After
`:composeApp:assembleRelease`, this fails unless:
  1. every @Serializable class in the model folders (guide/model, which holds the Part II lookup models, and part2/, where the Part II
     payload and backup models live) survives R8 with its generated `$$serializer` and its
     `Companion.serializer()` (read from the R8 mapping; the model list comes from the source, not a copy), and
  2. the Room classes generated for the database and the Part II DAO (`PayslipDatabase_Impl`, `Part2Dao_Impl`) survive, and so does
     the Part II reminder worker, under its own name (WorkManager stores it by name), and
  3. the release APK carries the Guide bundle byte-identical to the source file.
The `minifiedTest` build type exists only for the device smoke (scripts/run_guide_minified_smoke.sh), which builds it.

Usage: check_r8_guide.py [--mapping PATH] [--apk PATH]   (defaults: the release outputs of composeApp)
"""
import argparse
import glob
import os
import re
import sys
import zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ROOT_PACKAGE = "com.payslipmax.pdfparser"
PACKAGE = f"{ROOT_PACKAGE}.guide.model"
KOTLIN_ROOT = os.path.join(ROOT, "shared/src/commonMain/kotlin/com/payslipmax/pdfparser")
MODELS_DIR = os.path.join(KOTLIN_ROOT, "guide/model")
LAUNCH_FLAGS = os.path.join(KOTLIN_ROOT, "subscription/LaunchFlags.kt")
# Folders (under KOTLIN_ROOT) whose @Serializable classes must survive R8; a file's package is its folder's path.
MODEL_FOLDERS = ("guide/model", "part2")
BUNDLE_SRC = os.path.join(ROOT, "composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json")
BUNDLE_SUFFIX = "/files/guide/guide_bundle.json"
DEFAULT_MAPPING = os.path.join(ROOT, "composeApp/build/outputs/mapping/release/mapping.txt")
DEFAULT_APK_GLOB = os.path.join(ROOT, "composeApp/build/outputs/apk/release/*.apk")
BUILD_HINT = "Run ./gradlew :composeApp:assembleRelease -PallowPlaceholderGemmaModel=true first"

# A visibility modifier does not hide a model: Part II payloads are `internal` (a storage detail, not API).
SERIALIZABLE_CLASS = re.compile(r"@Serializable\s+(?:(?:public|internal|private)\s+)?(?:data\s+)?class\s+(\w+)")
# Any @Serializable declaration, whatever its kind or modifiers: used to find the ones SERIALIZABLE_CLASS (the kind this gate proves) misses.
ANY_SERIALIZABLE = re.compile(
    r"@Serializable(?:\([^)]*\))?\s+(?:(?:public|internal|private|data|sealed|enum|abstract|open|value)\s+)*(?:class|object|interface)\s+(\w+)"
)
CLASS_LINE = re.compile(r"^(\S+) -> \S+:$")
CLASS_RENAME = re.compile(r"^(\S+) -> (\S+):$")
METHOD_NAME = re.compile(r"\s(\w+)\(")


def serializable_models(kotlin_source):
    return SERIALIZABLE_CLASS.findall(kotlin_source)


def models_in(directory):
    """Every @Serializable model in the folder, so splitting GuideModels.kt can never drop a model from the gate."""
    models = []
    for name in sorted(os.listdir(directory)):
        if name.endswith(".kt"):
            with open(os.path.join(directory, name), encoding="utf-8") as fh:
                models += serializable_models(fh.read())
    return models


def _declared(kotlin_root, folder, pattern):
    """Fully qualified name of every declaration `pattern` finds under one folder (subfolders too), each in its own file's package."""
    found = []
    base = os.path.join(kotlin_root, *folder.split("/"))
    for current, dirs, names in os.walk(base):
        dirs.sort()
        package = ".".join([ROOT_PACKAGE, *os.path.relpath(current, kotlin_root).split(os.sep)])
        for name in sorted(n for n in names if n.endswith(".kt")):
            with open(os.path.join(current, name), encoding="utf-8") as fh:
                found += [f"{package}.{model}" for model in pattern.findall(fh.read())]
    return found


def qualified_models(kotlin_root, folders):
    """Fully qualified name of every @Serializable model under the folders (subfolders too), each in its own file's package."""
    return [model for folder in folders for model in _declared(kotlin_root, folder, SERIALIZABLE_CLASS)]


def unlisted_serializables(kotlin_root, folders):
    """@Serializable declarations of a kind this gate does not prove (sealed class, enum, object, interface, custom serializer): they
    would otherwise ship unchecked, so a test and the gate itself fail until the gate is extended to cover them."""
    listed = set(qualified_models(kotlin_root, folders))
    return [name for folder in folders for name in _declared(kotlin_root, folder, ANY_SERIALIZABLE) if name not in listed]


def summary(kotlin_root, folders):
    """"guide/model: 26 models, part2: 1 model": the count the gate proves, per folder."""
    parts = []
    for folder in folders:
        count = len(_declared(kotlin_root, folder, SERIALIZABLE_CLASS)) if os.path.isdir(os.path.join(kotlin_root, *folder.split("/"))) else 0
        parts.append(f"{folder}: {count} model{'' if count == 1 else 's'}")
    return ", ".join(parts)


def absent_folders(kotlin_root, folders):
    """Registered folders that do not exist yet, so the gate can say so instead of passing over them in silence."""
    return [f for f in folders if not os.path.isdir(os.path.join(kotlin_root, *f.split("/")))]


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
        fqcn = name if "." in name else f"{PACKAGE}.{name}"
        for cls in (fqcn, f"{fqcn}$$serializer"):
            if cls not in classes:
                problems.append(f"{cls} was removed by R8")
        companion = classes.get(f"{fqcn}$Companion")
        if companion is None or not any(METHOD_NAME.search(m) and METHOD_NAME.search(m).group(1) == "serializer" for m in companion):
            problems.append(f"{fqcn}.Companion.serializer() was removed by R8")
    return problems


# Room builds these by generated code, never by a name the compiler sees, so R8 can drop them in release only: the database
# and the Part II DAO it hands out. A DAO added later joins this list in the phase that adds it. WorkManager starts the Part II
# reminder worker from a class NAME it stored, so the worker is required too (and must keep its name: UNRENAMED_CLASSES).
REMINDER_WORKER = f"{ROOT_PACKAGE}.reminders.ReminderWorker"
REQUIRED_CLASSES = (f"{ROOT_PACKAGE}.database.PayslipDatabase_Impl", f"{ROOT_PACKAGE}.part2.data.Part2Dao_Impl", REMINDER_WORKER)
UNRENAMED_CLASSES = (REMINDER_WORKER,)


PART2_FLAG = re.compile(r"const\s+val\s+PART2_ENABLED\s*:\s*Boolean\s*=\s*(true|false)\b")


def part2_enabled(launch_flags_text):
    """Whether LaunchFlags.PART2_ENABLED is on. Unreadable fails loudly: guessing "off" would skip the worker check without a word."""
    m = PART2_FLAG.search(launch_flags_text)
    if m is None:
        raise ValueError("LaunchFlags.PART2_ENABLED not found in the shape this gate reads")
    return m.group(1) == "true"


def _required(part2_on):
    """The reminder worker is reachable only through part2Module, so a build with the flag off rightly has R8 drop it with the rest of Part II."""
    return [c for c in REQUIRED_CLASSES if part2_on or c != REMINDER_WORKER]


def check_required_classes(mapping_text, part2_enabled=True):
    classes = _classes(mapping_text)
    return [f"{name} was removed by R8" for name in _required(part2_enabled) if name not in classes]


def check_unrenamed_classes(mapping_text, part2_enabled=True):
    """A class WorkManager stored by name must still have that name after R8 (a rename would strand every reminder scheduled before an update).
    A removed class is check_required_classes' to report, so it is not reported twice."""
    renamed = {m.group(1): m.group(2) for m in map(CLASS_RENAME.match, mapping_text.splitlines()) if m}
    return [f"{name} was renamed to {renamed[name]} by R8, but WorkManager stores it by name" for name in UNRENAMED_CLASSES if (part2_enabled or name != REMINDER_WORKER) and renamed.get(name, name) != name]


def empty_folders(kotlin_root, folders):
    """Registered folders that exist but hold no @Serializable model: a broken walk (a moved folder, a changed pattern), not a pass."""
    return [f for f in folders if os.path.isdir(os.path.join(kotlin_root, *f.split("/"))) and not _declared(kotlin_root, f, SERIALIZABLE_CLASS)]


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
        sys.exit(f"{BUILD_HINT} (mapping: {args.mapping}, apks: {apks})")
    models = qualified_models(KOTLIN_ROOT, MODEL_FOLDERS)
    for folder in absent_folders(KOTLIN_ROOT, MODEL_FOLDERS):
        print(f"ℹ️  {folder}/ does not exist yet, so it has no models to check")
    with open(args.mapping, encoding="utf-8") as fh:
        mapping_text = fh.read()
    with open(LAUNCH_FLAGS, encoding="utf-8") as fh:
        part2_on = part2_enabled(fh.read())
    if not part2_on:
        print("ℹ️  PART2_ENABLED is false, so R8 drops the reminder worker with the rest of Part II; run this gate on a flag-on build (Phase 10) to prove it")
    problems = check_mapping(mapping_text, models) + check_required_classes(mapping_text, part2_on) + check_unrenamed_classes(mapping_text, part2_on)
    problems += [f"{folder}/ holds no @Serializable model, so the gate proved nothing there" for folder in empty_folders(KOTLIN_ROOT, MODEL_FOLDERS)]
    problems += [f"{name} is @Serializable but of a kind this gate does not check" for name in unlisted_serializables(KOTLIN_ROOT, MODEL_FOLDERS)]
    with open(BUNDLE_SRC, "rb") as fh:
        problems += check_apk(apks[0], fh.read())
    for p in problems:
        print(f"❌ {p}")
    if problems:
        sys.exit(1)
    print(
        f"✅ Guide and Part II R8 gate: {len(models)} models and serializers kept ({summary(KOTLIN_ROOT, MODEL_FOLDERS)}); "
        f"{', '.join(c.rsplit('.', 1)[1] for c in _required(part2_on))} kept; bundle shipped byte-identical"
    )


if __name__ == "__main__":
    main()

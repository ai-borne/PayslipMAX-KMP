#!/usr/bin/env python3
"""Runs the shared and composeApp debug unit tests with LaunchFlags.PART2_ENABLED switched ON (docs/Plan/13_OrdersBank_PhasePlan.md, Phase 0).

Every flag-off gate misses a test that quietly assumes Part II is off; the old Part II branch's first flag-on run found 23 such failures. This copies what
git tracks into a throwaway directory (no keystore, no model, no build output), switches the flag and its committed expectation
(Part2FlagExpectation) to true there, runs both suites, and deletes the copy. The repo itself is never touched. Once the repo's own flag is on
(after the flip commit) there is nothing to switch and it says so. Run from anywhere:
python3 scripts/run_flag_on_tests.py
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile

LAUNCH_FLAGS = "shared/src/commonMain/kotlin/com/payslipmax/pdfparser/subscription/LaunchFlags.kt"
EXPECTATION = "shared-test-fixtures/src/commonMain/kotlin/com/payslipmax/pdfparser/testing/Part2FlagExpectation.kt"
GRADLE_TASKS = [":shared:testDebugUnitTest", ":composeApp:testDebugUnitTest"]
# The one ignored file Gradle needs on a developer machine (the SDK path); CI sets ANDROID_HOME instead.
ALSO_COPY = ("local.properties",)
FLAG_OFF = re.compile(r"(const val PART2_ENABLED: Boolean = )false")
REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


class FlipError(Exception):
    pass


def tracked_files(src):
    out = subprocess.run(["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard"], cwd=src, check=True, capture_output=True).stdout
    return [name for name in out.decode("utf-8").split("\0") if name]


def copy_tracked(src, dst):
    """Copies what git tracks (and any untracked file that is not ignored) from src to dst, plus local.properties; returns the names copied."""
    copied = []
    for name in tracked_files(src) + [n for n in ALSO_COPY if os.path.isfile(os.path.join(src, n))]:
        source = os.path.join(src, name)
        if not os.path.isfile(source):  # tracked but deleted in the working tree
            continue
        target = os.path.join(dst, name)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        shutil.copy2(source, target)
        copied.append(name)
    return copied


def state(root):
    """'off' when the flag and its committed expectation are both false under root, 'on' when both are true; a mixed or malformed pair raises."""
    found = set()
    for rel in (LAUNCH_FLAGS, EXPECTATION):
        with open(os.path.join(root, rel), encoding="utf-8") as fh:
            values = re.findall(r"const val PART2_ENABLED: Boolean = (\w+)", fh.read())
        if len(values) != 1 or values[0] not in ("true", "false"):
            raise FlipError(f"{rel} must hold exactly one 'const val PART2_ENABLED: Boolean = true|false' line")
        found.add(values[0])
    if len(found) != 1:
        raise FlipError("LaunchFlags.kt and Part2FlagExpectation.kt disagree: the flip commit edits both together")
    return "on" if found == {"true"} else "off"


def flip(root):
    """Switches the flag and its committed expectation to true under root. Refuses a git checkout, and any file that does not hold the
    flag line exactly once and set to false, so a rename can never turn this into a flag-off run that passes."""
    if os.path.exists(os.path.join(root, ".git")):
        raise FlipError("refusing to edit a git checkout; flip a throwaway copy")
    changed = []
    for rel in (LAUNCH_FLAGS, EXPECTATION):
        path = os.path.join(root, rel)
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        if len(FLAG_OFF.findall(text)) != 1 or text.count("const val PART2_ENABLED") != 1:
            raise FlipError(f"{rel} must hold exactly one 'const val PART2_ENABLED: Boolean = false' line")
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(FLAG_OFF.sub(r"\1true", text))
        changed.append(rel)
    return changed


def main():
    if state(REPO) == "on":
        print("PART2_ENABLED is already on in the repo, so every normal suite is the flag-on run; nothing to do", flush=True)
        return 0
    copy = tempfile.mkdtemp(prefix="part2-flag-on-")
    try:
        names = copy_tracked(REPO, copy)
        flip(copy)
        print(f"flag-on copy of {len(names)} files in {copy}; PART2_ENABLED and its expectation are true there", flush=True)
        return subprocess.run(["./gradlew", *GRADLE_TASKS], cwd=copy).returncode
    finally:
        shutil.rmtree(copy, ignore_errors=True)


if __name__ == "__main__":
    sys.exit(main())

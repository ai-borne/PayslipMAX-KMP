"""Structural guard: no `println` / `System.out` / `System.err` in production Kotlin outside `logging/Logger.kt`.

Standard output reaches logcat (Android) and the device console (iOS) in release builds. Payslip tokens, names and amounts once
went out that way on every import (Nov 2026 tech-debt register, item 1). `Logger` is the one place that prints, and it only takes
fixed strings; everything else logs through it. Test source sets are not checked.

Line based like the other guards; comments and string contents are ignored (via stateful_singleton_guard._code_only), so a KDoc that
mentions println or a string that contains it is fine.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import stateful_singleton_guard as _singleton

_PRODUCTION_RE = re.compile(r"(^|/)src/[A-Za-z0-9]*Main/")
_LOGGER_RE = re.compile(r"(^|/)logging/Logger\.kt$")
_STDOUT_RE = re.compile(r"(?<![\w.])println\s*\(|\bSystem\s*\.\s*(?:out|err)\b")


def check_no_stdout(filepath):
    path = filepath.replace("\\", "/")
    if not _PRODUCTION_RE.search(path) or _LOGGER_RE.search(path):
        return []
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            lines = f.readlines()
    except Exception as e:
        return [f"Error reading file: {str(e)}"]

    errors = []
    in_raw = False
    for idx, line in enumerate(lines):
        code, in_raw = _singleton._code_only(line, in_raw)
        if _STDOUT_RE.search(code):
            errors.append(
                f"Standard output at line {idx + 1}: println/System.out reaches logcat and the device console in release builds. "
                f"Log a fixed string through Logger instead; never print payslip values, names or tokens"
            )
    return errors


def scan_repository(root):
    errors = []
    for path in _singleton._main_source_files(root):
        errors += [f"{os.path.relpath(path, root)}: {e}" for e in check_no_stdout(path)]
    return errors

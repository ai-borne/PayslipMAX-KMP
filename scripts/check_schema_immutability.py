#!/usr/bin/env python3
"""
Guards against silently mutating a Room database schema JSON that has already
shipped on main. Once a @Database version is released, its exported schema
(and the entity/table set it describes) must never change in place — the fix
for a schema mistake is always a NEW version with its own AutoMigration step,
never editing an old one.

Incident this guards against: the Gemini-removal cleanup deleted a table from
the entities list but left the @Database version unchanged and rewrote that
version's already-shipped schema JSON to match. Devices already on that
version never ran a migration (the version number matched), so Room's
identity-hash check failed on next launch -> uncaught exception -> crash.
"""
import os
import subprocess
import sys

SCHEMA_PATHSPEC = "shared/schemas"


def resolve_base_ref() -> str | None:
    for ref in ("origin/main", "origin/master", "main", "master"):
        result = subprocess.run(
            ["git", "rev-parse", "--verify", "--quiet", ref],
            capture_output=True,
        )
        if result.returncode == 0:
            return ref
    return None


def show(ref: str, path: str) -> str | None:
    result = subprocess.run(
        ["git", "show", f"{ref}:{path}"],
        capture_output=True,
        text=True,
    )
    return result.stdout if result.returncode == 0 else None


def base_schema_files(ref: str) -> list[str]:
    result = subprocess.run(
        ["git", "ls-tree", "-r", "--name-only", ref, "--", SCHEMA_PATHSPEC],
        capture_output=True,
        text=True,
    )
    return [p for p in result.stdout.splitlines() if p.endswith(".json")]


def main() -> int:
    base_ref = resolve_base_ref()
    if base_ref is None:
        print("⚠️  No base ref (origin/main) found — skipping schema immutability check.")
        return 0

    violations = []
    for path in base_schema_files(base_ref):
        base_content = show(base_ref, path)
        if not os.path.exists(path):
            violations.append(f"{path} (deleted — a released schema file must never be removed)")
            continue
        with open(path, "r", encoding="utf-8") as f:
            current_content = f.read()
        if current_content != base_content:
            violations.append(f"{path} (modified — this version already shipped on {base_ref})")

    if violations:
        print(f"❌ Schema immutability violation against {base_ref}:")
        for v in violations:
            print(f"   - {v}")
        print(
            "\nA Room schema JSON that already shipped must never change. Bump the "
            "@Database version and add a new AutoMigration step instead of editing "
            "an existing one — see PayslipDatabase.kt's v10->v11 migration for the pattern."
        )
        return 1

    print(f"✅ Schema immutability check passed against {base_ref}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

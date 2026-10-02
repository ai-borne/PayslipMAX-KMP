"""Structural guard: no new process-wide `object` that holds mutable state.

A top-level or companion `object` with a `var` or a mutable collection is hidden global state: tests become
order-dependent, it cannot be substituted, and it does not survive multiple instances. New code gets such
collaborators from Koin instead. Constants and pure functions in an `object` are fine and are not flagged.

The singletons that already existed when the rule was added are listed in ALLOWED_STATEFUL_SINGLETONS. Most
are platform-interop bridges that need a static entry point (Swift calls them, or a platform component that
starts before Koin). The list is closed: a new entry needs a reason in review, and an entry that no
longer matches a real singleton fails the build, so a stale name cannot excuse a future one.

Known blind spot: a collaborator cached with `by lazy` (e.g. `Logger.crashReporter`) is not flagged, because the
same construct is also a harmless cache of a pure value. Such singletons are listed in
docs/Plan/09_PayAudit_PhasePlan.md instead.

Line/regex based like the other guards in check_tech_debt_limits.py, not a Kotlin parser. It tracks brace
depth so a `var` inside a function or a nested class is not mistaken for the object's own state.
"""
import os
import re

# "<repo-relative path>::<object name>"; a companion object is "<EnclosingClass>.Companion".
ALLOWED_STATEFUL_SINGLETONS = frozenset(
    {
        # Android: process-wide holders set before Koin exists, or read by a static platform callback.
        "shared/src/androidMain/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt::ContextHolder",
        "shared/src/androidMain/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt::CryptoHelper",
        "shared/src/androidMain/kotlin/com/payslipmax/pdfparser/insights/gemma/GemmaBaseModelInstaller.android.kt::AndroidGemmaBaseModelInstaller.Companion",
        "shared/src/androidMain/kotlin/com/payslipmax/pdfparser/insights/gemma/LiteRtEngineStore.kt::LiteRtEngineStore",
        "shared/src/androidMain/kotlin/com/payslipmax/pdfparser/rating/ReviewRequester.android.kt::ReviewActivityBridge",
        # iOS: the Keychain key cache, and Swift-to-Kotlin bridge closures that Swift sets through a static entry point.
        "shared/src/iosMain/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt::CryptoHelper",
        "shared/src/iosMain/kotlin/com/payslipmax/pdfparser/insights/gemma/GemmaBaseModelInstaller.ios.kt::IosGemmaBaseModelInstaller.Companion",
        "shared/src/iosMain/kotlin/com/payslipmax/pdfparser/insights/gemma/GemmaEngine.ios.kt::GemmaEngine.Companion",
        "shared/src/iosMain/kotlin/com/payslipmax/pdfparser/telemetry/CrashReporter.ios.kt::IosCrashReporter.Companion",
        "shared/src/iosMain/kotlin/com/payslipmax/pdfparser/telemetry/GemmaInstallTelemetry.ios.kt::IosGemmaInstallTelemetry.Companion",
    }
)

SCAN_ROOTS = ("shared/src", "composeApp/src")

_DECL_RE = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|private|internal|protected|actual|expect|data|open|abstract|sealed|enum|inner)\s+)*"
    r"(?P<kind>companion\s+object|object|class|interface)\b\s*(?P<name>[A-Za-z_]\w*)?"
)
_VAR_RE = re.compile(r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|private|internal|protected|lateinit|override|actual)\s+)*var\b")
_MUTABLE_VAL_RE = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|private|internal|protected|override|actual)\s+)*val\b[^=]*=\s*"
    r"(?:mutable\w*Of|linkedMapOf|linkedSetOf|hashMapOf|hashSetOf|arrayListOf|"
    r"(?:Mutable|Linked|Hash|Array|Concurrent|Copy)\w*(?:Map|Set|List|Flow|Queue|Deque)\b|Atomic\w+|ArrayList|ArrayDeque)\s*[<(]"
)


def _code_only(line, in_raw):
    """Return (line with string/char contents and comments removed, still-inside-raw-string)."""
    out = []
    i = 0
    n = len(line)
    while i < n:
        if in_raw:
            end = line.find('"""', i)
            if end == -1:
                return "".join(out), True
            i, in_raw = end + 3, False
            continue
        if line.startswith('"""', i):
            in_raw, i = True, i + 3
        elif line.startswith("//", i):
            break
        elif line[i] == '"':
            i += 1
            while i < n and line[i] != '"':
                i += 2 if line[i] == "\\" else 1
            i += 1
        elif line[i] == "'" and i + 2 < n and (line[i + 1] != "\\" and line[i + 2] == "'" or line[i + 1] == "\\"):
            i = line.find("'", i + 2) + 1 or n
        else:
            out.append(line[i])
            i += 1
    return "".join(out), in_raw


def _body_brace_opens(code, parens):
    """True if the line opens a `{` outside any parenthesis, i.e. a declaration body and not a lambda default."""
    for ch in code:
        if ch == "(":
            parens += 1
        elif ch == ")":
            parens = max(0, parens - 1)
        elif ch == "{" and parens == 0:
            return True
    return False


def find_stateful_singletons(filepath):
    """Return [(object_name, line_number, reason)] for every stateful singleton declared in the file."""
    with open(filepath, "r", encoding="utf-8") as f:
        lines = f.readlines()

    found = []
    depth = 0
    in_raw = False
    stack = []  # (kind, name, body_depth) of every declaration whose `{` was seen
    pending = None  # a declaration whose header spans lines, waiting for its `{`
    parens = 0
    for number, raw in enumerate(lines, start=1):
        code, in_raw = _code_only(raw, in_raw)
        owner = stack[-1] if stack else None
        if owner and owner[0] == "object" and depth == owner[2]:
            reason = "mutable `var`" if _VAR_RE.match(code) else "mutable collection/holder" if _MUTABLE_VAL_RE.match(code) else None
            if reason:
                found.append((owner[1], number, reason))

        decl = _DECL_RE.match(code)
        opens, closes = code.count("{"), code.count("}")
        body_opens = _body_brace_opens(code, parens)
        parens = max(0, parens + code.count("(") - code.count(")"))
        if decl and not opens:
            pending = decl
        elif not decl and body_opens and pending:
            decl, pending = pending, None
        elif body_opens:
            pending = None
        if decl and opens:
            kind = "object" if "object" in decl.group("kind") else "class"
            name = decl.group("name")
            if "companion" in decl.group("kind"):
                enclosing = next((s[1] for s in reversed(stack) if s[1]), "")
                name = f"{enclosing}.Companion" if enclosing else "Companion"
            # `object : Runnable {` has no name; it is an expression, not a singleton.
            if name or kind == "class":
                stack.append((kind, name, depth + 1))
            else:
                stack.append(("class", "", depth + 1))
        depth += opens - closes
        while stack and depth < stack[-1][2]:
            stack.pop()
    return found


def is_allowlisted(filepath, name, allowlist):
    normalized = filepath.replace("\\", "/")
    return any(
        entry.partition("::")[2] == name and normalized.endswith(entry.partition("::")[0])
        for entry in allowlist
    )


_TEST_SOURCE_SET_RE = re.compile(r"/src/[^/]*[Tt]est[^/]*/")


def check_stateful_singletons(filepath, allowlist=ALLOWED_STATEFUL_SINGLETONS):
    if _TEST_SOURCE_SET_RE.search(filepath.replace("\\", "/")):
        return []  # test doubles may hold state; the rule is about production singletons
    try:
        found = find_stateful_singletons(filepath)
    except Exception as e:
        return [f"Error reading file: {str(e)}"]
    return [
        f"Stateful singleton '{name}' at line {line} ({reason}): hidden global state. "
        f"Provide it through Koin as a `single` instead; if it is a platform-interop bridge that needs a "
        f"static entry point, add it to ALLOWED_STATEFUL_SINGLETONS in scripts/stateful_singleton_guard.py"
        for name, line, reason in found
        if not is_allowlisted(filepath, name, allowlist)
    ]


def _main_source_files(root):
    for scan_root in SCAN_ROOTS:
        base = os.path.join(root, scan_root)
        if not os.path.isdir(base):
            continue
        for source_set in sorted(os.listdir(base)):
            if "test" in source_set.lower():
                continue
            for dirpath, _, files in os.walk(os.path.join(base, source_set)):
                for file in files:
                    if file.endswith(".kt"):
                        yield os.path.join(dirpath, file)


def scan_repository(root):
    errors = []
    for path in _main_source_files(root):
        errors += [f"{os.path.relpath(path, root)}: {e}" for e in check_stateful_singletons(path)]
    return errors


def stale_allowlist_entries(root):
    """Allowlist entries that no longer match a stateful singleton in the tree."""
    live = set()
    for path in _main_source_files(root):
        rel = os.path.relpath(path, root).replace(os.sep, "/")
        live.update(f"{rel}::{name}" for name, _, _ in find_stateful_singletons(path))
    return sorted(ALLOWED_STATEFUL_SINGLETONS - live)

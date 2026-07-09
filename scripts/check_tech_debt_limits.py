#!/usr/bin/env python3
import os
import re
import sys
import argparse

# Default directory paths to scan if no specific files are passed
SCAN_DIRECTORIES = [
    "composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui",
    "composeApp/src/iosMain/kotlin/com/payslipmax/pdfparser",
    "shared/src/commonMain/kotlin/com/payslipmax/pdfparser"
]

# On iOS every ComposeUIViewController is an independent Compose tree that inherits no providers
# (theme, etc.) from any other VC. To stop a screen being hosted un-themed (the Phase 4 regression
# where pushed detail screens rendered in the default light theme), ComposeUIViewController may only
# be constructed inside these approved factories, which apply PDFParserTheme centrally.
IOS_VC_ALLOWED_FUNCTIONS = {"themedViewController"}

# Every pushed iOS detail screen must self-apply the safe-area inset (or its own Scaffold) so it
# isn't cut off under the notch/status bar on a standalone ComposeUIViewController. See
# check_pushed_screen_insets() below for the full rationale.
SCREENS_DIR = "composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens"
_ENTRY_SCREEN_RE = re.compile(r'\b([A-Z][A-Za-z0-9]*Screen)\s*\(')
_SCAFFOLD_RE = re.compile(r'\bScaffold\s*\(')
_SAFE_AREA_CALL = "detailScreenSafeArea()"

def check_file_limits(filepath):
    errors = []
    with open(filepath, 'r', encoding='utf-8') as f:
        try:
            lines = f.readlines()
        except Exception as e:
            return [f"Error reading file: {str(e)}"]

    # 1. Enforce file length limit of 300 lines
    if len(lines) > 300:
        errors.append(f"File exceeds 300 lines limit ({len(lines)} lines)")

    # 2. Parse composables and check function length & coding standard regressions
    i = 0
    while i < len(lines):
        line = lines[i].strip()
        if line.startswith("@Composable"):
            # Find the actual function start (skip annotations)
            func_line_idx = -1
            for j in range(i + 1, min(i + 10, len(lines))):
                if "fun " in lines[j]:
                    func_line_idx = j
                    break

            if func_line_idx != -1:
                brace_count = 0
                started = False
                start_line = func_line_idx
                end_line = -1

                for k in range(func_line_idx, len(lines)):
                    k_line = lines[k]
                    brace_count += k_line.count('{')
                    brace_count -= k_line.count('}')
                    if '{' in k_line:
                        started = True
                    if started and brace_count == 0:
                        end_line = k
                        break

                if end_line != -1:
                    length = (end_line - start_line) + 1
                    match = re.search(r'fun\s+([A-Za-z0-9_]+)', lines[func_line_idx])
                    func_name = match.group(1) if match else f"function_at_line_{start_line+1}"

                    # Enforce maximum 50 lines per Composable
                    if length > 50:
                        errors.append(
                            f"Composable '{func_name}' at line {start_line+1} exceeds 50 lines limit ({length} lines)"
                        )

                    # Enforce design tokens for dimensions, colors & copy strings inside ui packages
                    if "ui/screens" in filepath or "ui/components" in filepath:
                        for l_idx in range(start_line, end_line + 1):
                            l_val = lines[l_idx].strip()
                            if l_val.startswith("//") or l_val.startswith("/*") or l_val.startswith("*"):
                                continue

                            # Raw .dp or .sp literal check
                            if re.search(r'\b\d+\s*\.(?:dp|sp)\b', l_val):
                                errors.append(
                                    f"Raw dimension literal found in '{func_name}' at line {l_idx+1}: '{l_val}'"
                                )

                            # Hardcoded color checks (e.g. Color(0xFF...))
                            if "Color(0xFF" in l_val or "Color(0xff" in l_val:
                                errors.append(
                                    f"Hardcoded hex color found in '{func_name}' at line {l_idx+1}: '{l_val}'"
                                )

                            # Direct hardcoded strings in Text/text parameters
                            if re.search(r'(?:text\s*=\s*|Text\(\s*)"[A-Za-z]+[^"]*"', l_val):
                                errors.append(
                                    f"Hardcoded user copy string found in '{func_name}' at line {l_idx+1}: '{l_val}'"
                                )

                    # Advance to skip checking inside this function
                    i = end_line
        i += 1
    return errors

def check_ios_vc_theming(filepath):
    """Structural guard: in iosMain, ComposeUIViewController may only be constructed inside an
    approved themed factory (IOS_VC_ALLOWED_FUNCTIONS), so no screen is ever hosted without the
    app theme applied. Only enforced on files under an iosMain source set."""
    if "iosMain" not in filepath.replace("\\", "/"):
        return []

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
    except Exception as e:
        return [f"Error reading file: {str(e)}"]

    errors = []
    fun_re = re.compile(r'\bfun\s+([A-Za-z0-9_]+)')
    # A construction call — "ComposeUIViewController(" or "ComposeUIViewController {" — not the
    # import statement or a KDoc/comment mention.
    call_re = re.compile(r'\bComposeUIViewController\s*[({]')
    current_fun = None

    for idx, raw in enumerate(lines):
        m = fun_re.search(raw)
        if m:
            current_fun = m.group(1)

        stripped = raw.strip()
        if stripped.startswith(("import ", "//", "*", "/*")):
            continue

        if call_re.search(raw) and current_fun not in IOS_VC_ALLOWED_FUNCTIONS:
            allowed = " or ".join(sorted(IOS_VC_ALLOWED_FUNCTIONS)) + "()"
            errors.append(
                f"Bare ComposeUIViewController at line {idx+1} (in '{current_fun}') — host iOS "
                f"Compose VCs only via {allowed} so the app theme is always applied"
            )
    return errors


def _brace_span(lines, start_idx):
    """Return the (start, end) inclusive line-index span of the block starting at start_idx.
    Paren-depth aware so a parameter default lambda on the signature line (e.g.
    `onBack: () -> Unit = {}`) isn't mistaken for the function body's opening/closing brace —
    braces only count once the top-level parameter-list parens have closed."""
    paren_depth = 0
    brace_count = 0
    started = False
    for k in range(start_idx, len(lines)):
        for ch in lines[k]:
            if ch == '(':
                paren_depth += 1
            elif ch == ')':
                paren_depth = max(0, paren_depth - 1)
            elif ch == '{' and paren_depth == 0:
                brace_count += 1
                started = True
            elif ch == '}' and paren_depth == 0:
                brace_count -= 1
        if started and brace_count == 0:
            return start_idx, k
    return start_idx, len(lines) - 1


def _find_fun_span(lines, name):
    """Locate `fun <name>(` in lines and return its brace-counted span, or None if not found."""
    fun_re = re.compile(r'\bfun\s+' + re.escape(name) + r'\s*\(')
    for idx, line in enumerate(lines):
        if fun_re.search(line):
            return _brace_span(lines, idx)
    return None


def _resolve_screen(workspace_dir, name):
    """Find the commonMain screen file defining composable `name` and its line span.
    Returns (rel_path, lines, span) or None if not found under SCREENS_DIR."""
    screens_dir = os.path.join(workspace_dir, SCREENS_DIR)
    if not os.path.isdir(screens_dir):
        return None
    for fname in sorted(os.listdir(screens_dir)):
        if not fname.endswith(".kt"):
            continue
        fpath = os.path.join(screens_dir, fname)
        try:
            with open(fpath, 'r', encoding='utf-8') as f:
                lines = f.readlines()
        except Exception:
            continue
        span = _find_fun_span(lines, name)
        if span:
            return os.path.relpath(fpath, workspace_dir), lines, span
    return None


def _has_safe_area(lines, span):
    start, end = span
    for idx in range(start, end + 1):
        if _SAFE_AREA_CALL in lines[idx] or _SCAFFOLD_RE.search(lines[idx]):
            return True
    return False


def check_pushed_screen_insets(filepath, workspace_dir):
    """Structural guard: every composable registered as a pushed iOS detail-screen entry point in
    IosNavHost.detailViewController (MainViewController.kt) must apply Modifier.detailScreenSafeArea()
    (or its own Scaffold(...), an equally valid pattern) on its root container — either directly, or
    via exactly one delegation hop to another local `SomethingScreen(...)` composable, matching this
    codebase's real (shallow) wrapper -> presentational-screen pattern. Deeper delegation chains are
    NOT followed and will silently pass this check — this guard is line/regex-based, not a full
    parser, consistent with check_file_limits()/check_ios_vc_theming() above.

    Known limitation: in pre-commit's file-list mode this only runs when MainViewController.kt is
    among the staged files. In practice that's unavoidable when a *new* screen is wired (editing the
    `when` block is unavoidable), but an edit that *removes* the inset call from an already-wired
    screen without touching MainViewController.kt is only caught by the full SCAN_DIRECTORIES scan
    (CI), not the pre-commit hook.
    """
    if not filepath.replace("\\", "/").endswith("MainViewController.kt"):
        return []

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
    except Exception as e:
        return [f"Error reading file: {str(e)}"]

    entry_span = _find_fun_span(lines, "detailViewController")
    if entry_span is None:
        return []
    start, end = entry_span
    span_text = "".join(lines[start:end + 1])
    entry_names = list(dict.fromkeys(_ENTRY_SCREEN_RE.findall(span_text)))

    errors = []
    for name in entry_names:
        resolved = _resolve_screen(workspace_dir, name)
        if resolved is None:
            errors.append(
                f"Pushed screen '{name}' referenced in detailViewController but its defining file "
                f"could not be resolved under {SCREENS_DIR}/ — cannot verify it applies "
                f"Modifier.{_SAFE_AREA_CALL} (see ui/components/ScreenInsets.kt)"
            )
            continue

        own_file, own_lines, own_span = resolved
        if _has_safe_area(own_lines, own_span):
            continue

        own_start, own_end = own_span
        own_span_text = "".join(own_lines[own_start:own_end + 1])
        hop_names = [n for n in _ENTRY_SCREEN_RE.findall(own_span_text) if n != name]

        hop_ok = False
        hop_file = None
        if hop_names:
            hop_resolved = _resolve_screen(workspace_dir, hop_names[0])
            if hop_resolved is not None:
                hop_file, hop_lines, hop_span = hop_resolved
                hop_ok = _has_safe_area(hop_lines, hop_span)

        if not hop_ok:
            target_file = hop_file if hop_names and hop_file else own_file
            errors.append(
                f"Pushed screen '{name}' ({own_file}) has no {_SAFE_AREA_CALL} or Scaffold(...) on "
                f"its root container (checked {target_file}) — apply Modifier.{_SAFE_AREA_CALL} per "
                f"ui/components/ScreenInsets.kt so this iOS detail screen isn't cut off under the notch"
            )

    return errors


def audit_file(filepath, workspace_dir):
    return (
        check_file_limits(filepath)
        + check_ios_vc_theming(filepath)
        + check_pushed_screen_insets(filepath, workspace_dir)
    )


def main():
    parser = argparse.ArgumentParser(description="Audit codebase files for tech debt limit violations.")
    parser.add_argument("files", nargs="*", help="Specific Kotlin files to audit. If none, runs full scan.")
    parser.add_argument("--strict", action="store_true", help="Fail build (exit code 1) on any violation.")
    args = parser.parse_args()

    print("🧹 Running tech debt limits and regressions audit...")
    workspace_dir = os.getcwd()
    all_errors = {}
    total_files_checked = 0

    if args.files:
        # Scan only the specified files passed
        for file in args.files:
            if file.endswith(".kt") and os.path.exists(file):
                rel_path = os.path.relpath(file, workspace_dir)
                errors = audit_file(file, workspace_dir)
                total_files_checked += 1
                if errors:
                    all_errors[rel_path] = errors
    else:
        # Full scan directories
        for scan_dir in SCAN_DIRECTORIES:
            full_path = os.path.join(workspace_dir, scan_dir)
            if not os.path.exists(full_path):
                continue

            for root, _, files in os.walk(full_path):
                for file in files:
                    if file.endswith(".kt"):
                        filepath = os.path.join(root, file)
                        rel_path = os.path.relpath(filepath, workspace_dir)
                        errors = audit_file(filepath, workspace_dir)
                        total_files_checked += 1
                        if errors:
                            all_errors[rel_path] = errors

    if all_errors:
        print(f"\n⚠️ Found violations in {len(all_errors)} / {total_files_checked} checked files:\n")
        for filepath, errors in all_errors.items():
            print(f"📄 {filepath}:")
            for err in errors:
                print(f"   - {err}")
        
        if args.strict:
            print("\n❌ Audit failed in strict mode! Please fix these tech debts to comply with the Zero Tech Debt Policy.")
            sys.exit(1)
        else:
            print("\n⚠️ Audit warnings printed. (Non-strict mode, build passed).")
            sys.exit(0)
    else:
        print(f"✅ Audit passed successfully! Cleaned all {total_files_checked} checked source files.")
        sys.exit(0)

if __name__ == "__main__":
    main()

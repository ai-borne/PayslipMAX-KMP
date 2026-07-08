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


def audit_file(filepath):
    return check_file_limits(filepath) + check_ios_vc_theming(filepath)


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
                errors = audit_file(file)
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
                        errors = audit_file(filepath)
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

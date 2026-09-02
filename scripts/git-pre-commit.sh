#!/bin/sh

# Get list of staged Kotlin files
staged_files=$(git diff --cached --name-only --diff-filter=d | grep '\.kt$')
staged_schema_files=$(git diff --cached --name-only --diff-filter=d | grep 'shared/schemas/')

if [ -z "$staged_files" ] && [ -z "$staged_schema_files" ]; then
    exit 0
fi

if command -v gitleaks >/dev/null 2>&1; then
    echo "🔒 Scanning staged changes for secrets (gitleaks)..."
    gitleaks protect --staged --config .gitleaks.toml
    if [ $? -ne 0 ]; then
        echo "❌ Commit rejected: gitleaks found a potential secret in staged changes."
        exit 1
    fi
else
    echo "⚠️  gitleaks not installed — skipping local secret scan (CI will still catch it). Install: brew install gitleaks"
fi

if [ -n "$staged_files" ]; then
    echo "🔍 Auditing staged Kotlin files for tech debt limits..."

    # Run check script in strict mode on staged files
    python3 scripts/check_tech_debt_limits.py --strict $staged_files

    if [ $? -ne 0 ]; then
        echo "❌ Commit rejected due to tech debt limits or design system regressions."
        exit 1
    fi
fi

if [ -n "$staged_schema_files" ]; then
    echo "🔐 Checking Room schema immutability..."
    python3 scripts/check_schema_immutability.py
    if [ $? -ne 0 ]; then
        echo "❌ Commit rejected: a shipped Room schema JSON was modified or deleted."
        exit 1
    fi
fi

echo "🧹 Running ktlint across all modules (mirrors CI)..."
./gradlew ktlintCheck -q 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Commit rejected: ktlint violations found. Run ./gradlew ktlintFormat to auto-fix."
    exit 1
fi

# Run debug-variant unit tests only for the module(s) actually touched, so the
# inner dev loop stays fast — the exhaustive matrix (release variant + full
# iOS suite) is pre-push's job, not pre-commit's.
if echo "$staged_files" | grep -qE 'shared/src/'; then
    echo "🧪 Running shared module unit tests (debug variant)..."
    ./gradlew :shared:testDebugUnitTest -q 2>&1
    if [ $? -ne 0 ]; then
        echo "❌ Commit rejected: shared module unit tests failed."
        exit 1
    fi
fi

if echo "$staged_files" | grep -qE 'composeApp/src/'; then
    echo "🧪 Running composeApp module unit tests (debug variant)..."
    ./gradlew :composeApp:testDebugUnitTest -q 2>&1
    if [ $? -ne 0 ]; then
        echo "❌ Commit rejected: composeApp module unit tests failed."
        exit 1
    fi
fi

# Trigger iOS verification on any KMP source change, not just iosMain —
# expect/actual mismatches (the popoverPresentationController-class bug) are
# often introduced by a commonMain change, not the iosMain side.
kmp_files=$(echo "$staged_files" | grep -E '(shared|composeApp)/src/(commonMain|iosMain)')

if [ -n "$kmp_files" ]; then
    echo "🍎 Verifying iOS framework links cleanly (mirrors Xcode's build phase)..."
    ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 -q 2>&1
    if [ $? -ne 0 ]; then
        echo "❌ Commit rejected due to iOS framework link/compilation errors."
        exit 1
    fi
fi

echo "✅ Tech debt audit, style checks, and compilation passed!"
exit 0

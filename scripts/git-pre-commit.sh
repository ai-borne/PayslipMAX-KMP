#!/bin/sh

# Get list of staged Kotlin files
staged_files=$(git diff --cached --name-only --diff-filter=d | grep '\.kt$')

if [ -z "$staged_files" ]; then
    exit 0
fi

echo "🔍 Auditing staged Kotlin files for tech debt limits..."

# Run check script in strict mode on staged files
python3 scripts/check_tech_debt_limits.py --strict $staged_files

if [ $? -ne 0 ]; then
    echo "❌ Commit rejected due to tech debt limits or design system regressions."
    exit 1
fi

echo "✅ Tech debt audit passed!"
exit 0

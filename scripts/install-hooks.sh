#!/bin/sh

HOOKS_DIR=".git/hooks"
PRE_COMMIT_HOOK="$HOOKS_DIR/pre-commit"

if [ ! -d ".git" ]; then
    echo "❌ Error: Not a git repository or not run from the root directory."
    exit 1
fi

echo "📦 Installing Git pre-commit hook..."

# Copy pre-commit script to git hooks directory
cp scripts/git-pre-commit.sh "$PRE_COMMIT_HOOK"
chmod +x "$PRE_COMMIT_HOOK"

echo "✅ Git pre-commit hook installed successfully under $PRE_COMMIT_HOOK"
exit 0

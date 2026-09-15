#!/bin/sh

# Exhaustive safety net, run once per push (not per commit): full Android+common matrix (both
# build variants, full corpus regression, lint, ktlint, checkFileSizes), the full iOS unit test
# suite (incl. ParserUtilsIosPerfTest's Native timing assertions), the native Swift XCTest suite
# (iosApp/PayslipMaxTests — nav bridge, byte marshaling, auth retry, Gemma engine cache; see
# docs/Plan/07_iOS_Native_Test_Coverage_Gap.md), a Room schema immutability check, and a gitleaks
# scan over the actual commit range being pushed. No staged-file gating — by push time the cost of
# a full run is worth paying once, unlike pre-commit's fast/scoped checks.
#
# git invokes this with no args; it supplies "<local ref> <local sha1> <remote ref> <remote sha1>"
# lines on stdin, one per ref being pushed.

zero="0000000000000000000000000000000000000000"
start_time=$(date +%s)

echo "🚦 Running exhaustive pre-push checks..."

echo ""
echo "1/5 🤖 Full Android + common gate (./gradlew check -x iosX64Test -x iosSimulatorArm64Test)..."
stage_start=$(date +%s)
./gradlew check -x iosX64Test -x iosSimulatorArm64Test -q 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Push rejected: Android/common check failed (debug+release variants, corpus regression, lint, ktlint, checkFileSizes)."
    exit 1
fi
echo "   ✅ done in $(($(date +%s) - stage_start))s"

echo ""
echo "2/5 🍎 Full iOS unit test suite (./gradlew iosX64Test iosSimulatorArm64Test)..."
stage_start=$(date +%s)
./gradlew iosX64Test iosSimulatorArm64Test -q 2>&1
if [ $? -ne 0 ]; then
    echo "❌ Push rejected: iOS unit tests failed (incl. ParserUtilsIosPerfTest timing assertions)."
    exit 1
fi
echo "   ✅ done in $(($(date +%s) - stage_start))s"

echo ""
echo "3/5 📱 Native iOS XCTest suite (PayslipMaxTests)..."
stage_start=$(date +%s)
SIM_ID=$(xcrun simctl list devices available | grep -m1 -E '^ *iPhone .*\(' | grep -oE '[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}')
if [ -z "$SIM_ID" ]; then
    echo "❌ Push rejected: no available iPhone simulator found (xcrun simctl list devices available)."
    exit 1
fi
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp -destination "id=$SIM_ID" -only-testing:PayslipMaxTests -quiet
if [ $? -ne 0 ]; then
    echo "❌ Push rejected: native iOS XCTest suite failed (PayslipMaxTests)."
    exit 1
fi
echo "   ✅ done in $(($(date +%s) - stage_start))s"

echo ""
echo "4/5 🔐 Checking Room schema immutability..."
stage_start=$(date +%s)
python3 scripts/check_schema_immutability.py
if [ $? -ne 0 ]; then
    echo "❌ Push rejected: a shipped Room schema JSON was modified or deleted."
    exit 1
fi
echo "   ✅ done in $(($(date +%s) - stage_start))s"

echo ""
echo "5/5 🔒 Scanning pushed commit range for secrets (gitleaks)..."
stage_start=$(date +%s)
if command -v gitleaks >/dev/null 2>&1; then
    while read -r local_ref local_sha remote_ref remote_sha; do
        if [ "$local_sha" = "$zero" ]; then
            # Deleting a ref — nothing to scan
            continue
        fi

        if [ "$remote_sha" = "$zero" ]; then
            # New ref on the remote — no prior remote sha to diff from, scan full history
            # reachable from local_sha instead of a range.
            range="$local_sha"
        else
            range="$remote_sha..$local_sha"
        fi

        gitleaks detect --config .gitleaks.toml --log-opts="$range" --no-banner
        if [ $? -ne 0 ]; then
            echo "❌ Push rejected: gitleaks found a potential secret in commit range $range."
            exit 1
        fi
    done
    echo "   ✅ done in $(($(date +%s) - stage_start))s"
else
    echo "   ⚠️  gitleaks not installed — skipping local secret scan (CI will still catch it). Install: brew install gitleaks"
fi

echo ""
echo "✅ All pre-push checks passed in $(($(date +%s) - start_time))s."
exit 0

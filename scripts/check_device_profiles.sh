#!/usr/bin/env bash
# Fails if PayslipMax is installed under an Android user profile other than the foreground one on a
# connected device. Run it before switching a device from a local build to the Play (internal
# testing) build: Android keeps ONE signature per package across all profiles, so a leftover copy in a
# secondary profile makes Play refuse with "another user has already installed an incompatible
# version on this device".
#
# Usage: scripts/check_device_profiles.sh [package]   (honours ANDROID_SERIAL; default: all devices)
set -euo pipefail

PKG="${1:-in.aiborne.payslipmax}"

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  serials="$ANDROID_SERIAL"
else
  serials="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
fi

if [[ -z "$serials" ]]; then
  echo "No adb device attached; nothing to check."
  exit 0
fi

status=0
for serial in $serials; do
  current="$(adb -s "$serial" shell am get-current-user | tr -d '\r')"
  users="$(adb -s "$serial" shell pm list users | grep -oE 'UserInfo\{[0-9]+' | grep -oE '[0-9]+')"
  for user in $users; do
    [[ "$user" == "$current" ]] && continue
    if adb -s "$serial" shell pm list packages --user "$user" | tr -d '\r' | grep -qx "package:$PKG"; then
      echo "FAIL [$serial]: $PKG is installed for user $user, but the foreground user is $current." >&2
      echo "  Play cannot install its build for user $current until this copy is removed. That deletes user $user's" >&2
      echo "  app data, so export a backup first if it matters:" >&2
      echo "  adb -s $serial shell pm uninstall --user $user $PKG" >&2
      status=1
    fi
  done
done

[[ $status -eq 0 ]] && echo "OK: $PKG is not installed under any non-foreground user profile."
exit $status

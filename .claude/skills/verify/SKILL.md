---
name: verify
description: Build/launch/drive recipe for manually verifying composeApp changes on a real Android device via adb. Use before the generic verify skill's cold-start when checking that a UI change actually works end-to-end.
---

# PayslipMax Android verify recipe

This repo has no CI-driven UI test harness for Compose Multiplatform — manual driving via `adb`
on a connected device/emulator is the only way to observe a UI change. This file captures the
recipe so future sessions skip the cold start.

## Build & install

```bash
./gradlew :composeApp:installDebug        # builds + installs on whatever `adb devices` shows
adb devices                                # confirm a device/emulator is attached first
```

Package name: `com.payslipmax.pdfparser`. Launch it with:

```bash
adb shell monkey -p com.payslipmax.pdfparser -c android.intent.category.LAUNCHER 1
```

## Driving + screenshots

```bash
adb shell input tap <x> <y>
adb shell input swipe <x1> <y1> <x2> <y2> <duration_ms>   # scroll
adb exec-out screencap -p > /path/to/file.png
```

Bottom tab bar (approx, real device pixels on a 1080x2424 screen): Dashboard ~x=110, History
~x=333, Insights ~x=676, Settings ~x=953, all at y≈2238.

**Coordinate gotcha:** the `Read` tool displays screenshots downscaled (e.g. 891x2000 for a
1080x2424 capture) and reports both sizes in its output. If you eyeball a tap target from the
*displayed* image, multiply both x and y by (real/displayed) — e.g. 1080/891 ≈ 1.212 — before
sending the `adb shell input tap`. Sending the displayed-image coordinates directly under-shoots
the real target and produces silent no-op taps (looks like the button didn't work; it's actually
a coordinate scale mismatch).

## Testing PRO vs Free gating without real Play Billing

Settings tab → scroll to the bottom → **Developer · PRO Override** section (debug-only, not in
release builds) → segmented control: `Follow Flag` / `Force PRO` / `Force Free`. This is the way
to exercise both entitlement paths (locked CTAs, `rememberHasAccess` gating, premium
tools/reports) live without needing a real subscription or sandbox purchase. Toggling it
recomposes immediately — no relaunch needed.

**Restore the override to whatever it was before you started** (check it before you touch it) —
this repo's dev device may already be mid-testing something else.

## Gotchas

- The connected device in this environment tends to be the user's own physical device with real
  (de-identified-in-git, but real-on-device) payslip data already loaded — not a throwaway
  emulator. Don't run destructive actions (`Reset App & Clear Data` in Settings) without asking.
- The footer on the Settings screen prints `PayslipMax iOS - Version x.y.z` even on the Android
  build — a pre-existing string bug unrelated to most feature work; don't "fix" it as a drive-by
  unless the task is actually about that screen.

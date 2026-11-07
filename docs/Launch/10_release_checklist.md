# 10 — Release checklist (backup items)

Items a release must not skip. Each one exists because a past release or review found the gap. Add new items at the end with the plan that owns them.

## Before tagging, on every release (docs/Plan/14a P9, section 6)

1. **Export a golden backup from each release build, on both platforms, before tagging.**
   - Install the release build (Android: the `bundleRelease` or `assembleRelease` build; iOS: the TestFlight build) on a device or simulator that holds **synthetic data only** (never the owner's data). Back Up with the password `BackupPass#1`.
   - Name the files `jvm-<versionName>-v<format>.pcda` (Android) and `ios-<versionName>-v<format>.pcda` (iOS), put them in `shared/src/commonTest/resources/golden-backups/`, and add one entry each to `manifest.json` with the counts the data held. The tests then restore them on both platforms with no code change.
   - Decrypt and scan each for PII before committing. **Never regenerate or overwrite a golden that is already committed**: a golden is a file an app really wrote.
   - Run `./gradlew :shared:testDebugUnitTest --tests "*GoldenBackup*"` and `./gradlew :shared:iosSimulatorArm64Test --tests "*GoldenBackup*"`.
2. **Bumping `PortableBackup.CURRENT_VERSION` needs a golden of the new format** (`GoldenBackupRestoreTest.everyFormatUpToTheCurrentOneHasAGolden` fails otherwise).
3. **Run the minified device smoke** (`scripts/run_guide_minified_smoke.sh`) on the Pixel. It now includes the Backup & Restore preview of a v5 golden on the R8 build.

## Release notes for the release that first writes backup format 6

Both stores (`composeApp/fastlane/release_notes/<version>`, `iosApp/fastlane/release_notes/<version>`) say, in plain words:

- **Restore backups on the latest version.** Apps up to 1.5.0 cannot read a format 6 backup (they show a decryption error), and shipped code cannot be fixed. A backup made on this version or later must be restored on this version or later.
- Crash reports no longer carry non-fatal log errors (`Logger.e` records nothing to Crashlytics now), so a drop in non-fatal counts after this release is expected, not an improvement.

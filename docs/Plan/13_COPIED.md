# Orders Bank: what was copied from the frozen branch, and the baseline

Source of every copy: `origin/feature/part2-orders` at `22554101` (tag `archive/part2-orders-2026-10-30`). Nothing is merged; each file below was
copied by path or ported by hand, then tested on this branch. The list grows every phase (plan 13a, Phase 0 step 3).

## Baseline (feature/orders-bank, unchanged tree, 2026-10-30)

| Check | Result |
|---|---|
| `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` | BUILD SUCCESSFUL |
| shared, debug / release unit tests | 1188 / 1188, 0 failed; release has 1 skipped |
| composeApp, debug / release unit tests | 1079 / 1053, 0 failed, 0 skipped |
| iOS simulator tests, shared / composeApp | 1142 / 669, 0 failed, 0 skipped |
| Corpus (`TokenParseCorpusRegressionTest`, 139 fixtures in `index.json`) | pass |
| `assembleRelease -PallowPlaceholderGemmaModel=true`, `check_r8_guide.py` | pass; 21 models kept, bundle byte-identical |
| Python: `scripts/tests` / rule-card tools | 30 / 265 pass |
| Release merged manifest | 11 permissions, 1 declared, 0 features, 3 exported components |

Pre-existing skip (main, not this work): `GemmaModelPathsAndroidTest`, one test, release variant only.

## Phase 0

| File here | From the old branch | How |
|---|---|---|
| `shared/.../crypto/DecryptionFailedException.kt`, `PcdaMark.kt`, `androidMain/.../AndroidDecryptFailure.kt`, `iosMain/.../IosDecryptFailure.kt` | same paths, commit `915e62a9` | verbatim |
| both `CryptoHelper.kt` (android, ios) | same paths | the old diff applied (uses `PcdaMark`, retypes failures) |
| `shared/.../repository/PortableBackup.kt` (`BackupDamagedException`) | `part2/data/Part2Backup.kt` | one class, moved to the repository package |
| `PayslipBackupService.kt` | same path | a 4-line catch; the rest of that file's old diff is Part II backup and is not copied |
| `composeApp/.../ui/screens/RestoreFailureStatus.kt` | `BackupGuard.kt` (`restoreFailedStatus`) | rewritten for main: no Part II report type |
| `ui/theme/BackupRestoreStrings.kt` and its test | same paths | verbatim |
| tests: `DecryptFailureTypeTest`, `PcdaMarkTest`, `AndroidDecryptFailureTest`, `IosDecryptFailureTest`, `DecryptFailureSourceTest`, `PayslipBackupWrongPasswordTest` | same paths | verbatim, except the source test points at `RestoreFailureStatus.kt` and the wrong-password test drops one import |
| `RestoreStatusTest` | same path | rewritten: main restores `Result<Unit>`, so the "rows left out" cases are not copied |
| `iosApp/iosApp/PrivacyInfo.xcprivacy` (disk-space entry E174.1), `scripts/tests/test_privacy_manifest.py` | commit `6c8877e6` | entry re-typed, test verbatim |
| `BaseModelDownloadBanner.kt` (`FlowRow`), `BaseModelDownloadBannerLayoutUiTest` | commit `95f5c4a3` | 3-line change, test verbatim |
| `scripts/check_merged_manifest.py` and its tests | same paths | verbatim |
| `scripts/merged_manifest_baseline.json` | same path | regenerated from main's release manifest; each entry keeps the reason reviewed on the old branch |
| `scripts/run_flag_on_tests.py` and its tests, `Part2FlagExpectation.kt` | same paths | verbatim except two comment lines and the value (false) |
| `Part2AvailabilityTest` | same path | rewritten: only `isPart2Enabled`, the flag pin and the catalog pin |
| `shared/schemas/.../15.json` | same path | byte-identical (sha256 `108917b0…b9b7`) |
| `part2/data/Part2Entities.kt` | same path | `Part2OrderEntity`, `Part2PageEntity` unchanged; `Part2DraftEntity` replaced by `Part2EntryEntity` |

Not copied in Phase 0 on purpose: `Part2Dao.kt` (no caller until Phases 3 and 4; it arrives with its first use).

# Part II Orders: Play Console and App Store wording (Phase 11c, item 40)

**Written for: the owner, who enters this by hand in Google Play Console, App Store Connect and the privacy-policy page.** The
repository cannot touch any of them. Written 2026-10-28, with `LaunchFlags.PART2_ENABLED` on (`DECISIONS.md` Round 40). Nothing here
has been entered, uploaded or submitted.

How to read it. Every statement about **this app** cites its evidence as `file:line`, a baseline entry or a test. A statement about
**Google's or Apple's rules** cannot be proved from this repository; it is marked **Console to confirm**, with the page it came from,
and the owner checks it in the Console before pressing submit. `scripts/tests/test_store_wording_doc.py` fails if a permission or an
exported component in `scripts/merged_manifest_baseline.json` is not named below, if a citation points past the end of its file, or if
the phase plan or runbook section 9 stop linking here.

## 1. In one page

| Question | Answer |
|---|---|
| Does Part II send anything off the device? | No. No Part II source file uses the network, a log, a crash report or the clipboard (tests in section 5). |
| What new permissions does the Android release declare? | `POST_NOTIFICATIONS` (the app) and `RECEIVE_BOOT_COMPLETED` (WorkManager). They are in the release **whether or not Part II is on**: the manifest does not follow the flag (`composeApp/src/androidMain/AndroidManifest.xml:6`). |
| Camera permission on Android? | None. The scanner runs inside Google Play services (`shared/src/androidMain/kotlin/com/payslipmax/pdfparser/part2/pages/MlKitScanSource.kt:25`). |
| What does the Play Data safety form need? | Probably no new row (section 5). **Console to confirm** against the form as it stands. |
| What does App Store Connect need? | No new data-type answer (section 7). The manifest now also declares the disk-space API (section 7.4, Phase 11d), which Apple will validate at upload. |
| What is new for the privacy policy? | One paragraph (section 6). |

## 2. Permissions (Play Console: App content and the store listing)

The release merged manifest has 13 permissions and 1 declared permission; the gate is `scripts/check_merged_manifest.py`, which prints
"13 permissions, 1 declared, 0 features, 5 exported components" and compares them with `scripts/merged_manifest_baseline.json`. Part II
added two of the 13 (Phase 7b). The other 11 were already declared in earlier releases and are unchanged; they are listed so the
document covers the whole file.

### 2.1 Added for Part II

**`android.permission.POST_NOTIFICATIONS`**: declared by the app (`composeApp/src/androidMain/AndroidManifest.xml:6`; baseline entry
`scripts/merged_manifest_baseline.json:55`).
- Why: a printed Part II slip can set a reminder ("add the published order", "draft your rejoining slip"). Android 13 and later ask the
  user before any notification can show.
- When the user is asked: once, after the app's own explainer, and only when a printed slip first sets a reminder; a decline is final and
  the Due list still works (`composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme/Part2ReminderStrings.kt:19-24`;
  `shared/src/commonMain/kotlin/com/payslipmax/pdfparser/part2/reminders/ReminderPromptStore.kt:4-5`; `DECISIONS.md` Round 28).
- What the notification says: one fixed sentence, "A Part II reminder is due.", under the title "PayslipMax". It never names a code, an
  order number or a date (`composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme/Part2ReminderStrings.kt:12-13`;
  `shared/src/commonMain/kotlin/com/payslipmax/pdfparser/part2/reminders/ReminderNotificationText.kt:3-7`).
- Suggested wording for the Console's permission explanation: *"PayslipMax can remind the officer, at 09:00 on the due day, that a Part II
  order slip they printed needs the next step. The notification is one generic sentence. It is asked for once, after an in-app
  explanation, and the app works fully if it is declined."*
- **Console to confirm**: POST_NOTIFICATIONS is, to my knowledge, not one of Play's restricted permissions and needs no declaration form
  (source: Play Console Help, "Use of permissions"); the Console shows whether it asks for one.

**`android.permission.RECEIVE_BOOT_COMPLETED`**: not declared by the app. It comes in from WorkManager 2.11.2
(`gradle/libs.versions.toml:31`; baseline entry `scripts/merged_manifest_baseline.json:59`).
- Why: Android drops queued jobs when the phone restarts, so without it a scheduled reminder is lost until the user next opens the app.
  This was found on a real phone and fixed (Phase 7b): the app's own receiver, `ReminderBootReceiver`, rebuilds the schedule once from
  the drafts (`composeApp/src/androidMain/kotlin/com/payslipmax/pdfparser/reminders/ReminderBootReceiver.kt:10-24`).
- That receiver is **not exported**, checks the action, does nothing while Part II is off, and logs nothing
  (`composeApp/src/androidMain/AndroidManifest.xml:56-63`; `composeApp/src/androidMain/kotlin/com/payslipmax/pdfparser/reminders/ReminderBootReceiver.kt:10-16`).
- Suggested wording: *"Lets the app re-schedule the user's own reminders after the phone restarts. Nothing is sent anywhere."*
- **Console to confirm**: as above, not a restricted permission to my knowledge.

### 2.2 Declared earlier, unchanged (reason lines are in the baseline)

| Permission | Baseline entry | What it is for |
|---|---|---|
| `INTERNET` | `scripts/merged_manifest_baseline.json:11` | Firebase Analytics and Crashlytics (sanitized) and RevenueCat billing |
| `ACCESS_NETWORK_STATE` | `scripts/merged_manifest_baseline.json:15` | Firebase Analytics: send only when a network exists |
| `WAKE_LOCK` | `scripts/merged_manifest_baseline.json:19` | Firebase Analytics: stay awake while a batch uploads |
| `BIND_GET_INSTALL_REFERRER_SERVICE` | `scripts/merged_manifest_baseline.json:23` | Firebase Analytics: Play install referrer |
| `AD_ID` (`com.google.android.gms.permission.AD_ID`) | `scripts/merged_manifest_baseline.json:27` | the advertising-ID permission Play asks the app to declare |
| `BILLING` (`com.android.vending.BILLING`) | `scripts/merged_manifest_baseline.json:31` | Premium purchases |
| `FOREGROUND_SERVICE` | `scripts/merged_manifest_baseline.json:35` | Play Asset Delivery: unpacking the Gemma model pack |
| `FOREGROUND_SERVICE_DATA_SYNC` | `scripts/merged_manifest_baseline.json:39` | the same extraction (target SDK 34 and later) |
| `ACCESS_ADSERVICES_ATTRIBUTION` | `scripts/merged_manifest_baseline.json:43` | Firebase Analytics: Privacy Sandbox attribution |
| `ACCESS_ADSERVICES_AD_ID` | `scripts/merged_manifest_baseline.json:47` | Firebase Analytics: Privacy Sandbox advertising ID |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (`in.aiborne.payslipmax.…`) | `scripts/merged_manifest_baseline.json:51` and, as the one declared permission, `:65` | the app's own signature-level permission that keeps dynamically registered receivers private (androidx.core) |

No permission in the file is a camera, storage, media, contacts or location permission, and there is no `uses-feature` entry
(`scripts/merged_manifest_baseline.json:3`, the `about` text, and its empty `features` list; `Part2PagesSourceTest` pins "no camera permission",
`shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/pages/Part2PagesSourceTest.kt:70-80`).

## 3. WorkManager and the other exported components

Five components are exported; every one is in the baseline with the permission that guards it, so none can be started by another app
(the check compares `permission` and `enabled` too, `scripts/check_merged_manifest.py`).

| Component | Baseline entry | Guarded by | Plain-words reason |
|---|---|---|---|
| `com.payslipmax.pdfparser.MainActivity` | `scripts/merged_manifest_baseline.json:74` | none (the launcher) | the only way in; a reminder tap opens it with a known reminder kind and a plain id, and the draft must still exist |
| `androidx.work.impl.background.systemjob.SystemJobService` | `scripts/merged_manifest_baseline.json:102` | `BIND_JOB_SERVICE` (system only) | where WorkManager runs the reminder job; Android must be able to bind it |
| `androidx.work.impl.diagnostics.DiagnosticsReceiver` | `scripts/merged_manifest_baseline.json:95` | `DUMP` (shell and system only) | an `adb` diagnostics broadcast; reports job counts to `adb` and sends nothing out |
| `androidx.profileinstaller.ProfileInstallReceiver` | `scripts/merged_manifest_baseline.json:81` | `DUMP` | baseline-profile installation (earlier release) |
| `com.google.android.play.core.assetpacks.AssetPackExtractionService` | `scripts/merged_manifest_baseline.json:88` | none, but shipped **disabled** | Play Asset Delivery turns it on when it unpacks the Gemma model (earlier release) |

WorkManager starts on demand: its automatic start-up entry is removed, so a build without Part II starts as it did before
(`composeApp/src/androidMain/AndroidManifest.xml:44-53`). WorkManager also adds non-exported components (a foreground service, two
reboot or force-stop receivers) that do not need a Console entry; the merged manifest after `assembleRelease` lists them.
The reminder job reads the drafts on the device and posts one local notification; it uses no network
(`composeApp/src/androidMain/kotlin/com/payslipmax/pdfparser/reminders/ReminderWorker.kt:19-23`; the Part II source tests in section 5).

## 4. The document scanner (ML Kit, Google Play services)

- What it is: Google's ML Kit Document Scanner 16.0.0 (`gradle/libs.versions.toml:28`), started from the vault's "Scan a published order".
  It adds three components to the merged manifest, all `exported=false`: a delegate activity, an init provider and a discovery service
  (`scripts/merged_manifest_baseline.json:3`, the `about` text).
- No camera permission in the app: the scanner runs in Play services' process and uses Play services' own camera permission
  (`shared/src/androidMain/kotlin/com/payslipmax/pdfparser/part2/pages/MlKitScanSource.kt:25-26`). Google's page says "No camera
  permission is needed from your app" (https://developers.google.com/ml-kit/vision/doc-scanner, read 2026-10-28).
- What it is set to: photos only (JPEG), no gallery import, at most the page limit
  (`shared/src/androidMain/kotlin/com/payslipmax/pdfparser/part2/pages/MlKitScanSource.kt:60-63`). Pages come back as files in the app's own cache, are read once, encoded, stored encrypted and deleted
  (`shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/pages/Part2PagesSourceTest.kt:35-48` keeps pages away from the gallery, external storage, the file provider, the clipboard and the network).
- Where the processing happens: Google states "The entire document scanner flow operates on-device" and "On-device processing,
  preserving user's privacy" (same page). The first use may download the scanner's models through Play services (the page says users
  "might have to wait for the models, logic and UI flow to download"); that download is Google's, not the app's.
- A phone without Play services or without the module reports the scan as unavailable (`ScanFailure.UNAVAILABLE`,
  `shared/src/androidMain/kotlin/com/payslipmax/pdfparser/part2/pages/MlKitScanSource.kt:37-47`); nothing else depends on it.
- **Console to confirm**: whether Google's own Data safety guidance for the Document Scanner asks the app to declare anything. The ML Kit
  data-disclosure page (https://developers.google.com/ml-kit/android-data-disclosure) does not list the Document Scanner at all (read
  2026-10-28), so I could not confirm either way; I found nothing that requires a new row.

## 5. Data safety (Play Console: App content, Data safety)

**What Part II keeps, and where.**
- Drafts, orders and scanned pages are stored in the app's Room database, each as an AES-256-GCM ciphertext under the device key
  (`shared/src/commonMain/kotlin/com/payslipmax/pdfparser/part2/data/Part2Codec.kt:12`, `:30`, `:97`;
  `shared/src/androidMain/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt:10`). Android auto-backup is off
  (`composeApp/src/androidMain/AndroidManifest.xml:17`).
- Nothing in Part II is sent: `shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/pages/Part2PagesSourceTest.kt:35-48` (no network class in the page pipeline), `shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/data/Part2StorageSourceTest.kt:53`
  (storage: no network, clipboard or regex) and `shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/print/Part2PrintSourceTest.kt:30` (printing: no sharing, writing, clipboard or network);
  no log, crash report or telemetry call: `shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/pages/Part2PagesSourceTest.kt:50`, `shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/data/Part2StorageSourceTest.kt:44`, `shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/print/Part2PrintSourceTest.kt:44`,
  `shared/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/part2/domain/Part2EngineSourceTest.kt:63`.
- The only way a draft, order or page leaves the phone is inside the user's own `.pcda` backup: one archive, encrypted with the user's
  password, written when the user taps Back Up and handed to the share sheet (`shared/src/commonMain/kotlin/com/payslipmax/pdfparser/repository/PayslipBackupService.kt:30-31`
  and `:74`). It does not depend on the device key (`shared/src/commonTest/kotlin/com/payslipmax/pdfparser/part2/data/Part2BackupTest.kt:72`
  and `:97`). The developer never receives it.
- Reset App & Clear Data deletes Part II drafts, orders and pages with everything else
  (`shared/src/commonMain/kotlin/com/payslipmax/pdfparser/database/PayslipDao.kt:192-198`, called from
  `shared/src/commonMain/kotlin/com/payslipmax/pdfparser/repository/PayslipRepository.kt:158`).

**Suggested answers for the Part II data types** (the form as a whole is the owner's; its existing rows for analytics, crash logs and
billing are not changed by Part II):

| Data safety question | Part II answer | Why |
|---|---|---|
| Photos and videos | Not collected | Scanned order pages stay on the device (encrypted) and are not transmitted to the developer or anyone else. |
| Files and docs | Not collected | The `.pcda` backup is created and shared by the user to a place the user chooses; the developer does not receive it. |
| Personal info (name, user IDs, and so on) | Not collected by Part II | Drafts hold the officer's own typed words and dates on the device only. |
| Data shared with third parties | Nothing new | The scanner is Google Play services running on the device; no Part II data is passed to it beyond the camera image it takes itself. |
| Data encrypted in transit | Unchanged | Part II adds no transmission. |
| Users can request deletion | Unchanged | All Part II data is on the device; "Reset App & Clear Data" and uninstalling remove it. |

- **Console to confirm**: that Google's Data safety definition of "collect" ("transmit data off a user's device") applies to data the user
  themself exports through the share sheet to a place of their choice, and that on-device processing by a Google Play services module is
  not "sharing". My reading is that neither is collection or sharing, but that is Google's rule, not something this repository proves
  (source: Play Console Help, "Provide information for Google Play's Data safety section").
- **Console to confirm**: that the existing form still matches the app. This repository does not hold the form; what the app sends today
  is Firebase Analytics, Crashlytics (through `TelemetrySanitizer`, `shared/src/commonMain/kotlin/com/payslipmax/pdfparser/telemetry/TelemetrySanitizer.kt:8`)
  and RevenueCat billing. On iOS the same facts are declared in `iosApp/iosApp/PrivacyInfo.xcprivacy:27-66` (Device ID and Product
  Interaction for analytics, Crash Data for app functionality, none linked to the user, no tracking).

## 6. Privacy policy paragraph (for https://www.ai-borne.in/privacy-policy)

The page itself is not in this repository; the app links to it (`composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme/LegalStrings.kt:60`).
The in-app Help text now has a seventh point for Part II (Phase 11d, `composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme/LegalStrings.kt:42`); it says the same in fewer words. Suggested paragraph for the website:

> **Part II order drafting and the order vault.** PayslipMax can help you draft Part II order slips and keep scanned copies of published
> orders. Drafts, order numbers and dates, and scanned pages are stored only on your device, encrypted, and are never uploaded to us or
> to anyone else. Scanning on Android uses Google's document scanner, which runs on your device through Google Play services; on iPhone
> it uses Apple's document camera. The app can remind you when a printed slip needs a next step; reminders are generated on your device,
> contain one generic sentence and no personal details, and are optional: if you decline notifications the app still lists what is due.
> If you choose Back Up, your drafts, orders and scanned pages are included in the backup file, which is encrypted with the password you
> choose and saved or shared only where you send it. We cannot open it. "Reset App & Clear Data" removes everything described here from
> your device.

## 7. iOS and App Store Connect

7.1 **Camera.** `NSCameraUsageDescription` is the only privacy usage key in the app (`iosApp/iosApp/Info.plist:31-32`), and its text is
pinned to the reviewed copy: "PayslipMax uses the camera only when you scan a published Part II order into your vault. Pages are stored
encrypted on this device and are never added to your Photos." (`composeApp/src/androidUnitTest/kotlin/com/payslipmax/pdfparser/guide/Part2CameraUsageTest.kt:35-40`
fails if any other usage key is added). The scan uses Apple's document camera (`iosApp/iosApp/DocumentScanPresenter.swift`).

7.2 **Notifications.** iOS asks at run time, so there is no Info.plist key and nothing to declare in App Store Connect:
`UNUserNotificationCenter.requestAuthorization` is called once, after the same in-app explainer (`iosApp/iosApp/ReminderNotificationGateway.swift:5-14`).
No background mode is declared (`iosApp/iosApp/Info.plist` has no `UIBackgroundModes` key). Reminders are scheduled local notifications.

7.3 **Privacy manifest** (`iosApp/iosApp/PrivacyInfo.xcprivacy`). Required-reason APIs declared: file timestamps, reason 3B52.1
(`:16`), disk space, reason E174.1 (`:24`, added in Phase 11d, section 7.4) and UserDefaults, reason CA92.1 (`:32`). Part II uses UserDefaults for the "already asked about reminders" flag
(`shared/src/iosMain/kotlin/com/payslipmax/pdfparser/part2/reminders/ReminderPromptStore.ios.kt:3-9`), which CA92.1 (app-only access to
the app's own data) covers; Part II adds no other required-reason API that I found (a search of `iosApp` and `shared/src/iosMain` for
file-timestamp, boot-time, disk-space and active-keyboard calls found only the item below).

7.4 **Not Part II's, found and fixed in Phase 11d: the disk-space API.** `shared/src/iosMain/kotlin/com/payslipmax/pdfparser/insights/gemma/DeviceCapabilityManager.ios.kt:45`
reads `NSFileSystemFreeSize` (Apple's "disk space" category) to decide whether the offline model can be installed; the number is neither shown
nor sent (the screen says the fixed line "Requires 1.5GB free storage", `composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/PayslipViewModelGemmaExtensions.kt:145-146`).
The manifest now declares `NSPrivacyAccessedAPICategoryDiskSpace` with reason E174.1 (`iosApp/iosApp/PrivacyInfo.xcprivacy:19-26`), and
`scripts/tests/test_privacy_manifest.py` fails if any first-party code uses a required-reason API (disk space, file timestamps, boot time,
UserDefaults, active keyboards) without a declaration. **Console to confirm** (Apple's "Describing use of required reason API" page, which
needs a browser and could not be read here): that E174.1 is the right code for "decide whether there is room to install" and that the value
may stay on the device; the wording I found in secondary sources matches, 85F4.1 (show it to the person) does not apply.

7.5 **App Privacy "nutrition label".** The label lives only in App Store Connect (App Privacy), which this repository cannot read, so I
cannot say what it holds today. What the app itself declares is `iosApp/iosApp/PrivacyInfo.xcprivacy:27-66`: Device ID and Product Interaction (analytics)
and Crash Data (app functionality), none linked to the user, no tracking (`:5`). Part II collects nothing off the device, so **no new
data type is needed**. The owner's step: open App Store Connect, App Privacy, and check that it says the same as the manifest. Apple's
definition of "collect" (data transmitted off the device) is what excludes the scanned pages and drafts: **Console to confirm** (App
Store Connect Help, "App privacy details").

7.6 **Export compliance.** `ITSAppUsesNonExemptEncryption` is `false` (`iosApp/iosApp/Info.plist:25-26`). Part II encrypts with the same
`CryptoHelper` as the payslip database (`shared/src/commonMain/kotlin/com/payslipmax/pdfparser/part2/data/Part2Codec.kt:30`), so nothing changes in kind. **Console to confirm** with the answer already
given for the app.

7.7 **Review notes for the next submission** (`iosApp/fastlane/release_notes/<version>/review_notes.txt`): *"Guide, Part II is a Premium
feature. Scanning a published order opens Apple's document camera (the camera permission text explains it). Notifications are optional
and asked once; the notification text is the same generic sentence for every reminder."*

## 8. What the owner does

Google Play Console (before the first upload with Part II on):
1. App content, Data safety: open the form and check it against section 5; no Part II row is expected. Save only if something differs.
2. Permissions: if the Console asks about notifications or boot, paste the wording in section 2.1.
3. Privacy policy page: add the paragraph in section 6; the link stays `https://www.ai-borne.in/privacy-policy`.
4. Store listing: no change is required by Part II; the listing text is yours.

App Store Connect (before the next submission):
1. App Privacy: check it matches section 7.5; no change for Part II.
2. Upload as usual: Apple validates the privacy manifest then. If it reports a missing or wrong reason code (ITMS-91053), section 7.4 is where to look.
3. Paste the review notes in section 7.7.

## 9. What keeps this true

- `scripts/tests/test_store_wording_doc.py`: every permission, declared permission, feature and exported component in the baseline is named
  here; every citation points at a real line; the plan and runbook link here.
- `scripts/tests/test_privacy_manifest.py`: the iOS privacy manifest declares every required-reason API the app's own code uses.
- `scripts/check_merged_manifest.py` (CI and pre-push): the release manifest equals the baseline.
- `Part2PagesSourceTest`, `Part2StorageSourceTest`, `Part2PrintSourceTest`, `Part2EngineSourceTest`: no network, log, telemetry or
  clipboard in Part II code. `Part2CameraUsageTest`: the iOS camera text, and no other usage key.
- `Part2BackupTest`: the backup archive is under the password, not the device key.

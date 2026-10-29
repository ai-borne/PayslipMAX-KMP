# Part II Orders: iPhone checklist (Phase 10)

This closes Phase 11 items 14, 20 and 47 of `docs/Plan/12_Part2Orders_PhasePlan.md`. Only a real iPhone can: the simulator has no
document camera and no AirPrint. Tick each line and write anything odd next to it; reply with the list.

**Rules for the walk.** Use made-up dates, station and Corps names only. Scan a public page (a leaflet, a blank form), never a real
person's document, and do not screenshot History, Dashboard or the Leave Bank table. Delete anything you saved to Files afterwards.

**Getting the build.** The flag is off in the repo. Make a copy of the tree, set `LaunchFlags.PART2_ENABLED = true` in the copy only,
build it to the iPhone from Xcode (`iosApp/iosApp.xcodeproj`), and delete the copy afterwards. Do not upload this build anywhere.
A debug build shows a developer section at the bottom of Settings: set Force Premium for the walk, Force Free for step 9, and use
its reminder switch (a few seconds from now) for step 6. A release build has neither.

## 1. Browse and search (item 14)
- [ ] Guide tab shows a Part II row. Part II home shows Leave and Records tiles. Light and dark both read.
- [ ] Leave opens its list; a code page opens (try GTDAL). Search by code (`rejoinal`), by serial (`3.28`) and by name works.
- [ ] Back at every level goes up one level (the on-screen back header; the edge swipe does not apply inside a tab). Back from the
      top of Part II lands on Guide Home.
- [ ] A row marked "Seen in practice" shows that label, and a row with no page shows none.

## 2. Draft forms (items 14 and 47)
- [ ] Annual leave (GTDAL): date pickers, chips and the order reference box work with the keyboard up and down.
- [ ] Type a leave of 14 days with the casual leave code: the HAA questions appear with their boxes (corps, notification, serial);
      at 13 days they do not. At 15 days the field-area boxes appear (station, operation, sanction date).
- [ ] Annual leave covering a whole calendar month asks the Transport Allowance questions (station, disability); a part month does not.
- [ ] The retirement date, the two NCC questions and the earlier-allowances question appear on the annual leave form.
- [ ] ACCLVE: "Fill from your Leave Bank" asks for the year first; with a year it fills the figures and shows the cap note; typing over
      a figure then pressing it asks "Refill" with Replace and Keep mine. ENCASH shows the multi-line break-up box with the missing-years
      and statement-total notes. (Do not read the figures aloud or record them.)
- [ ] The slip preview shows the rows in date order, with each certificate and its page note. Drafts list: delete asks first.

## 3. Print (item 14)
- [ ] Print opens the iOS print sheet. Pick an AirPrint printer or Save to Files; "Printed on" appears after printing.
- [ ] Cancel on the sheet leaves the slip unprinted ("Printed on" does not appear).
- [ ] While the sheet is up, the edge swipe does not dismiss the sheet or leave the slip. After a change, "Changed since printed" shows.
- [ ] "Check the HRMS draft" opens in light and dark and reads.

## 4. Scan with VisionKit (item 20)
- [ ] Scan from the Part II home: the camera permission prompt appears with the app's own sentence. Allow it.
- [ ] Scan two pages; both reach the order screen. Photos has no new image.
- [ ] Cancel from the scanner returns with nothing added. Deny the permission (Settings, then retry): the app says so and does not crash.
- [ ] Lock the app (passcode on, background it, return) while the camera is up: the camera closes.
- [ ] Order screen: number and date, view and hide a page, Add pages, Open the draft, delete with its confirmation.
- [ ] A published cancel order is scanned like any other.

## 5. Cancel (item 14)
- [ ] "Cancel this order" and "Cancel and re-draft" on a vault order; the cancel slip and its print sheet; the hop between the two
      slips; the time-bar note; the note on the cancelled order.

## 6. Reminders and notifications (items 14 and 20)
- [ ] The Due row on the Part II home shows its count, or "Nothing is due right now."; the list shows Due now (overdue colour), Coming up,
      an empty list and a Dismiss button on a row; a row opens its slip.
- [ ] The Dashboard card after an import ("See what is due" opens Due; "Not now" hides it). This needs a real import: skip it if you
      would rather not, and say so.
- [ ] The explainer appears first, then the system notification prompt. Decline once: it is never asked again and the Due list still works.
- [ ] Allow it, turn on the reminder switch in Settings so a reminder falls due in seconds, close the app: the banner reads only
      "PayslipMax / A Part II reminder is due." Tap it with the app open, with a detail screen pushed (it closes first), with the app
      closed, and while the app is locked (it waits, then opens Due).
- [ ] Restart the phone: the pending reminder is still delivered. Settings, Notifications, lists PayslipMax.
- [ ] (Phase 12a, item 39) Decline the notification prompt, then open Part II, Due: the list opens with "Reminders are off. You can turn them on
      in your phone's notification settings." and an "Open settings" button. The button opens PayslipMax's page in the Settings app. Turn
      Notifications on there and come back: the note is gone. With reminders allowed (or before the prompt was ever answered) the note is not there.
      After Allow on the explainer while Due is open, no note should remain (if it does, say so: the check runs when the screen appears and on return to the app).

## 7. Native back and lock
- [ ] Back from every Part II screen (form, slip, vault order, Due, check screen) goes where the screen above it is. The edge swipe never
      loses a half-typed draft (it is saved on the way out).
- [ ] With the passcode lock on, backgrounding and returning shows the lock screen; the Part II screens are not visible behind it.

## 8. Backup and restore
- [ ] Settings, back up (.pcda) saves a file; restore into the same app brings the drafts and orders back; the restore message counts
      anything it could not read.
- [ ] Restore with a wrong password, then with a file that is not a backup (any other file): both read "Wrong password, or this isn't a PayslipMax backup" (Phase 11 item 60). No "error", no code or number, and nothing on the phone changes.

## 9. A lapsed officer
- [ ] Set the app to Free: the Part II lookup still works; drafting says it is a Premium feature and opens the upgrade panel; saved
      drafts and orders can be viewed, deleted and backed up; the Due list shows the upgrade panel.

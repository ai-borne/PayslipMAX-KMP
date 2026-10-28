"""Phase 11d (item 61): the iOS privacy manifest must declare every required-reason API the app's own code uses.

Apple rejects a build (ITMS-91053) whose code uses a required-reason API without a declared reason, and the error comes only at upload.
`DeviceCapabilityManager.ios.kt` read the free disk space for several releases with no declaration; nothing in the repository noticed.
This scans first-party Swift and iOS Kotlin for the API names of Apple's required-reason categories and fails when a category in use is
missing from `iosApp/iosApp/PrivacyInfo.xcprivacy`, is declared with no reason, or with a reason that belongs to another category.
Third-party SDKs carry their own manifests and are not scanned. The lists below are Apple's published ones; extend them if Apple adds one.
Run: python3 -m unittest discover -s scripts/tests -p 'test_privacy_manifest.py'
"""
import os
import plistlib
import re
import unittest

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
MANIFEST = os.path.join(ROOT, "iosApp", "iosApp", "PrivacyInfo.xcprivacy")
SCANNED = ("iosApp/iosApp", "shared/src/iosMain", "composeApp/src/iosMain")
SUFFIXES = (".swift", ".kt", ".m", ".mm")

# category -> (the API names that put the app in it, the reason codes Apple allows for it)
CATEGORIES = {
    "NSPrivacyAccessedAPICategoryFileTimestamp": (
        ("NSFileCreationDate", "NSFileModificationDate", "NSURLContentModificationDateKey", "NSURLCreationDateKey",
         "contentModificationDateKey", "creationDateKey", "getattrlist", "fstat", "lstat"),
        ("DDA9.1", "C617.1", "3B52.1", "0A2A.1"),
    ),
    "NSPrivacyAccessedAPICategorySystemBootTime": (
        ("systemUptime", "mach_absolute_time"),
        ("35F9.1", "8FFB.1", "3D61.1"),
    ),
    "NSPrivacyAccessedAPICategoryDiskSpace": (
        ("NSFileSystemFreeSize", "NSFileSystemSize", "volumeAvailableCapacityKey", "volumeTotalCapacityKey", "NSURLVolumeAvailableCapacityKey",
         "NSURLVolumeTotalCapacityKey", "volumeAvailableCapacityForImportantUsageKey", "volumeAvailableCapacityForOpportunisticUsageKey",
         "statfs", "statvfs", "fstatfs"),
        ("E174.1", "85F4.1", "7D9E.1", "B728.1"),
    ),
    "NSPrivacyAccessedAPICategoryUserDefaults": (
        ("NSUserDefaults", "UserDefaults"),
        ("CA92.1", "1C8F.1", "C56D.1", "AC6B.1"),
    ),
    "NSPrivacyAccessedAPICategoryActiveKeyboards": (
        ("activeInputModes",),
        ("3EC4.1", "54BD.1"),
    ),
}


def word_in(name, text):
    return re.search(r"(?<![A-Za-z0-9_])" + re.escape(name) + r"(?![A-Za-z0-9_])", text) is not None


def categories_used(sources):
    """{category: [source file, ...]} for the required-reason API names that appear in the given {path: text}."""
    used = {}
    for path, text in sources.items():
        for category, (names, _) in CATEGORIES.items():
            if any(word_in(name, text) for name in names):
                used.setdefault(category, []).append(path)
    return used


def declared(plist):
    return {entry["NSPrivacyAccessedAPIType"]: list(entry.get("NSPrivacyAccessedAPITypeReasons", [])) for entry in plist.get("NSPrivacyAccessedAPITypes", [])}


def problems(used, declarations):
    found = []
    for category, files in sorted(used.items()):
        reasons = declarations.get(category)
        where = ", ".join(sorted(files)[:2])
        if reasons is None:
            found.append(f"{category} is used ({where}) but not declared")
        elif not reasons:
            found.append(f"{category} is declared with no reason")
        else:
            allowed = CATEGORIES[category][1]
            found += [f"{category}: reason {r} is not one of {allowed}" for r in reasons if r not in allowed]
    for category, reasons in sorted(declarations.items()):
        if category not in CATEGORIES:
            found.append(f"{category} is declared but this test does not know the category (add it with Apple's reason list)")
    return found


def first_party_sources(root=ROOT):
    sources = {}
    for folder in SCANNED:
        for base, _, files in os.walk(os.path.join(root, folder)):
            if "/build" in base or "/Products" in base:
                continue
            for name in files:
                if name.endswith(SUFFIXES):
                    path = os.path.join(base, name)
                    with open(path, encoding="utf-8", errors="replace") as fh:
                        sources[os.path.relpath(path, root)] = fh.read()
    return sources


class CheckerTest(unittest.TestCase):
    """The checks themselves, on synthetic input, so they cannot pass by scanning nothing."""

    DISK = "NSPrivacyAccessedAPICategoryDiskSpace"
    DEFAULTS = "NSPrivacyAccessedAPICategoryUserDefaults"

    def plist(self, **by_category):
        return {"NSPrivacyAccessedAPITypes": [{"NSPrivacyAccessedAPIType": c, "NSPrivacyAccessedAPITypeReasons": r} for c, r in by_category.items()]}

    def test_a_used_but_undeclared_category_is_reported(self):
        used = categories_used({"a/X.kt": "val free = attributes?.get(NSFileSystemFreeSize)"})
        self.assertEqual({self.DISK: ["a/X.kt"]}, used)
        self.assertEqual(1, len(problems(used, declared(self.plist()))))

    def test_a_declared_category_with_a_reason_of_its_own_passes(self):
        used = categories_used({"a/X.swift": "UserDefaults.standard"})
        self.assertEqual([], problems(used, declared(self.plist(**{self.DEFAULTS: ["CA92.1"]}))))

    def test_a_reason_that_belongs_to_another_category_is_reported(self):
        used = categories_used({"a/X.swift": "UserDefaults.standard"})
        self.assertEqual(1, len(problems(used, declared(self.plist(**{self.DEFAULTS: ["E174.1"]})))))

    def test_a_declaration_with_no_reason_is_reported(self):
        used = categories_used({"a/X.swift": "UserDefaults.standard"})
        self.assertEqual(1, len(problems(used, declared(self.plist(**{self.DEFAULTS: []})))))

    def test_a_longer_name_does_not_count_as_the_api(self):
        self.assertEqual({}, categories_used({"a/X.kt": "val MyUserDefaultsHelper = 1; statfsLike()"}))

    def test_an_unknown_declared_category_is_reported_so_the_list_cannot_go_stale(self):
        self.assertEqual(1, len(problems({}, {"NSPrivacyAccessedAPICategoryNew": ["X.1"]})))


class ShippedManifestTest(unittest.TestCase):
    def test_the_manifest_is_a_valid_plist_with_the_keys_apple_needs(self):
        with open(MANIFEST, "rb") as fh:
            data = plistlib.load(fh)
        self.assertIs(False, data["NSPrivacyTracking"])
        self.assertIn("NSPrivacyAccessedAPITypes", data)
        self.assertIn("NSPrivacyCollectedDataTypes", data)

    def test_the_scan_finds_the_code_it_is_meant_to_watch(self):
        used = categories_used(first_party_sources())
        self.assertIn("NSPrivacyAccessedAPICategoryUserDefaults", used, "the scanned folders moved: UserDefaults is used by the app")

    def test_every_required_reason_api_the_app_uses_is_declared_with_a_valid_reason(self):
        with open(MANIFEST, "rb") as fh:
            declarations = declared(plistlib.load(fh))
        self.assertEqual([], problems(categories_used(first_party_sources()), declarations))


if __name__ == "__main__":
    unittest.main()

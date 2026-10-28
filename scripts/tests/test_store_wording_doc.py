"""Part II, Phase 11c (item 40): the store wording stays complete and true.

`scripts/merged_manifest_baseline.json` lists every permission, feature and exported component the release can reach. A library that adds
one is a reviewed edit to that file; this test makes the same edit fail until `docs/Launch/10_part2_store_wording.md` also says something
about it, so a Play listing can never go out with an unexplained permission. It also fails when a `file:line` the document cites no longer
exists, and when the plan or the runbook stops linking to the document.
Run: python3 -m unittest discover -s scripts/tests -p 'test_store_wording_doc.py'
"""
import json
import os
import re
import unittest

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DOC = os.path.join(ROOT, "docs", "Launch", "10_part2_store_wording.md")
DOC_NAME = "10_part2_store_wording.md"
BASELINE = os.path.join(ROOT, "scripts", "merged_manifest_baseline.json")
PLAN = os.path.join(ROOT, "docs", "Plan", "12_Part2Orders_PhasePlan.md")
RUNBOOK = os.path.join(ROOT, "docs", "Plan", "rule_cards", "18_content_update_runbook.md")
# What a Play or App Store submission needs worded, as headings the owner can find.
REQUIRED_HEADINGS = ("Permissions", "Data safety", "Privacy policy", "App Store Connect", "iOS")
CITATION = re.compile(r"`([A-Za-z0-9_./\-]+\.(?:kt|kts|xml|json|py|md|swift|plist|xcprivacy|pro|toml|yml)):(\d+)(?:-(\d+))?`")


def read(path):
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def baseline_names(baseline):
    """Every name the baseline lets the app have, from the four sections that widen its reach."""
    names = []
    for section in ("permissions", "declared_permissions", "features", "exported_components"):
        names += [entry["name"] for entry in baseline.get(section, [])]
    return names


def last_segment(name):
    return re.split(r"[.$]", name)[-1]


def unmentioned(doc_text, baseline):
    """Baseline names the document never says: neither the full name nor its last segment as a whole word."""
    missing = []
    for name in baseline_names(baseline):
        if name in doc_text:
            continue
        if re.search(r"(?<![A-Za-z0-9_])" + re.escape(last_segment(name)) + r"(?![A-Za-z0-9_])", doc_text):
            continue
        missing.append(name)
    return missing


def bad_citations(doc_text, root=ROOT):
    """Citations whose file is gone or whose line is past the end of the file (a moved or shortened source)."""
    problems = []
    for path, first, last in CITATION.findall(doc_text):
        full = os.path.join(root, path)
        if not os.path.isfile(full):
            problems.append(f"{path}: no such file")
            continue
        with open(full, encoding="utf-8", errors="replace") as fh:
            total = sum(1 for _ in fh)
        if int(first) < 1 or int(last or first) > total or int(last or first) < int(first):
            problems.append(f"{path}:{first}{'-' + last if last else ''}: the file has {total} lines")
    return problems


class CheckerTest(unittest.TestCase):
    """The checks themselves, on synthetic input, so they cannot pass by checking nothing."""

    BASE = {
        "permissions": [{"name": "android.permission.POST_NOTIFICATIONS"}, {"name": "com.android.vending.BILLING"}],
        "declared_permissions": [{"name": "in.x.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}],
        "features": [{"name": "android.hardware.camera"}],
        "exported_components": [{"name": "androidx.work.impl.background.systemjob.SystemJobService"}],
    }

    def test_every_name_missing_is_reported(self):
        self.assertEqual(len(baseline_names(self.BASE)), len(unmentioned("nothing relevant here", self.BASE)))

    def test_a_name_is_covered_by_its_full_name_or_its_last_segment(self):
        doc = "`android.permission.POST_NOTIFICATIONS` BILLING DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION camera SystemJobService"
        self.assertEqual([], unmentioned(doc, self.BASE))

    def test_a_longer_word_does_not_cover_a_shorter_name(self):
        doc = "POST_NOTIFICATIONS BILLING_PORTAL DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION camera SystemJobService"
        self.assertEqual(["com.android.vending.BILLING"], unmentioned(doc, self.BASE))

    def test_a_new_library_entry_fails_until_the_document_names_it(self):
        grown = json.loads(json.dumps(self.BASE))
        grown["permissions"].append({"name": "android.permission.RECORD_AUDIO"})
        doc = "POST_NOTIFICATIONS BILLING DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION camera SystemJobService"
        self.assertEqual(["android.permission.RECORD_AUDIO"], unmentioned(doc, grown))

    def test_a_citation_past_the_end_of_its_file_or_to_a_missing_file_is_reported(self):
        here = "scripts/tests/test_store_wording_doc.py"
        self.assertEqual([], bad_citations(f"`{here}:1` and `{here}:1-5`"))
        self.assertEqual(1, len(bad_citations(f"`{here}:99999`")))
        self.assertEqual(1, len(bad_citations("`scripts/no_such_file.py:3`")))
        self.assertEqual(1, len(bad_citations(f"`{here}:9-3`")))


class StoreWordingDocTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.doc = read(DOC) if os.path.isfile(DOC) else ""
        cls.baseline = json.loads(read(BASELINE))

    def test_the_document_exists(self):
        self.assertTrue(os.path.isfile(DOC), "docs/Launch/" + DOC_NAME + " is the owner's text for the Play Console and App Store Connect")

    def test_it_says_something_about_every_permission_and_exported_component_in_the_baseline(self):
        self.assertEqual([], unmentioned(self.doc, self.baseline),
                         "add these to the document (with a reason a store reviewer would accept), or remove them from the baseline")

    def test_it_has_the_sections_a_submission_needs(self):
        headings = [line.lstrip("# ").strip() for line in self.doc.splitlines() if line.startswith("#")]
        for wanted in REQUIRED_HEADINGS:
            self.assertTrue(any(wanted.lower() in heading.lower() for heading in headings), f"no heading about '{wanted}'")

    def test_every_citation_points_at_a_real_line(self):
        self.assertEqual([], bad_citations(self.doc))

    def test_it_cites_evidence_and_marks_what_only_a_console_can_confirm(self):
        self.assertGreaterEqual(len(CITATION.findall(self.doc)), 15, "every claim cites a file:line, a baseline entry or a test")
        self.assertTrue("Console to confirm" in self.doc, "rules of Google or Apple are not provable from this repository and must say so")

    def test_it_contains_no_personal_data_or_secrets(self):
        for pattern in (r"[A-Za-z0-9._%+-]+@(?!ai-borne\.in)[A-Za-z0-9.-]+\.[a-z]{2,}", r"\b[A-Z]{5}\d{4}[A-Z]\b", r"AIza[0-9A-Za-z_-]{20,}"):
            self.assertIsNone(re.search(pattern, self.doc), f"matches {pattern}")

    def test_the_plan_and_the_runbook_link_to_it(self):
        self.assertTrue(DOC_NAME in read(PLAN), "the phase plan must link the document")
        self.assertTrue(DOC_NAME in read(RUNBOOK), "runbook section 9 must link the document")


if __name__ == "__main__":
    unittest.main()

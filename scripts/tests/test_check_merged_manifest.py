import importlib.util
import io
import json
import os
import tempfile
import unittest
from contextlib import redirect_stdout

_SCRIPTS = os.path.join(os.path.dirname(__file__), "..")
_spec = importlib.util.spec_from_file_location("check_merged_manifest", os.path.join(_SCRIPTS, "check_merged_manifest.py"))
guard = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(guard)

ROOT = os.path.abspath(os.path.join(_SCRIPTS, ".."))
HEAD = '<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="x.y">'


def with_app(app, perms="", application_attrs='android:allowBackup="false"'):
    return f'{HEAD}{perms}<application {application_attrs}>{app}</application></manifest>'


def reasoned(items):
    return [dict(item, reason="why") for item in items]


def baseline(**kw):
    base = {"version": 1, "application": {"allowBackup": "false", "debuggable": None, "usesCleartextTraffic": None},
            "permissions": [], "declared_permissions": [], "features": [], "exported_components": []}
    base.update(kw)
    return base


class ParsingTest(unittest.TestCase):
    """What counts as a permission, a feature or an exported component: the three ways an app's reach grows without a line of its own code."""

    def test_uses_permission_and_the_sdk_23_form_are_both_permissions(self):
        parsed = guard.parse_manifest(with_app("", '<uses-permission android:name="android.permission.CAMERA"/>'
                                                   '<uses-permission-sdk-23 android:name="android.permission.READ_X"/>'))
        self.assertEqual({"android.permission.CAMERA", "android.permission.READ_X"}, set(parsed["permissions"]))

    def test_a_declared_permission_is_kept_with_its_protection_level(self):
        parsed = guard.parse_manifest(with_app("", '<permission android:name="x.y.P" android:protectionLevel="signature"/>'))
        self.assertEqual({"x.y.P": "signature"}, parsed["declared_permissions"])

    def test_a_feature_is_kept_with_whether_it_is_required(self):
        parsed = guard.parse_manifest(with_app("", '<uses-feature android:name="android.hardware.camera" android:required="true"/>'
                                                   '<uses-feature android:name="android.hardware.nfc"/>'))
        self.assertEqual({"android.hardware.camera": "true", "android.hardware.nfc": "true"}, parsed["features"])

    def test_a_feature_that_is_not_required_says_so(self):
        parsed = guard.parse_manifest(with_app("", '<uses-feature android:name="android.hardware.camera" android:required="false"/>'))
        self.assertEqual({"android.hardware.camera": "false"}, parsed["features"])

    def test_exported_true_is_exported_for_every_component_kind(self):
        app = "".join(f'<{kind} android:name="c.{kind}" android:exported="true"/>' for kind in ("activity", "activity-alias", "service", "receiver", "provider"))
        self.assertEqual({("activity", "c.activity"), ("activity-alias", "c.activity-alias"), ("service", "c.service"),
                          ("receiver", "c.receiver"), ("provider", "c.provider")}, set(guard.parse_manifest(with_app(app))["exported"]))

    def test_an_exported_value_that_is_not_a_plain_false_counts_as_exported(self):
        app = '<service android:name="c.S" android:exported="@bool/flag"/>'
        self.assertIn(("service", "c.S"), guard.parse_manifest(with_app(app))["exported"])

    def test_a_manifest_without_an_application_element_parses_to_nothing_exported(self):
        parsed = guard.parse_manifest(f"{HEAD}<uses-permission android:name=\"p\"/></manifest>")
        self.assertEqual(({"p": True}, {}, {}), (parsed["permissions"], parsed["exported"], parsed["application"]))

    def test_exported_false_is_not_exported_even_with_an_intent_filter(self):
        app = '<service android:name="c.S" android:exported="false"><intent-filter><action android:name="a"/></intent-filter></service>'
        self.assertEqual({}, guard.parse_manifest(with_app(app))["exported"])

    def test_an_intent_filter_without_the_attribute_is_exported(self):
        app = '<receiver android:name="c.R"><intent-filter><action android:name="a"/></intent-filter></receiver>'
        self.assertIn(("receiver", "c.R"), guard.parse_manifest(with_app(app))["exported"])

    def test_no_attribute_and_no_intent_filter_is_not_exported(self):
        self.assertEqual({}, guard.parse_manifest(with_app('<activity android:name="c.A"/><service android:name="c.S"/>'))["exported"])

    def test_a_provider_is_exported_only_when_it_says_so(self):
        app = '<provider android:name="c.P"><intent-filter><action android:name="a"/></intent-filter></provider>'
        self.assertEqual({}, guard.parse_manifest(with_app(app))["exported"])

    def test_an_exported_component_keeps_its_guarding_permission_and_whether_it_is_enabled(self):
        app = ('<receiver android:name="c.R" android:exported="true" android:permission="android.permission.DUMP"/>'
               '<service android:name="c.S" android:exported="true" android:enabled="false"/>')
        exported = guard.parse_manifest(with_app(app))["exported"]
        self.assertEqual({"permission": "android.permission.DUMP", "enabled": "true"}, exported[("receiver", "c.R")])
        self.assertEqual({"permission": None, "enabled": "false"}, exported[("service", "c.S")])

    def test_components_inside_a_comment_or_outside_application_do_not_count(self):
        xml = f'{HEAD}<application><!-- <service android:name="c.S" android:exported="true"/> --></application></manifest>'
        self.assertEqual({}, guard.parse_manifest(xml)["exported"])

    def test_application_flags_are_read_and_absent_means_none(self):
        parsed = guard.parse_manifest(with_app("", application_attrs='android:allowBackup="true" android:debuggable="true"'))
        self.assertEqual({"allowBackup": "true", "debuggable": "true", "usesCleartextTraffic": None}, parsed["application"])


class CompareTest(unittest.TestCase):
    def check(self, xml, base):
        return guard.compare(guard.parse_manifest(xml), base)

    def test_the_same_manifest_has_no_problem(self):
        base = baseline(permissions=reasoned([{"name": "android.permission.INTERNET"}]),
                        exported_components=reasoned([{"kind": "activity", "name": "c.A", "permission": None, "enabled": "true"}]))
        xml = with_app('<activity android:name="c.A" android:exported="true"/>', '<uses-permission android:name="android.permission.INTERNET"/>')
        self.assertEqual([], self.check(xml, base))

    def test_a_new_permission_is_named_and_the_fix_is_said(self):
        problems = self.check(with_app("", '<uses-permission android:name="android.permission.CAMERA"/>'), baseline())
        self.assertEqual(1, len(problems), problems)
        self.assertIn("android.permission.CAMERA", problems[0])
        self.assertIn("merged_manifest_baseline.json", problems[0])
        self.assertIn("reason", problems[0])

    def test_a_permission_that_is_gone_is_reported_so_the_baseline_stays_true(self):
        problems = self.check(with_app(""), baseline(permissions=reasoned([{"name": "android.permission.INTERNET"}])))
        self.assertEqual(1, len(problems), problems)
        self.assertIn("android.permission.INTERNET", problems[0])
        self.assertIn("no longer", problems[0])

    def test_a_new_exported_component_is_named(self):
        problems = self.check(with_app('<service android:name="c.S" android:exported="true"/>'), baseline())
        self.assertEqual(1, len(problems), problems)
        self.assertIn("service", problems[0])
        self.assertIn("c.S", problems[0])

    def test_a_component_that_became_exported_is_a_new_exported_component(self):
        base = baseline()
        self.assertEqual([], self.check(with_app('<service android:name="c.S" android:exported="false"/>'), base))
        self.assertEqual(1, len(self.check(with_app('<service android:name="c.S" android:exported="true"/>'), base)))

    def test_an_exported_component_that_is_gone_is_reported(self):
        base = baseline(exported_components=reasoned([{"kind": "service", "name": "c.S", "permission": None, "enabled": "true"}]))
        problems = self.check(with_app(""), base)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("c.S", problems[0])

    def test_losing_the_guarding_permission_of_an_exported_component_is_a_problem(self):
        base = baseline(exported_components=reasoned([{"kind": "receiver", "name": "c.R", "permission": "android.permission.DUMP", "enabled": "true"}]))
        problems = self.check(with_app('<receiver android:name="c.R" android:exported="true"/>'), base)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("android.permission.DUMP", problems[0])

    def test_an_exported_component_that_became_enabled_is_a_problem(self):
        base = baseline(exported_components=reasoned([{"kind": "service", "name": "c.S", "permission": None, "enabled": "false"}]))
        self.assertEqual(1, len(self.check(with_app('<service android:name="c.S" android:exported="true"/>'), base)))

    def test_a_new_feature_requirement_is_a_problem_because_it_narrows_who_can_install(self):
        problems = self.check(with_app("", '<uses-feature android:name="android.hardware.camera" android:required="true"/>'), baseline())
        self.assertEqual(1, len(problems), problems)
        self.assertIn("android.hardware.camera", problems[0])

    def test_a_feature_whose_requirement_changed_is_a_problem(self):
        base = baseline(features=reasoned([{"name": "android.hardware.camera", "required": "false"}]))
        problems = self.check(with_app("", '<uses-feature android:name="android.hardware.camera" android:required="true"/>'), base)
        self.assertEqual(1, len(problems), problems)
        self.assertIn("android.hardware.camera", problems[0])

    def test_a_new_declared_permission_or_a_changed_protection_level_is_a_problem(self):
        base = baseline(declared_permissions=reasoned([{"name": "x.P", "protection_level": "signature"}]))
        self.assertEqual([], self.check(with_app("", '<permission android:name="x.P" android:protectionLevel="signature"/>'), base))
        self.assertEqual(1, len(self.check(with_app("", '<permission android:name="x.P" android:protectionLevel="normal"/>'), base)))
        self.assertEqual(2, len(self.check(with_app("", '<permission android:name="x.Q" android:protectionLevel="normal"/>'), base)))

    def test_allow_backup_turning_on_is_a_problem(self):
        problems = self.check(with_app("", application_attrs='android:allowBackup="true"'), baseline())
        self.assertEqual(1, len(problems), problems)
        self.assertIn("allowBackup", problems[0])

    def test_debuggable_or_cleartext_appearing_is_a_problem(self):
        attrs = 'android:allowBackup="false" android:debuggable="true" android:usesCleartextTraffic="true"'
        self.assertEqual(2, len(self.check(with_app("", application_attrs=attrs), baseline())))


class BaselineFileTest(unittest.TestCase):
    def test_a_baseline_entry_without_a_reason_is_a_problem(self):
        base = baseline(permissions=[{"name": "android.permission.INTERNET", "reason": ""}],
                        exported_components=[{"kind": "service", "name": "c.S", "permission": None, "enabled": "true"}],
                        features=[{"name": "f", "required": "true", "reason": "   "}],
                        declared_permissions=[{"name": "x.P", "protection_level": "signature"}])
        problems = guard.baseline_problems(base)
        self.assertEqual(4, len(problems), problems)
        self.assertTrue(any("x.P" in p for p in problems))
        self.assertTrue(any("android.permission.INTERNET" in p for p in problems))
        self.assertTrue(any("c.S" in p for p in problems))
        self.assertTrue(any("f" in p for p in problems))

    def test_a_baseline_with_reasons_everywhere_has_no_problem(self):
        self.assertEqual([], guard.baseline_problems(baseline(permissions=reasoned([{"name": "p"}]))))

    def test_the_shipped_baseline_has_a_reason_for_every_entry_and_is_within_300_lines(self):
        path = os.path.join(ROOT, "scripts", "merged_manifest_baseline.json")
        self.assertEqual([], guard.baseline_problems(guard.load_baseline(path)))
        with open(path, encoding="utf-8") as fh:
            self.assertLessEqual(len(fh.read().splitlines()), 300)

    def test_the_shipped_baseline_asks_for_no_camera_no_storage_and_no_location(self):
        names = {p["name"] for p in guard.load_baseline(os.path.join(ROOT, "scripts", "merged_manifest_baseline.json"))["permissions"]}
        self.assertEqual(set(), {n for n in names if any(w in n for w in ("CAMERA", "STORAGE", "MEDIA", "LOCATION", "CONTACTS"))})

    def test_the_shipped_baseline_keeps_backup_off(self):
        self.assertEqual("false", guard.load_baseline(os.path.join(ROOT, "scripts", "merged_manifest_baseline.json"))["application"]["allowBackup"])


class MainTest(unittest.TestCase):
    def run_main(self, xml, base):
        with tempfile.TemporaryDirectory() as tmp:
            manifest_path, base_path = os.path.join(tmp, "AndroidManifest.xml"), os.path.join(tmp, "baseline.json")
            with open(manifest_path, "w", encoding="utf-8") as fh:
                fh.write(xml)
            with open(base_path, "w", encoding="utf-8") as fh:
                json.dump(base, fh)
            out = io.StringIO()
            with redirect_stdout(out), self.assertRaises(SystemExit) as raised:
                guard.main(["--manifest", manifest_path, "--baseline", base_path])
            return raised.exception.code, out.getvalue()

    def test_a_matching_manifest_exits_zero_and_says_what_it_proved(self):
        xml = with_app("".join(f'<activity android:name="c.A{i}" android:exported="true"/>' for i in range(3)),
                       '<uses-permission android:name="p"/><uses-permission android:name="q"/><uses-feature android:name="f" android:required="false"/>'
                       '<permission android:name="x.P" android:protectionLevel="signature"/>')
        code, out = self.run_main(xml, baseline(
            permissions=reasoned([{"name": "p"}, {"name": "q"}]), features=reasoned([{"name": "f", "required": "false"}]),
            declared_permissions=reasoned([{"name": "x.P", "protection_level": "signature"}]),
            exported_components=reasoned([{"kind": "activity", "name": f"c.A{i}", "permission": None, "enabled": "true"} for i in range(3)])))
        self.assertEqual(0, code, out)
        self.assertIn("2 permissions, 1 declared, 1 features, 3 exported components", out)

    def test_a_new_permission_exits_non_zero_and_names_it(self):
        code, out = self.run_main(with_app("", '<uses-permission android:name="android.permission.CAMERA"/>'), baseline())
        self.assertNotEqual(0, code)
        self.assertIn("android.permission.CAMERA", out)

    def test_a_baseline_entry_without_a_reason_exits_non_zero_even_when_the_manifest_matches(self):
        code, out = self.run_main(with_app("", '<uses-permission android:name="p"/>'), baseline(permissions=[{"name": "p", "reason": ""}]))
        self.assertNotEqual(0, code)
        self.assertIn("reason", out)

    def test_a_missing_manifest_says_how_to_build_it(self):
        with tempfile.TemporaryDirectory() as tmp, redirect_stdout(io.StringIO()):
            with self.assertRaises(SystemExit) as raised:
                guard.main(["--manifest", os.path.join(tmp, "nope.xml")])
        self.assertIn("processReleaseManifest", str(raised.exception.code))


class WiringTest(unittest.TestCase):
    """The guard is only a guard if CI and the push gate run it after the release build."""

    def read(self, *parts):
        with open(os.path.join(ROOT, *parts), encoding="utf-8") as fh:
            return fh.read()

    def test_ci_runs_it_after_assemble_release_and_the_r8_gate(self):
        ci = self.read(".github", "workflows", "ci.yml")
        self.assertIn("scripts/check_merged_manifest.py", ci, "ci.yml must run the merged-manifest guard")
        self.assertLess(ci.index("assembleRelease"), ci.index("scripts/check_merged_manifest.py"))
        self.assertLess(ci.index("scripts/check_r8_guide.py"), ci.index("scripts/check_merged_manifest.py"))

    def test_the_push_gate_runs_it_after_the_release_build_and_the_r8_gate_in_one_chain(self):
        push = self.read("scripts", "git-pre-push.sh")
        chain = next(line for line in push.splitlines() if line.startswith("./gradlew") and "assembleRelease" in line)
        self.assertRegex(chain, r"assembleRelease.*&& python3 scripts/check_r8_guide\.py && python3 scripts/check_merged_manifest\.py")


if __name__ == "__main__":
    unittest.main()

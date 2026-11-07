#!/usr/bin/env python3
"""Release-manifest gate (Part II plan, Phase 6a): a library can add a permission, a feature requirement or an exported component
without one line of our code, so the merged release manifest is compared with a committed baseline.

After `:composeApp:assembleRelease`, this fails unless the merged manifest has exactly the baseline's
  1. permissions the app asks for (`uses-permission`, also the sdk-23 form) and permissions it declares (with their protection level),
  2. hardware/software features it requires (a `required=true` camera feature would hide the app from devices without one),
  3. exported components: activity, activity-alias, service, receiver, provider with `exported=true`, or a filter and no `exported`
     (providers only when they say so); each with the permission that guards it and whether it is enabled, and
  4. application flags `allowBackup`, `debuggable` and `usesCleartextTraffic`.
Every baseline entry carries a reason, so a change is a reviewed line in `scripts/merged_manifest_baseline.json`, never a silent one.

Usage: check_merged_manifest.py [--manifest PATH] [--baseline PATH]   (defaults: composeApp's release merge output and the committed baseline)
"""
import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
BASELINE = os.path.join(ROOT, "scripts", "merged_manifest_baseline.json")
BASELINE_NAME = "scripts/merged_manifest_baseline.json"
DEFAULT_MANIFEST = os.path.join(ROOT, "composeApp/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml")
BUILD_HINT = "Run ./gradlew :composeApp:assembleRelease -PallowPlaceholderGemmaModel=true (or :composeApp:processReleaseManifest) first"

ANDROID = "{http://schemas.android.com/apk/res/android}"
COMPONENT_KINDS = ("activity", "activity-alias", "service", "receiver", "provider")
APPLICATION_FLAGS = ("allowBackup", "debuggable", "usesCleartextTraffic")
FIX = f"Review it, then add or remove it in {BASELINE_NAME} with a reason"


def _attr(element, name):
    return element.get(ANDROID + name)


def _is_exported(element):
    explicit = _attr(element, "exported")
    if explicit is not None:
        # Anything but a plain "false" (a resource reference such as @bool/x included) counts as exported: the safe reading.
        return explicit != "false"
    # A provider with no word on it is private; any other component with an intent filter is public.
    return element.tag != "provider" and element.find("intent-filter") is not None


def parse_manifest(xml_text):
    """The parts of a merged manifest that widen what the app can do or who can reach it."""
    root = ET.fromstring(xml_text)
    parsed = {"permissions": {}, "declared_permissions": {}, "features": {}, "exported": {}, "application": {}}
    for tag in ("uses-permission", "uses-permission-sdk-23"):
        for element in root.findall(tag):
            parsed["permissions"][_attr(element, "name")] = True
    for element in root.findall("permission"):
        parsed["declared_permissions"][_attr(element, "name")] = _attr(element, "protectionLevel")
    for element in root.findall("uses-feature"):
        parsed["features"][_attr(element, "name")] = _attr(element, "required") or "true"
    application = root.find("application")
    if application is None:
        return parsed
    for flag in APPLICATION_FLAGS:
        parsed["application"][flag] = _attr(application, flag)
    for kind in COMPONENT_KINDS:
        for element in application.findall(kind):
            if _is_exported(element):
                parsed["exported"][(kind, _attr(element, "name"))] = {
                    "permission": _attr(element, "permission"),
                    "enabled": _attr(element, "enabled") or "true",
                }
    return parsed


def load_baseline(path):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def baseline_problems(baseline):
    """Every entry must say why it is there: the baseline is a record of reviewed decisions, not a snapshot."""
    problems = []
    for section, label in (("permissions", "permission"), ("declared_permissions", "declared permission"), ("features", "feature"),
                           ("exported_components", "exported component")):
        for entry in baseline.get(section, []):
            if not str(entry.get("reason", "")).strip():
                problems.append(f"{label} {entry.get('name')} has no reason in {BASELINE_NAME}")
    return problems


def _diff(label, actual, expected):
    """Entries only one side has; both are problems, with the same fix."""
    problems = [f"{label} {name} is new. {FIX}" for name in sorted(actual - expected)]
    problems += [f"{label} {name} is in the baseline but no longer in the manifest. {FIX}" for name in sorted(expected - actual)]
    return problems


def compare(actual, baseline):
    permissions = {entry["name"] for entry in baseline.get("permissions", [])}
    problems = _diff("Permission", set(actual["permissions"]), permissions)

    declared = {entry["name"]: entry["protection_level"] for entry in baseline.get("declared_permissions", [])}
    problems += _diff("Declared permission", set(actual["declared_permissions"]), set(declared))
    problems += [
        f"Declared permission {name} changed its protection level from {declared[name]} to {level}. {FIX}"
        for name, level in sorted(actual["declared_permissions"].items())
        if name in declared and declared[name] != level
    ]

    features = {entry["name"]: entry["required"] for entry in baseline.get("features", [])}
    problems += _diff("Feature", set(actual["features"]), set(features))
    problems += [
        f"Feature {name} changed from required={features[name]} to required={required}. {FIX}"
        for name, required in sorted(actual["features"].items())
        if name in features and features[name] != required
    ]

    components = {(entry["kind"], entry["name"]): entry for entry in baseline.get("exported_components", [])}
    problems += [f"Exported {kind} {name} is new. {FIX}" for kind, name in sorted(set(actual["exported"]) - set(components))]
    problems += [f"Exported {kind} {name} is in the baseline but no longer exported. {FIX}" for kind, name in sorted(set(components) - set(actual["exported"]))]
    for key in sorted(set(actual["exported"]) & set(components)):
        for field in ("permission", "enabled"):
            if actual["exported"][key][field] != components[key].get(field):
                problems.append(f"Exported {key[0]} {key[1]}: {field} is {actual['exported'][key][field]}, the baseline says {components[key].get(field)}. {FIX}")

    expected_flags = baseline.get("application", {})
    problems += [
        f"Application flag {flag} is {actual['application'].get(flag)}, the baseline says {expected_flags.get(flag)}. {FIX}"
        for flag in APPLICATION_FLAGS
        if actual["application"].get(flag) != expected_flags.get(flag)
    ]
    return problems


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--manifest", default=DEFAULT_MANIFEST)
    parser.add_argument("--baseline", default=BASELINE)
    args = parser.parse_args(argv)
    if not os.path.exists(args.manifest):
        sys.exit(f"{BUILD_HINT} (manifest: {args.manifest})")
    baseline = load_baseline(args.baseline)
    with open(args.manifest, encoding="utf-8") as fh:
        actual = parse_manifest(fh.read())
    problems = baseline_problems(baseline) + compare(actual, baseline)
    for problem in problems:
        print(f"❌ {problem}")
    if problems:
        sys.exit(1)
    print(
        f"✅ Release manifest gate: {len(actual['permissions'])} permissions, {len(actual['declared_permissions'])} declared, "
        f"{len(actual['features'])} features, {len(actual['exported'])} exported components, all as in {BASELINE_NAME}"
    )
    sys.exit(0)


if __name__ == "__main__":
    main()

import importlib.util
import os
import tempfile
import unittest
import zipfile

_SCRIPT = os.path.join(os.path.dirname(__file__), "..", "check_r8_guide.py")
_spec = importlib.util.spec_from_file_location("check_r8_guide", _SCRIPT)
gate = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(gate)

PKG = "com.payslipmax.pdfparser.guide.model"
MODELS_SRC = """
@Serializable
data class GuideBundle(val version: Int)

/** doc */
@Serializable
data class GuideCard(val id: String)

enum class GuideChip { RATES }
"""


def _mapping(*classes, drop=()):
    """A minimal R8 mapping where each model keeps its serializer and Companion.serializer()."""
    lines = ["# compiler: R8", "com.example.Other -> a.a:"]
    for name in classes:
        for cls, members in (
            (f"{PKG}.{name}", ["    int version -> a"]),
            (f"{PKG}.{name}$$serializer", ["    1:1:void <init>():0:0 -> <init>"]),
            (f"{PKG}.{name}$Companion", ["    1:1:kotlinx.serialization.KSerializer serializer():12:12 -> serializer"]),
        ):
            if cls in drop:
                continue
            lines.append(f"{cls} -> b.{len(lines)}:")
            lines += [m for m in members if f"{cls}#serializer" not in drop]
    return "\n".join(lines) + "\n"


class ModelDiscoveryTest(unittest.TestCase):
    def test_finds_every_serializable_class_and_nothing_else(self):
        self.assertEqual(gate.serializable_models(MODELS_SRC), ["GuideBundle", "GuideCard"])


class MappingCheckTest(unittest.TestCase):
    """A model whose serializer R8 removed or renamed away fails at runtime only in release, with a blank Guide."""

    def test_clean_mapping_passes(self):
        self.assertEqual(gate.check_mapping(_mapping("GuideBundle", "GuideCard"), ["GuideBundle", "GuideCard"]), [])

    def test_missing_model_class_fails(self):
        problems = gate.check_mapping(_mapping("GuideBundle"), ["GuideBundle", "GuideCard"])
        self.assertTrue(any("GuideCard" in p for p in problems), problems)

    def test_missing_generated_serializer_fails(self):
        mapping = _mapping("GuideBundle", drop=(f"{PKG}.GuideBundle$$serializer",))
        self.assertTrue(any("$$serializer" in p for p in gate.check_mapping(mapping, ["GuideBundle"])))

    def test_companion_without_serializer_method_fails(self):
        mapping = _mapping("GuideBundle", drop=(f"{PKG}.GuideBundle$Companion#serializer",))
        self.assertTrue(any("serializer()" in p for p in gate.check_mapping(mapping, ["GuideBundle"])))


class ApkCheckTest(unittest.TestCase):
    """The bundle must ship byte-identical: a shrunk, re-encoded or missing file is an empty or broken Guide."""

    def _apk(self, tmp, entries):
        path = os.path.join(tmp, "app.apk")
        with zipfile.ZipFile(path, "w") as z:
            for name, data in entries.items():
                z.writestr(name, data)
        return path

    def test_identical_bundle_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = self._apk(tmp, {"assets/composeResources/x.resources/files/guide/guide_bundle.json": b"{}"})
            self.assertEqual(gate.check_apk(apk, b"{}"), [])

    def test_missing_bundle_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = self._apk(tmp, {"classes.dex": b"dex"})
            self.assertTrue(any("not in" in p for p in gate.check_apk(apk, b"{}")))

    def test_changed_bundle_fails(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = self._apk(tmp, {"assets/composeResources/x.resources/files/guide/guide_bundle.json": b"{ }"})
            self.assertTrue(any("differs" in p for p in gate.check_apk(apk, b"{}")))


if __name__ == "__main__":
    unittest.main()

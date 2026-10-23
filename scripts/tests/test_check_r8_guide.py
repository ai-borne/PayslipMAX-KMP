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


def _mapping(*classes, drop=(), pkg=PKG):
    """A minimal R8 mapping where each model keeps its serializer and Companion.serializer()."""
    lines = ["# compiler: R8", "com.example.Other -> a.a:"]
    for name in classes:
        for cls, members in (
            (f"{pkg}.{name}", ["    int version -> a"]),
            (f"{pkg}.{name}$$serializer", ["    1:1:void <init>():0:0 -> <init>"]),
            (f"{pkg}.{name}$Companion", ["    1:1:kotlinx.serialization.KSerializer serializer():12:12 -> serializer"]),
        ):
            if cls in drop:
                continue
            lines.append(f"{cls} -> b.{len(lines)}:")
            lines += [m for m in members if f"{cls}#serializer" not in drop]
    return "\n".join(lines) + "\n"


class ModelDiscoveryTest(unittest.TestCase):
    def test_finds_every_serializable_class_and_nothing_else(self):
        self.assertEqual(gate.serializable_models(MODELS_SRC), ["GuideBundle", "GuideCard"])


class ModelFolderTest(unittest.TestCase):
    def test_models_in_every_file_of_the_folder_are_found_not_only_the_first(self):
        with tempfile.TemporaryDirectory() as folder:
            for name, source in (("GuideModels.kt", MODELS_SRC), ("GuidePreClaimModels.kt", "@Serializable\ndata class GuideOption(val id: String)\n"),
                                 ("notes.txt", "@Serializable\nclass NotKotlin")):
                with open(os.path.join(folder, name), "w", encoding="utf-8") as fh:
                    fh.write(source)
            self.assertEqual(gate.models_in(folder), ["GuideBundle", "GuideCard", "GuideOption"])

    def test_the_real_folder_holds_the_pre_claim_models(self):
        self.assertIn("GuidePreClaim", gate.models_in(gate.MODELS_DIR))
        self.assertGreaterEqual(len(gate.models_in(gate.MODELS_DIR)), 20)


PART2_PKG = "com.payslipmax.pdfparser.part2.domain"


class ModelFoldersTest(unittest.TestCase):
    """Part II models live outside guide/model (slips, payloads), so the gate must walk every registered folder, with each file's own package."""

    def _tree(self, tmp):
        for rel, source in (
            ("guide/model/GuideModels.kt", MODELS_SRC),
            ("part2/Part2Payload.kt", "@Serializable\ndata class Part2Payload(val v: Int)\n"),
            ("part2/domain/Part2Slip.kt", "@Serializable\ndata class Part2Slip(val orders: List<String>)\n"),
            ("other/Ignored.kt", "@Serializable\ndata class NotRegistered(val id: String)\n"),
        ):
            path = os.path.join(tmp, *rel.split("/"))
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "w", encoding="utf-8") as fh:
                fh.write(source)

    def test_every_registered_folder_is_walked_with_its_own_package(self):
        with tempfile.TemporaryDirectory() as tmp:
            self._tree(tmp)
            found = gate.qualified_models(tmp, ("guide/model", "part2"))
        self.assertEqual(found, [f"{gate.ROOT_PACKAGE}.guide.model.GuideBundle", f"{gate.ROOT_PACKAGE}.guide.model.GuideCard",
                                 f"{gate.ROOT_PACKAGE}.part2.Part2Payload", f"{gate.ROOT_PACKAGE}.part2.domain.Part2Slip"])

    def test_a_folder_that_does_not_exist_yet_is_reported_rather_than_skipped_in_silence(self):
        with tempfile.TemporaryDirectory() as tmp:
            self._tree(tmp)
            self.assertEqual(gate.absent_folders(tmp, ("guide/model", "part2", "part2/data")), ["part2/data"])

    def test_the_part_ii_folder_is_registered_next_to_the_guide_models(self):
        self.assertEqual(gate.MODEL_FOLDERS, ("guide/model", "part2"))

    def test_the_real_tree_yields_the_guide_models_including_the_part_ii_lookup(self):
        found = gate.qualified_models(gate.KOTLIN_ROOT, gate.MODEL_FOLDERS)
        self.assertIn(f"{gate.ROOT_PACKAGE}.guide.model.Part2Lookup", found)
        self.assertIn(f"{gate.ROOT_PACKAGE}.guide.model.GuideBundle", found)
        self.assertEqual(len(found), len(set(found)), "a model listed twice would hide a missing one")

    def test_a_part_ii_model_that_r8_removed_fails_the_mapping_check(self):
        models = [f"{PART2_PKG}.Part2Slip"]
        self.assertEqual(gate.check_mapping(_mapping("Part2Slip", pkg=PART2_PKG), models), [])
        problems = gate.check_mapping(_mapping("Other", pkg=PART2_PKG), models)
        self.assertTrue(any("Part2Slip" in p and "removed" in p for p in problems), problems)

    def test_a_part_ii_model_without_its_serializer_fails_the_mapping_check(self):
        models = [f"{PART2_PKG}.Part2Slip"]
        mapping = _mapping("Part2Slip", pkg=PART2_PKG, drop=(f"{PART2_PKG}.Part2Slip$$serializer",))
        self.assertTrue(any("$$serializer" in p for p in gate.check_mapping(mapping, models)))


class DefaultsTest(unittest.TestCase):
    """The gate must inspect the build that ships; a minifiedTest-only check can pass while release breaks."""

    def test_defaults_point_at_the_release_outputs(self):
        self.assertTrue(gate.DEFAULT_MAPPING.endswith("outputs/mapping/release/mapping.txt"), gate.DEFAULT_MAPPING)
        self.assertTrue(gate.DEFAULT_APK_GLOB.endswith("outputs/apk/release/*.apk"), gate.DEFAULT_APK_GLOB)

    def test_failure_hint_names_assemble_release(self):
        self.assertIn("assembleRelease", gate.BUILD_HINT)


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

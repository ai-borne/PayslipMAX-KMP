import importlib.util
import os
import stat
import subprocess
import tempfile
import unittest

_SCRIPTS = os.path.join(os.path.dirname(__file__), "..")
_spec = importlib.util.spec_from_file_location("run_flag_on_tests", os.path.join(_SCRIPTS, "run_flag_on_tests.py"))
tool = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(tool)

ROOT = os.path.abspath(os.path.join(_SCRIPTS, ".."))
FLAG_LINE_OFF = "    const val PART2_ENABLED: Boolean = false\n"
EXPECT_LINE_OFF = "    const val PART2_ENABLED: Boolean = false\n"


def write(root, rel, text):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)
    return path


def read(root, rel):
    with open(os.path.join(root, rel), encoding="utf-8") as fh:
        return fh.read()


def fake_tree(root, flag=FLAG_LINE_OFF, expectation=EXPECT_LINE_OFF):
    write(root, tool.LAUNCH_FLAGS, "object LaunchFlags {\n    const val GUIDE_ENABLED: Boolean = true\n" + flag + "}\n")
    write(root, tool.EXPECTATION, "object Part2FlagExpectation {\n" + expectation + "}\n")


class FlipTest(unittest.TestCase):
    """The copy must run with the flag ON and its committed expectation ON, or the flag-on run would fail on its own pin."""

    def test_both_files_are_switched_on_and_nothing_else_changes(self):
        with tempfile.TemporaryDirectory() as root:
            fake_tree(root)
            tool.flip(root)
            self.assertEqual(read(root, tool.LAUNCH_FLAGS), "object LaunchFlags {\n    const val GUIDE_ENABLED: Boolean = true\n    const val PART2_ENABLED: Boolean = true\n}\n")
            self.assertEqual(read(root, tool.EXPECTATION), "object Part2FlagExpectation {\n    const val PART2_ENABLED: Boolean = true\n}\n")

    def test_a_file_with_no_flag_line_is_a_loud_failure_not_a_silent_flag_off_run(self):
        with tempfile.TemporaryDirectory() as root:
            fake_tree(root, flag="    const val PART2_ENABLED_RENAMED: Boolean = false\n")
            with self.assertRaises(tool.FlipError):
                tool.flip(root)

    def test_a_file_with_the_flag_line_twice_is_a_loud_failure(self):
        with tempfile.TemporaryDirectory() as root:
            fake_tree(root, expectation=EXPECT_LINE_OFF * 2)
            with self.assertRaises(tool.FlipError):
                tool.flip(root)

    def test_a_flag_that_is_already_on_is_a_loud_failure_so_nothing_is_flipped_twice(self):
        with tempfile.TemporaryDirectory() as root:
            fake_tree(root, flag=FLAG_LINE_OFF.replace("false", "true"), expectation=EXPECT_LINE_OFF.replace("false", "true"))
            with self.assertRaises(tool.FlipError):
                tool.flip(root)

    def test_it_refuses_to_touch_a_git_checkout(self):
        with tempfile.TemporaryDirectory() as root:
            fake_tree(root)
            os.makedirs(os.path.join(root, ".git"))
            with self.assertRaises(tool.FlipError):
                tool.flip(root)
            self.assertIn("false", read(root, tool.LAUNCH_FLAGS), "the refused checkout is left as it was")


class RepoContractTest(unittest.TestCase):
    """The script must keep working on the real files: a rename would otherwise turn the CI job into a flag-off run that passes."""

    def test_the_real_files_each_hold_exactly_one_flag_line(self):
        for rel in (tool.LAUNCH_FLAGS, tool.EXPECTATION):
            text = read(ROOT, rel)
            self.assertEqual(text.count("const val PART2_ENABLED: Boolean = "), 1, rel)

    def test_the_committed_expectation_equals_the_flag(self):
        flag = read(ROOT, tool.LAUNCH_FLAGS).split("const val PART2_ENABLED: Boolean = ")[1].split()[0]
        expectation = read(ROOT, tool.EXPECTATION).split("const val PART2_ENABLED: Boolean = ")[1].split()[0]
        self.assertEqual(flag, expectation, "the flip commit edits both files together")

    def test_the_flag_on_run_covers_both_modules_debug_unit_tests(self):
        self.assertEqual(tool.GRADLE_TASKS, [":shared:testDebugUnitTest", ":composeApp:testDebugUnitTest"])


class CopyTest(unittest.TestCase):
    """What is copied is what CI would check out: tracked and unignored files, never a keystore or the model."""

    def git(self, root, *args):
        subprocess.run(["git", *args], cwd=root, check=True, capture_output=True)

    def test_tracked_and_unignored_files_are_copied_and_ignored_ones_are_not(self):
        with tempfile.TemporaryDirectory() as src, tempfile.TemporaryDirectory() as dst:
            self.git(src, "init", "-q")
            write(src, ".gitignore", "*.keystore\nbuild/\nlocal.properties\n")
            write(src, "a/tracked.kt", "tracked")
            write(src, "b/new.kt", "untracked but not ignored")
            write(src, "release.keystore", "secret")
            write(src, "build/out.bin", "output")
            write(src, "local.properties", "sdk.dir=/somewhere")
            self.git(src, "add", ".gitignore", "a/tracked.kt")
            tool.copy_tracked(src, dst)
            self.assertEqual(read(dst, "a/tracked.kt"), "tracked")
            self.assertEqual(read(dst, "b/new.kt"), "untracked but not ignored")
            self.assertFalse(os.path.exists(os.path.join(dst, "release.keystore")), "a keystore never enters the copy")
            self.assertFalse(os.path.exists(os.path.join(dst, "build", "out.bin")))
            self.assertEqual(read(dst, "local.properties"), "sdk.dir=/somewhere", "the SDK path is the one ignored file Gradle needs")
            self.assertFalse(os.path.exists(os.path.join(dst, ".git")))

    def test_the_executable_bit_of_the_gradle_wrapper_survives(self):
        with tempfile.TemporaryDirectory() as src, tempfile.TemporaryDirectory() as dst:
            self.git(src, "init", "-q")
            path = write(src, "gradlew", "#!/bin/sh\n")
            os.chmod(path, 0o755)
            self.git(src, "add", "gradlew")
            tool.copy_tracked(src, dst)
            self.assertTrue(os.stat(os.path.join(dst, "gradlew")).st_mode & stat.S_IXUSR)


if __name__ == "__main__":
    unittest.main()

import importlib.util
import os
import unittest

_SCRIPT = os.path.join(os.path.dirname(__file__), "..", "check_r8_guide.py")
_spec = importlib.util.spec_from_file_location("check_r8_guide", _SCRIPT)
gate = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(gate)

WORKER = f"{gate.ROOT_PACKAGE}.reminders.ReminderWorker"


class ReminderWorkerTest(unittest.TestCase):
    """Phase 7b: WorkManager stores the worker's class NAME in its database and starts it by that name, possibly after an app update. R8 may
    neither remove the class nor rename it, or every reminder scheduled before the update would fail to start."""

    def _mapping(self, obfuscated):
        return f"# compiler: R8\n{WORKER} -> {obfuscated}:\n    int x -> a\n"

    def test_a_worker_that_keeps_its_name_passes(self):
        self.assertEqual(gate.check_unrenamed_classes(self._mapping(WORKER)), [])

    def test_a_renamed_worker_fails_and_names_both(self):
        problems = gate.check_unrenamed_classes(self._mapping("a.b"))
        self.assertEqual(len(problems), 1, problems)
        self.assertIn("ReminderWorker", problems[0])
        self.assertIn("renamed", problems[0])

    def test_a_removed_worker_is_not_reported_twice(self):
        self.assertEqual(gate.check_unrenamed_classes("# compiler: R8\ncom.example.Other -> a.a:\n"), [])
        self.assertEqual(len(gate.check_required_classes("# compiler: R8\ncom.example.Other -> a.a:\n")), 3)

    def test_the_gate_checks_the_names_it_requires(self):
        self.assertEqual(gate.UNRENAMED_CLASSES, (WORKER,))
        self.assertIn(WORKER, gate.REQUIRED_CLASSES)

    def test_the_real_sources_declare_the_worker_the_gate_requires(self):
        path = os.path.join(gate.ROOT, "composeApp/src/androidMain/kotlin/com/payslipmax/pdfparser/reminders/ReminderWorker.kt")
        with open(path, encoding="utf-8") as fh:
            self.assertIn("class ReminderWorker", fh.read())


class FlagAwareTest(unittest.TestCase):
    """With PART2_ENABLED false nothing reaches the worker, so R8 rightly drops it with the rest of Part II (seen on the first run of this gate,
    2026-10-26). The Room classes are always required; the worker only where the flag is on, and then it is proved under its own name."""

    ROOM = (f"{gate.ROOT_PACKAGE}.database.PayslipDatabase_Impl", f"{gate.ROOT_PACKAGE}.part2.data.Part2Dao_Impl")
    ROOM_ONLY = "# compiler: R8\n" + "".join(f"{name} -> a.b{i}:\n    int x -> a\n" for i, name in enumerate(ROOM))

    def test_with_the_flag_off_a_missing_worker_is_expected(self):
        self.assertEqual(gate.check_required_classes(self.ROOM_ONLY, part2_enabled=False), [])
        self.assertEqual(gate.check_unrenamed_classes(self.ROOM_ONLY, part2_enabled=False), [])

    def test_with_the_flag_off_even_a_renamed_worker_is_not_checked(self):
        renamed = f"# compiler: R8\n{WORKER} -> a.b:\n    int x -> a\n"
        self.assertEqual(gate.check_unrenamed_classes(renamed, part2_enabled=False), [])
        self.assertEqual(len(gate.check_unrenamed_classes(renamed, part2_enabled=True)), 1)

    def test_with_the_flag_off_the_room_classes_are_still_required(self):
        problems = gate.check_required_classes("# compiler: R8\n", part2_enabled=False)
        self.assertEqual(len(problems), 2, problems)
        self.assertFalse(any("ReminderWorker" in p for p in problems))

    def test_with_the_flag_on_a_missing_worker_fails(self):
        problems = gate.check_required_classes(self.ROOM_ONLY, part2_enabled=True)
        self.assertEqual(len(problems), 1, problems)
        self.assertIn("ReminderWorker", problems[0])

    def test_the_flag_is_read_from_the_launch_flags_source(self):
        self.assertTrue(gate.part2_enabled("const val PART2_ENABLED: Boolean = true"))
        self.assertFalse(gate.part2_enabled("    const val PART2_ENABLED: Boolean = false // dark launch"))

    def test_an_unreadable_flag_fails_loudly_instead_of_skipping_the_worker(self):
        with self.assertRaises(ValueError):
            gate.part2_enabled("const val SOMETHING_ELSE: Boolean = true")

    def test_the_real_launch_flags_file_holds_the_flag_in_the_shape_the_gate_reads(self):
        with open(os.path.join(gate.KOTLIN_ROOT, "subscription/LaunchFlags.kt"), encoding="utf-8") as fh:
            self.assertIsInstance(gate.part2_enabled(fh.read()), bool)


if __name__ == "__main__":
    unittest.main()

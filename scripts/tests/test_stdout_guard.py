import importlib.util
import os
import tempfile
import textwrap
import unittest

_SCRIPTS = os.path.join(os.path.dirname(__file__), "..")
_spec = importlib.util.spec_from_file_location("stdout_guard", os.path.join(_SCRIPTS, "stdout_guard.py"))
guard = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(guard)


def _check(kotlin_source, relpath="shared/src/androidMain/kotlin/p/Sample.kt"):
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, relpath)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(textwrap.dedent(kotlin_source))
        return guard.check_no_stdout(path)


class StdoutGuardTest(unittest.TestCase):
    """Standard output reaches logcat in release, so production code may only print through Logger.

    WHY: AndroidTokenExtractor printed every payslip token (names, account numbers, amounts) with println on each import.
    """

    def test_println_of_a_value_is_flagged_in_every_production_source_set(self):
        for source_set in ("androidMain", "commonMain", "iosMain"):
            with self.subTest(source_set=source_set):
                errors = _check('fun f(t: String) { println("text=$t") }', f"shared/src/{source_set}/kotlin/p/Sample.kt")
                self.assertEqual(1, len(errors), errors)

    def test_system_out_and_err_are_flagged(self):
        self.assertEqual(1, len(_check("fun f() { System.out.println(1) }")))
        self.assertEqual(1, len(_check("fun f() { System.err.print(1) }")))

    def test_composeapp_production_code_is_covered(self):
        self.assertEqual(1, len(_check("fun f() { println(1) }", "composeApp/src/commonMain/kotlin/p/Sample.kt")))

    def test_logger_is_the_one_place_allowed_to_print(self):
        self.assertEqual([], _check('fun f() { println("x") }', "shared/src/commonMain/kotlin/p/logging/Logger.kt"))

    def test_test_source_sets_are_not_checked(self):
        for source_set in ("commonTest", "androidUnitTest", "iosTest"):
            with self.subTest(source_set=source_set):
                self.assertEqual([], _check("fun f() { println(1) }", f"shared/src/{source_set}/kotlin/p/SampleTest.kt"))

    def test_comments_strings_and_unrelated_print_functions_are_not_flagged(self):
        source = '''
            // println(token) was removed
            /** never call println here */
            val s = "println(x)"
            interface Printer { suspend fun print(job: String) }
            fun g(printer: Printer) = printer.print("x")
        '''
        self.assertEqual([], _check(source))

    def test_a_raw_string_that_mentions_println_is_not_flagged(self):
        source = 'val doc = """\n  println("x")\n"""\n'
        self.assertEqual([], _check(source))


if __name__ == "__main__":
    unittest.main()

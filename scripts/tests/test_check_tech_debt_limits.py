import importlib.util
import os
import tempfile
import textwrap
import unittest

_SCRIPT = os.path.join(os.path.dirname(__file__), "..", "check_tech_debt_limits.py")
_spec = importlib.util.spec_from_file_location("check_tech_debt_limits", _SCRIPT)
audit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(audit)


def _violations(kotlin_source):
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, "Sample.kt")
        with open(path, "w", encoding="utf-8") as f:
            f.write(textwrap.dedent(kotlin_source))
        return audit.check_file_limits(path)


def _composable(signature_params, body_lines):
    body = "\n".join(f"    val v{i} = {i}" for i in range(body_lines))
    return f"@Composable\nfun Sample(\n{signature_params}) {{\n{body}\n}}\n"


class ComposableLengthTest(unittest.TestCase):
    """A composable's length is its whole body, whatever its parameter defaults look like.

    The audit once ended a composable at the first line whose braces balanced, so a default such as
    `= remember { X() }` or `= {}` made a 52-line composable measure ~8 lines and pass the 50-line limit.
    """

    def test_long_composable_with_plain_defaults_is_flagged(self):
        errors = _violations(_composable("    a: Int = 1,\n", 60))
        self.assertTrue(any("exceeds 50 lines" in e for e in errors), errors)

    def test_long_composable_with_a_braced_default_is_still_flagged(self):
        for default in ("= remember { X() }", "= {}"):
            with self.subTest(default=default):
                errors = _violations(_composable(f"    a: Foo {default},\n", 60))
                self.assertTrue(any("exceeds 50 lines" in e for e in errors), errors)

    def test_short_composable_with_a_braced_default_passes(self):
        self.assertEqual([], _violations(_composable("    a: Foo = remember { X() },\n", 5)))

    def test_expression_bodied_composable_is_not_measured_to_end_of_file(self):
        source = "@Composable\nfun Sample() = Other()\n" + "\n".join(f"val x{i} = {i}" for i in range(80)) + "\n"
        self.assertEqual([], _violations(source))


if __name__ == "__main__":
    unittest.main()

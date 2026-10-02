import importlib.util
import os
import tempfile
import textwrap
import unittest

_SCRIPTS = os.path.join(os.path.dirname(__file__), "..")


def _load(name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(_SCRIPTS, f"{name}.py"))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


guard = _load("stateful_singleton_guard")
audit = _load("check_tech_debt_limits")


def _check(kotlin_source, allowlist=frozenset(), name="Sample.kt"):
    with tempfile.TemporaryDirectory() as tmp:
        path = os.path.join(tmp, name)
        with open(path, "w", encoding="utf-8") as f:
            f.write(textwrap.dedent(kotlin_source))
        return guard.check_stateful_singletons(path, allowlist=allowlist)


class StatefulSingletonTest(unittest.TestCase):
    """A process-wide `object` may not hold mutable state or a mutable collection.

    Hidden global state makes tests order-dependent and blocks substitution, which is why the developer
    tools registry was replaced by Koin. Constants and pure functions in an `object` stay allowed.
    """

    def test_a_var_in_an_object_is_flagged(self):
        errors = _check("object X {\n    var y = 0\n}\n")
        self.assertEqual(1, len(errors), errors)
        self.assertIn("X", errors[0])

    def test_lateinit_and_volatile_vars_are_flagged(self):
        for decl in ("lateinit var y: String", "@Volatile private var y: Int = 0", "internal var y: Int? = null"):
            with self.subTest(decl=decl):
                self.assertEqual(1, len(_check(f"object X {{\n    {decl}\n}}\n")))

    def test_a_mutable_collection_or_holder_in_an_object_is_flagged(self):
        for init in (
            "mutableMapOf<String, Int>()",
            "linkedMapOf<String, Int>()",
            "mutableListOf<Int>()",
            "HashMap<String, Int>()",
            "MutableStateFlow(0)",
            "AtomicInteger(0)",
            "ConcurrentHashMap<String, Int>()",
        ):
            with self.subTest(init=init):
                self.assertEqual(1, len(_check(f"object X {{\n    private val y = {init}\n}}\n")), init)

    def test_a_companion_object_is_checked_and_named_after_its_class(self):
        errors = _check("class Engine {\n    companion object {\n        var cache: Int = 0\n    }\n}\n")
        self.assertEqual(1, len(errors), errors)
        self.assertIn("Engine.Companion", errors[0])

    def test_constants_and_pure_functions_pass(self):
        source = """
            object PayMatrix {
                const val LIMIT = 3
                val names = listOf("a", "b")
                private val rates = mapOf("x" to 1)
                fun total(values: List<Int>): Int {
                    var sum = 0
                    for (v in values) sum += v
                    val seen = mutableSetOf<Int>()
                    return sum + seen.size
                }
            }
        """
        self.assertEqual([], _check(source))

    def test_a_var_inside_a_nested_class_is_not_the_objects_state(self):
        source = """
            object Holder {
                class Counter {
                    var n = 0
                }
            }
        """
        self.assertEqual([], _check(source))

    def test_an_ordinary_class_with_state_is_not_a_singleton(self):
        self.assertEqual([], _check("class Counter {\n    var n = 0\n}\n"))

    def test_an_anonymous_object_expression_is_not_a_singleton(self):
        source = """
            fun make() = object : Runnable {
                var ran = false
                override fun run() { ran = true }
            }
        """
        self.assertEqual([], _check(source))

    def test_braces_in_strings_and_comments_do_not_confuse_the_scan(self):
        source = '''
            object Quiet {
                // { an unbalanced brace in a comment
                val text = "}} and ${'$'}{1 + 1} {"
                fun f() {}
            }
            object Loud {
                var y = 0
            }
        '''
        errors = _check(source)
        self.assertEqual(1, len(errors), errors)
        self.assertIn("Loud", errors[0])

    def test_an_allowlisted_singleton_is_not_flagged_but_others_in_the_file_are(self):
        source = "object Bridge {\n    var cb: Int = 0\n}\nobject Other {\n    var n = 0\n}\n"
        errors = _check(source, allowlist={"Sample.kt::Bridge"})
        self.assertEqual(1, len(errors), errors)
        self.assertIn("Other", errors[0])

    def test_the_allowlist_matches_on_the_path_suffix_so_it_works_from_any_checkout(self):
        self.assertTrue(guard.is_allowlisted("/ci/work/shared/src/x/Logger.kt", "Logger", {"shared/src/x/Logger.kt::Logger"}))
        self.assertFalse(guard.is_allowlisted("/ci/work/shared/src/y/Logger.kt", "Logger", {"shared/src/x/Logger.kt::Logger"}))


class RepositoryStateTest(unittest.TestCase):
    """The real source tree is clean against the guard, with the existing singletons explicitly allowlisted."""

    def test_the_main_source_sets_have_no_unlisted_stateful_singleton(self):
        root = os.path.abspath(os.path.join(_SCRIPTS, ".."))
        errors = guard.scan_repository(root)
        self.assertEqual([], errors)

    def test_every_allowlist_entry_still_matches_a_real_stateful_singleton(self):
        """A stale entry would silently excuse a future one that reuses the name, so it must fail."""
        root = os.path.abspath(os.path.join(_SCRIPTS, ".."))
        self.assertEqual([], guard.stale_allowlist_entries(root))

    def test_the_tech_debt_audit_runs_the_guard_on_a_file_it_is_given(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "Sample.kt")
            with open(path, "w", encoding="utf-8") as f:
                f.write("object X {\n    var y = 0\n}\n")
            errors = audit.audit_file(path, tmp)
        self.assertTrue(any("stateful singleton" in e.lower() for e in errors), errors)


if __name__ == "__main__":
    unittest.main()

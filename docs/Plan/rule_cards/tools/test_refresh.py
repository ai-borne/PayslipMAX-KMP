"""Tests for refresh.py, the one-command rebuild.

Why they matter: a content fix used to need five scripts in the right order; a skipped or mis-ordered step ships a
stale bundle or an unchecked copy. These tests pin the order, the stop-at-first-failure rule and the loud copycheck
skip. Run: python3 tools/test_refresh.py
"""
import unittest

import refresh


def names(steps):
    return [n for n, _ in steps]


class StepPlan(unittest.TestCase):
    def test_write_mode_runs_the_documented_order(self):
        self.assertEqual(names(refresh.plan(check=False, have_sources=True)),
                         ['compile --check', 'compile', 'ids --update', 'render_md', 'bundle', 'review_queue', 'tool tests', 'copycheck'])

    def test_check_mode_never_writes_files(self):
        steps = names(refresh.plan(check=True, have_sources=True))
        self.assertEqual(steps, ['compile --check --fresh', 'ids --check', 'bundle --check', 'tool tests', 'copycheck'])

    def test_copycheck_is_dropped_when_sources_are_missing(self):
        self.assertNotIn('copycheck', names(refresh.plan(check=True, have_sources=False)))


class Running(unittest.TestCase):
    def test_stops_at_the_first_failure(self):
        ran = []
        code = refresh.run([('a', ['a']), ('b', ['b']), ('c', ['c'])], lambda argv: (ran.append(argv[0]), 1 if argv[0] == 'b' else 0)[1])
        self.assertEqual((code, ran), (1, ['a', 'b']))

    def test_missing_sources_fail_loudly_unless_allowed(self):
        out = []
        self.assertNotEqual(refresh.main(['--check'], have_sources=False, runner=lambda a: 0, say=out.append), 0)
        self.assertTrue(any('SKIPPED copycheck (sources missing)' in line for line in out))

    def test_allow_no_sources_passes_but_still_says_skipped(self):
        out = []
        self.assertEqual(refresh.main(['--check', '--allow-no-sources'], have_sources=False, runner=lambda a: 0, say=out.append), 0)
        self.assertTrue(any('SKIPPED copycheck (sources missing)' in line for line in out))

    def test_sources_present_means_no_skip_message(self):
        out = []
        self.assertEqual(refresh.main(['--check'], have_sources=True, runner=lambda a: 0, say=out.append), 0)
        self.assertFalse(any('SKIPPED' in line for line in out))


if __name__ == '__main__':
    unittest.main()

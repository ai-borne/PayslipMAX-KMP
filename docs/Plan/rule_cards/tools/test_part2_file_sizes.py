"""Part II Orders rule: no Part II file passes 300 lines. The Kotlin gates (Gradle checkFileSizes, check_tech_debt_limits.py) do not
see Python, JSON, Markdown or Swift, so this test does. It looks at every file whose name starts with part2 or test_part2 in the
rule-card tools folder, at everything in docs/Plan/rule_cards/part2/ (templates too, as they arrive), and at the Part II Swift files
under iosApp. A generated data file may be longer only by being listed in ONE_ROW_PER_LINE, with the reason.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_file_sizes.py'
"""
import os
import tempfile
import unittest

from config import CARDS_DIR, REPO_ROOT

MAX_LINES = 300
SUFFIXES = ('.py', '.json', '.md', '.swift')
# One record per line on purpose, so a review diff reads row by row. Still checked at three times the limit.
ONE_ROW_PER_LINE = {'codes.json'}
REVIEW_PACK = 'part2_review.md'  # generated side-by-side pack for the owner; not source


def files():
    tools = os.path.dirname(os.path.abspath(__file__))
    for name in sorted(os.listdir(tools)):
        if name.startswith(('part2', 'test_part2')) and name.endswith('.py'):
            yield os.path.join(tools, name)
    for root in (os.path.join(CARDS_DIR, 'part2'), os.path.join(REPO_ROOT, 'iosApp')):
        for folder, dirs, names in os.walk(root):
            dirs[:] = [d for d in dirs if d not in ('build', 'Pods', '.build', 'DerivedData')]
            for name in sorted(names):
                part2_swift = name.endswith('.swift') and 'Part2' in name
                if (root.endswith('part2') and name.endswith(SUFFIXES) and name != REVIEW_PACK) or part2_swift:
                    yield os.path.join(folder, name)


def too_long(paths):
    """(path, lines, limit) for every file over its limit."""
    found = []
    for path in paths:
        with open(path, encoding='utf-8') as fh:
            lines = sum(1 for _ in fh)
        limit = MAX_LINES * 3 if os.path.basename(path) in ONE_ROW_PER_LINE else MAX_LINES
        if lines > limit:
            found.append((os.path.relpath(path, REPO_ROOT), lines, limit))
    return found


class Part2FileSizeTest(unittest.TestCase):
    def test_every_part2_file_is_within_300_lines(self):
        seen = list(files())
        self.assertGreater(len(seen), 3, 'the scan found almost nothing, so it is looking in the wrong place')
        self.assertEqual(too_long(seen), [])

    def test_a_301_line_file_is_caught_and_a_300_line_file_is_not(self):
        with tempfile.TemporaryDirectory() as folder:
            over, exact = os.path.join(folder, 'over.py'), os.path.join(folder, 'exact.py')
            open(over, 'w').write('x = 1\n' * 301)
            open(exact, 'w').write('x = 1\n' * 300)
            self.assertEqual([row[0:2] for row in too_long([over, exact])], [(os.path.relpath(over, REPO_ROOT), 301)])


if __name__ == '__main__':
    unittest.main()

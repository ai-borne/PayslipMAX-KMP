"""Part II Orders, Phase 10: the release documents stay true to the shipped data. A runbook that names a file that is gone, a plan that
quotes last month's template count, a CLAUDE.md past its 200 lines, or an iPhone checklist that forgets a Phase 11 item all fail
silently otherwise, and the owner signs off from these pages.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_release_docs.py'
"""
import glob
import json
import os
import re
import unittest

from config import CARDS_DIR, REPO_ROOT

PLAN = os.path.join(REPO_ROOT, 'docs', 'Plan', '12_Part2Orders_PhasePlan.md')
RUNBOOK = os.path.join(CARDS_DIR, '18_content_update_runbook.md')
CLAUDE_MD = os.path.join(REPO_ROOT, 'CLAUDE.md')
CHECKLIST = os.path.join(REPO_ROOT, 'docs', 'Part2Orders', 'IPHONE_CHECKLIST.md')
CLAUDE_MD_MAX = 200
# Phase 11 items that only an iPhone can close (plan items 14, 20 and 47), and what the checklist must name for each.
IPHONE_ITEMS = ('14', '20', '47')
IPHONE_TOPICS = ('VisionKit', 'Print', 'notification', 'back', 'lock')


def read(path):
    with open(path, encoding='utf-8') as fh:
        return fh.read()


def shipped_template_count():
    total = 0
    for path in glob.glob(os.path.join(CARDS_DIR, 'part2', 'templates', '*.json')):
        data = json.loads(read(path))
        total += len(data.get('templates', data) if isinstance(data, dict) else data)
    return total


def section(text, heading_pattern):
    """The body of the first `## ` section whose heading matches, up to the next `## ` heading, or None."""
    parts = re.split(r'(?m)^## ', text)
    for part in parts[1:]:
        if re.match(heading_pattern, part.split('\n', 1)[0]):
            return part
    return None


def repo_paths_named(body):
    """Backticked tokens that look like repo files (a slash and a known suffix); globs and placeholders are skipped."""
    found = set()
    for token in re.findall(r'`([^`\s]+)`', body):
        if '/' in token and re.search(r'\.(py|md|json|kt|sh|swift)$', token) and not re.search(r'[*<>{}]', token):
            found.add(token)
    return sorted(found)


def exists_anywhere(token):
    roots = (REPO_ROOT, CARDS_DIR, os.path.join(CARDS_DIR, 'tools'), os.path.join(REPO_ROOT, 'scripts'))
    return any(os.path.exists(os.path.join(root, token)) for root in roots)


class PlanTemplateCountTest(unittest.TestCase):
    def test_the_plan_quotes_the_shipped_template_count_wherever_it_says_all_templates(self):
        real = shipped_template_count()
        self.assertGreater(real, 24, 'the scan found almost no templates, so it is looking in the wrong place')
        text = read(PLAN)
        quoted = {int(n) for n in re.findall(r'all (\d+) templates', text) + re.findall(r'(\d+) templates ×', text)}
        self.assertEqual(quoted | {real}, {real}, f'the plan quotes {sorted(quoted)} templates, the shipped data has {real}')

    def test_phase_10_asks_for_sign_off_on_every_shipped_template(self):
        body = section(read(PLAN), r'Phases$') or ''
        phase_10 = body.split('### Phase 10', 1)[-1].split('### Phase 11', 1)[0]
        self.assertIn(f'all {shipped_template_count()} templates', phase_10)


class RunbookPartTwoSectionTest(unittest.TestCase):
    def setUp(self):
        self.body = section(read(RUNBOOK), r'\d+\. .*Part II') or ''

    def test_the_runbook_has_a_part_two_section(self):
        self.assertTrue(self.body, 'no "## N. ... Part II ..." section in 18_content_update_runbook.md')

    def test_the_section_covers_the_steps_a_template_change_needs(self):
        for needle in ('part2_templates.py', 'bundle.py', 'part2_review.py', 'refresh.py', 'PART2_ENABLED', 'owner'):
            self.assertIn(needle, self.body, f'the Part II section never mentions {needle}')

    def test_every_file_the_section_names_exists(self):
        named = repo_paths_named(self.body) + [p for p in re.findall(r'`(\w+\.py)`', self.body)]
        self.assertTrue(named, 'the section names no files at all')
        self.assertEqual([p for p in named if not exists_anywhere(p)], [])


class ClaudeMdTest(unittest.TestCase):
    def test_claude_md_stays_within_200_lines(self):
        self.assertLessEqual(len(read(CLAUDE_MD).splitlines()), CLAUDE_MD_MAX)

    def test_feature_areas_has_one_part_two_bullet_that_points_at_the_plan_and_the_flag(self):
        features = section_h3(read(CLAUDE_MD), 'Feature areas')
        bullets = [line for line in features.splitlines() if line.startswith('- ') and 'Part II' in line]
        self.assertEqual(len(bullets), 1, bullets)
        self.assertIn('docs/Plan/12_Part2Orders_PhasePlan.md', bullets[0])
        self.assertIn('LaunchFlags.PART2_ENABLED', bullets[0])
        self.assertTrue(os.path.exists(PLAN))


def section_h3(text, heading_start):
    parts = re.split(r'(?m)^### ', text)
    for part in parts[1:]:
        if part.startswith(heading_start):
            return part
    return ''


class IphoneChecklistTest(unittest.TestCase):
    def test_the_checklist_exists_and_names_each_phase_11_item_only_an_iphone_closes(self):
        self.assertTrue(os.path.exists(CHECKLIST), 'docs/Part2Orders/IPHONE_CHECKLIST.md is missing')
        text = read(CHECKLIST)
        for item in IPHONE_ITEMS:
            self.assertRegex(text, rf'(?i)items?[^)\n]*\b{item}\b', f'Phase 11 item {item} is not tied to a checklist line')

    def test_the_checklist_covers_the_native_surfaces_the_plan_lists(self):
        text = read(CHECKLIST) if os.path.exists(CHECKLIST) else ''
        missing = [topic for topic in IPHONE_TOPICS if topic.lower() not in text.lower()]
        self.assertEqual(missing, [])

    def test_the_checklist_is_within_300_lines_and_carries_no_real_figure_or_name(self):
        text = read(CHECKLIST) if os.path.exists(CHECKLIST) else ''
        self.assertLessEqual(len(text.splitlines()), 300)
        self.assertNotRegex(text, r'\b\d{9,}\b', 'a long number could be an account or PAN-like figure')


if __name__ == '__main__':
    unittest.main()

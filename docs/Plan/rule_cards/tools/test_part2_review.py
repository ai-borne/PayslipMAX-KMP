"""Tests for part2_review.py, the owner-review pack of the Part II content batches (Part II plan, phase 2b).

Why they matter: the owner checks every narrative, column and citation against the printed page before a batch counts as done, and the
pack is the only place the template is laid out next to the page it came from. A pack that left a template out, showed a column the
template does not print, or drifted from the committed templates would let a wrong row pass review. Run:
python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_review.py'
"""
import copy
import os
import unittest

import part2_codes_check
import part2_review
import part2_rules
import part2_synthetic as synthetic
import part2_templates

CODES = synthetic.codes()
RULES = synthetic.rules()
TEMPLATES = synthetic.templates()


def pack(templates=None):
    return part2_review.render(TEMPLATES if templates is None else templates, RULES, CODES)


class Content(unittest.TestCase):
    def test_every_template_has_one_section_with_its_serial_group_page_and_the_manuals_own_name(self):
        text = pack()
        for t in TEMPLATES:
            row = next(r for r in CODES['serials'] if t['code'] in r['codes'])
            self.assertEqual(text.count(f"## {t['code']} "), 1, t['code'])
            self.assertIn(f"serial {row['serial']}", text)
            self.assertIn(row['name'], text)
            self.assertIn(f"printed page {t['page']}", text)

    def test_a_template_section_lays_out_the_narrative_every_column_and_the_certificate(self):
        text = pack()
        self.assertIn('Granted the first thing for {days} days in {year} from {leave_from} to {leave_to}', text)
        self.assertIn('| From | required | `{leave_from}` |', text)
        self.assertIn('| Data 3 | not required |', text)
        self.assertIn('Certified that the second thing is in order', text)
        self.assertIn('Sanction {sanction.no} dated {sanction.date}', text)

    def test_the_rejoin_override_the_rules_and_the_links_are_shown_with_their_citations(self):
        text = pack()
        self.assertIn('Printed on the slip as `REJOIN`', text)
        self.assertIn('`cap_days`: warn, ODP-2026 p.115 note 2', text)
        self.assertIn('`grant_quoted`: ack, ODP-2026 p.131', text)
        self.assertIn('-> `BETARATION` (when `on_ration`): ODP-2026 p.97 note 1.1', text)
        self.assertIn('-> `BETASTOP` (when `in_hill_station`): seen in practice, not in the manual', text)

    def test_a_computed_optional_and_check_only_slot_says_what_it_is(self):
        templates = copy.deepcopy(TEMPLATES)
        t = next(x for x in templates if x['code'] == 'DELTANOTE')
        t['slots'] += [synthetic._slot('first', 'earliest', 'First', 'Worked out.', between=['noted_on', 'noted_on2']),
                       synthetic._slot('extra', 'text', 'Extra', 'Optional.', if_missing='Nil'),
                       synthetic._slot('earlier', 'number', 'Earlier', 'Only a check reads it.', **{'for': 'cap_days'})]
        text = part2_review.render(templates, RULES, CODES)
        self.assertIn('worked out: earliest of noted_on, noted_on2', text)
        self.assertIn('optional, prints "Nil" when left out', text)
        self.assertIn('only for the `cap_days` check, not printed', text)

    def test_the_open_points_the_owner_must_decide_are_listed_first(self):
        text = pack()
        self.assertLess(text.index('## Points to decide'), text.index('## ALPHAGRANT '))
        for point in part2_review.OPEN_POINTS:
            self.assertIn(point, text)

    def test_the_pack_is_plain_text_with_no_markup_from_the_templates(self):
        self.assertNotIn('<', pack().replace('<br>', ''))


class Stability(unittest.TestCase):
    def test_the_output_is_deterministic(self):
        self.assertEqual(pack(), pack())

    def test_a_changed_narrative_changes_the_pack(self):
        edited = copy.deepcopy(TEMPLATES)
        next(x for x in edited if x['code'] == 'DELTANOTE')['narrative'] += ' (edited)'
        self.assertNotEqual(pack(edited), pack())

    def test_the_committed_pack_is_current(self):
        # why: a stale pack would show the owner words that are no longer the shipped words.
        with open(part2_review.REVIEW_PATH, encoding='utf-8') as fh:
            committed = fh.read()
        real = part2_review.render(part2_templates.load_templates(), part2_rules.load_rules(), part2_codes_check.load())
        self.assertEqual(committed, real, 'run: python3 docs/Plan/rule_cards/tools/part2_review.py')

    def test_every_real_template_is_in_the_committed_pack(self):
        with open(part2_review.REVIEW_PATH, encoding='utf-8') as fh:
            committed = fh.read()
        codes = [t['code'] for t in part2_templates.load_templates()]
        self.assertGreaterEqual(len(codes), 24)
        for code in codes:
            self.assertEqual(committed.count(f'## {code} '), 1, code)
        self.assertTrue(os.path.basename(part2_review.REVIEW_PATH) == 'part2_review.md')


if __name__ == '__main__':
    unittest.main()

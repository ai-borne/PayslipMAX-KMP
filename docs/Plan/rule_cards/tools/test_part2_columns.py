"""Tests for part2/columns.json, the 303-row Annexure III table (Part II Orders Bank, phase 1): what each casualty's From, To and
Data 1-4 columns need, in the manual's own words.

Why they matter: the Bank's entry form is built from this table, so a wrong `required` makes the officer type a value the order never
carries, and a wrong `none` hides a field the clerk's order does print. The synthetic tests prove the reading rules and each refusal on
invented cells (no manual text is committed here); the shape tests prove the committed file; the oracle test compares 63 casualties
against needs exported once from the old branch's hand-built templates, an independent reading of the same printed pages.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_columns.py'
"""
import json
import os
import unittest

import part2_columns
import part2_columns_check
from config import CARDS_DIR
from part2_columns_hand import KNOWN_DISAGREEMENTS

HERE = os.path.dirname(os.path.abspath(__file__))
COLUMNS_PATH = os.path.join(CARDS_DIR, 'part2', 'columns.json')
CODES = json.load(open(os.path.join(CARDS_DIR, 'part2', 'codes.json'), encoding='utf-8'))
ORACLE = {r['code']: r for r in json.load(open(os.path.join(HERE, 'fixtures', 'part2_columns_oracle.json'), encoding='utf-8'))['rows']}
REAL = json.load(open(COLUMNS_PATH, encoding='utf-8')) if os.path.exists(COLUMNS_PATH) else None
COLS = ('from', 'to', 'd1', 'd2', 'd3', 'd4')

def cells(row):
    return [row['from'], row['to']] + row['data']


class NormaliseTest(unittest.TestCase):
    def test_a_date_the_column_wrapped_is_whole_again(self):
        for printed in ('dd/mm/ yyyy', 'dd/mm/yyy y', 'dd/mm/yy yy', 'dd/mm /yyyy'):
            self.assertEqual(part2_columns.normalise(f'Required as {printed}'), 'Required as dd/mm/yyyy', printed)

    def test_the_manuals_case_is_kept(self):
        self.assertEqual(part2_columns.normalise('Dd/mm/yyy y'), 'Dd/mm/yyyy')

    def test_a_word_cut_by_the_column_is_rejoined(self):
        self.assertEqual(part2_columns.normalise('Require d as  (name)'), 'Required as (name)')
        self.assertEqual(part2_columns.normalise('Not require d'), 'Not required')
        self.assertEqual(part2_columns.normalise('Nomina- tion form'), 'Nomination form')

    def test_placeholder_angle_brackets_are_dropped_and_entities_decoded(self):
        # why: the manual writes <Type of Leave> as a placeholder; the bundle refuses angle brackets, and an undecoded &lt; would show as text.
        self.assertEqual(part2_columns.normalise('Required as &lt;Type of Leave&gt;'), 'Required as Type of Leave')
        self.assertEqual(part2_columns.normalise('Pay &amp; Pension <x>'), 'Pay & Pension x')

    def test_superscript_ordinals_the_extractor_scattered_are_put_back(self):
        self.assertEqual(part2_columns.normalise('1 /2 st nd spell'), '1st/ 2nd spell')

    def test_whitespace_is_collapsed(self):
        self.assertEqual(part2_columns.normalise('  No \n of   days '), 'No of days')


class ClassifyTest(unittest.TestCase):
    def test_not_required_in_every_printed_spelling_is_none_with_no_caption(self):
        for printed in ('Not required', 'Not Required', 'Not Reqd', 'Not reqd', 'Not red', 'Not required (Prefix not allowed)', ''):
            self.assertEqual(part2_columns.classify(printed), ('none', ''), printed)

    def test_the_required_as_boilerplate_is_dropped_and_the_rest_kept_word_for_word(self):
        self.assertEqual(part2_columns.classify('Required as No of days'), ('required', 'No of days'))
        self.assertEqual(part2_columns.classify('Reqd as Name of Hospital'), ('required', 'Name of Hospital'))
        self.assertEqual(part2_columns.classify('Requires as Pt II Order No'), ('required', 'Pt II Order No'))

    def test_the_boilerplate_alone_leaves_a_blank_caption_not_the_boilerplate(self):
        self.assertEqual(part2_columns.classify('Required as'), ('required', ''))
        self.assertEqual(part2_columns.classify('Reqd. as Pvt./ Alternative Email ID'), ('required', 'Pvt./ Alternative Email ID'))
        self.assertEqual(part2_columns.classify('Required as---% of pay'), ('required', '---% of pay'))

    def test_a_cell_that_only_names_the_content_is_required(self):
        self.assertEqual(part2_columns.classify('Home Town….'), ('required', 'Home Town….'))

    def test_prefix_and_suffix_days_are_optional_because_the_manual_says_if_any(self):
        self.assertEqual(part2_columns.classify('Required as prefix dd/mm/yyyy'), ('optional', 'prefix dd/mm/yyyy'))
        self.assertEqual(part2_columns.classify('Reqd as Suffix dd/mm/yyyy'), ('optional', 'Suffix dd/mm/yyyy'))

    def test_a_cell_marked_optional_is_optional_and_the_marker_is_not_a_caption(self):
        self.assertEqual(part2_columns.classify('(Optional)'), ('optional', ''))
        self.assertEqual(part2_columns.classify('Reqd as Amount (Optional)'), ('optional', 'Amount'))

    def test_a_figure_the_manual_prints_as_an_example_is_not_a_caption(self):
        # why: "Required as 220" shows an example value; shown as a caption it would tell the officer to type 220.
        self.assertEqual(part2_columns.classify('Required as 220'), ('required', ''))
        self.assertEqual(part2_columns.classify('Required as 26/01/2014'), ('required', ''))
        self.assertEqual(part2_columns.classify('Required as10'), ('required', ''))
        self.assertTrue(part2_columns.prints_a_figure('Required as 220'))
        self.assertFalse(part2_columns.prints_a_figure('Required as dd/mm/yyyy'))
        self.assertFalse(part2_columns.prints_a_figure('Required as No of days'))

    def test_two_wrapped_dates_do_not_run_together(self):
        self.assertEqual(part2_columns.normalise('dd/mm/yyyy dd/mm/yyyy'), 'dd/mm/yyyy dd/mm/yyyy')

    def test_a_hand_read_row_replaces_the_rules_and_says_why(self):
        raw = {'cells': ['Not required'] * 6}
        hand = {'reason': 'read from the notes', 'cols': [('required', 'a')] + [('none', '')] * 5}
        row = part2_columns.build_row(raw, {'serial': '1.1', 'codes': ['X'], 'page': 1}, hand)
        self.assertEqual((row['from'], row['hand']), ({'need': 'required', 'caption': 'a'}, 'read from the notes'))

    def test_a_row_with_an_example_figure_is_marked_for_the_spot_check(self):
        raw = {'cells': ['Required as dd/mm/yyyy', 'Not required', 'Required as 220'] + ['Not required'] * 3}
        row = part2_columns.build_row(raw, {'serial': '1.1', 'codes': ['X'], 'page': 1})
        self.assertIn('example', row['hand'])


def good_row():
    row = {'serial': '1.1', 'codes': ['GTDPC'], 'page': 28,
           'from': {'need': 'required', 'caption': 'dd/mm/yyyy'}, 'to': {'need': 'none', 'caption': ''},
           'data': [{'need': 'none', 'caption': ''}] * 4}
    return row


def tiny(rows):
    return {'version': 1, 'source': 'x', 'rows': rows}, {'serials': [{'serial': r['serial'], 'codes': r['codes'], 'page': r['page']} for r in rows]}


class RefusalTest(unittest.TestCase):
    """Each rule is broken on purpose: a checker that cannot fail would let a bad table into the app."""

    def refused(self, edit, *fragments):
        row = good_row()
        edit(row)
        data, codes = tiny([good_row()])
        data['rows'] = [row]
        found = part2_columns_check.problems(data, codes)
        self.assertTrue(any(all(f in line for f in fragments) for line in found), f'wanted {fragments} in {found}')

    def test_a_good_row_passes(self):
        self.assertEqual(part2_columns_check.problems(*tiny([good_row()])), [])

    def test_wrong_code(self):
        self.refused(lambda r: r.update(codes=['GTDXX']), '1.1', 'codes')

    def test_unknown_need(self):
        self.refused(lambda r: r['from'].update(need='maybe'), '1.1', 'need')

    def test_a_none_column_with_a_caption(self):
        self.refused(lambda r: r['to'].update(caption='Not required'), '1.1', 'none', 'caption')

    def test_angle_brackets(self):
        self.refused(lambda r: r['from'].update(caption='<script>'), '1.1', 'angle')

    def test_caption_too_long(self):
        self.refused(lambda r: r['from'].update(caption='x' * 400), '1.1', 'caption', 'over')

    def test_control_characters(self):
        self.refused(lambda r: r['from'].update(caption='a\x00b'), '1.1', 'control')

    def test_not_four_data_columns(self):
        self.refused(lambda r: r.update(data=r['data'][:3]), '1.1', 'data')

    def test_a_missing_serial(self):
        data, codes = tiny([good_row()])
        codes['serials'].append({'serial': '1.2', 'codes': ['GTDSSC'], 'page': 28})
        self.assertTrue(any('1.2' in p and 'missing' in p for p in part2_columns_check.problems(data, codes)))

    def test_a_serial_that_is_not_in_codes(self):
        data, codes = tiny([good_row()])
        data['rows'].append(dict(good_row(), serial='9.9'))
        self.assertTrue(any('9.9' in p for p in part2_columns_check.problems(data, codes)))

    def test_a_duplicate_serial(self):
        data, codes = tiny([good_row()])
        data['rows'].append(good_row())
        self.assertTrue(any('duplicate' in p for p in part2_columns_check.problems(data, codes)))

    def test_an_unknown_version(self):
        data, codes = tiny([good_row()])
        data['version'] = 2
        self.assertTrue(any('version' in p for p in part2_columns_check.problems(data, codes)))

    def test_a_hand_read_reason_is_required_to_be_text_and_bounded(self):
        self.refused(lambda r: r.update(hand='y' * 500), '1.1', 'hand')


class CommittedTableTest(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(REAL, 'part2/columns.json is missing (a missing table must fail, never skip)')

    def test_the_file_passes_every_check(self):
        self.assertEqual(part2_columns_check.problems(REAL, CODES), [])

    def test_one_row_per_serial_in_codes_order_with_the_same_codes(self):
        self.assertEqual([r['serial'] for r in REAL['rows']], [s['serial'] for s in CODES['serials']])
        self.assertEqual([r['codes'] for r in REAL['rows']], [s['codes'] for s in CODES['serials']])
        self.assertEqual(len(REAL['rows']), 303)

    def test_a_none_column_never_carries_a_caption(self):
        for row in REAL['rows']:
            for col in cells(row):
                if col['need'] == 'none':
                    self.assertEqual(col['caption'], '', row['serial'])

    def test_the_same_page_as_codes_json(self):
        self.assertEqual([r['page'] for r in REAL['rows']], [s['page'] for s in CODES['serials']])

    def test_the_file_is_exactly_what_render_writes(self):
        with open(COLUMNS_PATH, encoding='utf-8') as fh:
            self.assertEqual(fh.read(), part2_columns.render(REAL))

    def test_every_casualty_has_a_from_date_or_says_why_not(self):
        # why: 60 of the 63 built casualties need a From date; a table with no From column anywhere is a misread grid.
        needing = sum(1 for r in REAL['rows'] if r['from']['need'] != 'none')
        self.assertGreater(needing, 250)

    def test_the_63_built_casualties_match_the_independent_oracle(self):
        by_code = {code: row for row in REAL['rows'] for code in row['codes']}
        seen = set()
        for code, want in ORACLE.items():
            got = [c['need'] for c in cells(by_code[code])]
            expected = [want['from'], want['to']] + want['data']
            for i, (g, e) in enumerate(zip(got, expected)):
                if g != e:
                    seen.add((code, COLS[i]))
                    self.assertEqual(KNOWN_DISAGREEMENTS.get((code, COLS[i]), (None,))[0:2], (g, e), f'{code} {COLS[i]} differs from the oracle')
        self.assertEqual(seen, set(KNOWN_DISAGREEMENTS), 'a listed disagreement no longer exists, or a new one is unlisted')
        self.assertEqual(len(ORACLE), 63)


if __name__ == '__main__':
    unittest.main()

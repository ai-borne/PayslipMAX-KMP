"""Tests for the TD summary card's data path (plan 24, P3): the `summary=td` card, its place first in the TD area, its authority wording,
and the DA tier the free table is drawn at.

Why they matter: the card is read by every officer, so its words may hold no rupee amount (the table computes them; a typed number
would go stale at the next DA step), the DoE OM may only ever be named as the civil parent beside the MoD letter (register 23, S7;
its para 8 leaves Armed Forces to MoD), and the bundle must say which card gets the table. Run: python3 tools/test_td_summary_card.py
"""
import json
import os
import re
import unittest

import bundle
import compile as comp
import figures
from config import CARDS_DIR
from test_figures import REAL, fresh, problems

RULEBOOK = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))
BUILT = bundle.build(RULEBOOK)
CARD_ID = 'RB-TD-SUMMARY'
RUPEES = re.compile(r'₹|\bRs\.?\s*\d', re.I)


def summary_cards(cards):
    return [c for c in cards if c.get('summary')]


class TheCard(unittest.TestCase):
    def setUp(self):
        self.card = next(c for c in RULEBOOK['cards'] if c['id'] == CARD_ID)

    def test_exactly_one_card_carries_the_td_summary(self):
        self.assertEqual([(c['id'], c['summary']) for c in summary_cards(RULEBOOK['cards'])], [(CARD_ID, 'td')])

    def test_it_is_the_first_card_in_the_td_daily_allowance_case(self):
        case = next(c for a in RULEBOOK['nav'] for c in a['cases'] if c['id'] == 'td-da')
        self.assertEqual(case['cards'][0], CARD_ID)
        self.assertEqual(self.card['nav'], 'td-da')
        self.assertEqual(self.card['facet'], 'H')

    def test_it_names_the_mod_letters_and_the_doe_om_only_as_the_civil_parent(self):
        cite = self.card['cite']
        self.assertIn('12630/Mov C/242/D(Mov)/2017', cite)
        self.assertNotIn('1(23)/2017/D(Pay/Services)', cite)  # the CEA letter: CEA is not on this card
        self.assertIn('civil parent: DoE OM 19030/1/2017-E.IV', cite)
        # Every mention of the DoE OM is the civil-parent one: it never stands alone as the Army entitlement.
        self.assertEqual(cite.count('DoE OM'), cite.count('civil parent: DoE OM'))

    def test_no_user_read_text_holds_a_rupee_amount(self):
        words = [self.card['title'], self.card['answer'], self.card['details'], *self.card['key'], *self.card['attach'], *self.card['watch']]
        for text in words:
            self.assertIsNone(RUPEES.search(text), text)

    def test_the_card_does_not_relay_cea(self):
        # Owner decision 2026-11-07: CEA and hostel subsidy live on their own card (RB-SS-P092); the TD card must not repeat them.
        text = ' '.join([self.card['title'], self.card['answer'], self.card['details'], self.card['cite'], *self.card['key'], *self.card['attach'], *self.card['watch']])
        for word in ('cea', 'hostel', 'children education'):
            self.assertNotIn(word, text.lower())

    def test_the_table_is_for_officers_so_the_card_says_so(self):
        # JCO and OR rates are not approved figures (register 23, S7), so the card must not read as if it covered them.
        self.assertTrue('officer' in self.card['answer'].lower())


class TheBundle(unittest.TestCase):
    def test_only_the_summary_card_ships_a_summary_field(self):
        shipped = [c for c in BUILT['cards'] if 'summary' in c]
        self.assertEqual([(c['id'], c['summary']) for c in shipped], [(CARD_ID, 'td')])

    def test_no_cea_or_hostel_figure_ships(self):
        self.assertFalse({'cea', 'hostel_subsidy'} & set(BUILT['figures']['figures']))

    def test_the_free_table_is_drawn_at_the_step_in_force_so_it_ships_the_da_tier(self):
        tier = BUILT['figures']['da_step']['table_da_percent']
        self.assertEqual(tier, 50)
        step = BUILT['figures']['da_step']
        self.assertEqual(step['increase_percent'] * (tier // step['per_da_percent']), 25)


class TheValidators(unittest.TestCase):
    def test_a_missing_or_zero_table_da_percent_fails(self):
        for bad in (None, 0, -50, '50'):
            d = fresh()
            if bad is None:
                del d['da_step']['table_da_percent']
            else:
                d['da_step']['table_da_percent'] = bad
            self.assertTrue(any('table_da_percent' in p for p in problems(d)), repr(bad))

    def test_the_real_file_is_valid(self):
        self.assertEqual(problems(REAL), [])

    def test_an_unknown_summary_kind_fails(self):
        errs = comp.summary_problems([{'id': 'RB-X', 'summary': 'ta', 'where': 'x'}])
        self.assertTrue(any("summary 'ta'" in e for e in errs))

    def test_two_cards_may_not_claim_one_summary(self):
        errs = comp.summary_problems([{'id': 'RB-A', 'summary': 'td'}, {'id': 'RB-B', 'summary': 'td'}])
        self.assertTrue(any('RB-A' in e and 'RB-B' in e for e in errs))

    def test_one_card_per_known_kind_is_clean(self):
        self.assertEqual(comp.summary_problems([{'id': 'RB-A', 'summary': 'td'}, {'id': 'RB-C'}]), [])


if __name__ == '__main__':
    unittest.main()

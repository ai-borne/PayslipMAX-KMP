"""Pins for the three hospital-stay cards (RB-HS-01..03).

Why they matter: these cards tell an officer that a hospital stay without Part II orders can cost pay. A card
that drifts from its home (Leave > annual, casual and sick leave), cites the wrong authority, or names the source
letter would mislead or breach the owner decision of 2026-11-06 (cite ODP 2026 and the Leave Rules only).
Run: python3 tools/test_hospital_cards.py
"""
import json
import os
import re
import unittest

import bundle
import compile as compiler
from config import CARDS_DIR

RULEBOOK = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))
BY_ID = {c['id']: c for c in RULEBOOK['cards']}
IDS = ['RB-HS-01', 'RB-HS-02', 'RB-HS-03']
BUILT = {c['id']: c for c in bundle.build(RULEBOOK)['cards']}
LETTER_MARKERS = re.compile(r'B/25451|PS-6\(D\)|ADG PS|AG.s Branch|17 ?Nov|Nov 2025', re.I)


class HospitalCards(unittest.TestCase):
    def test_the_three_cards_exist_and_ship(self):
        for i in IDS:
            self.assertIn(i, BY_ID, i)
            self.assertIn(i, BUILT, i)

    def test_each_is_homed_with_the_leave_cards_not_in_a_new_case(self):
        home = BY_ID['RB-RP-016']['nav']
        for i in IDS:
            self.assertEqual(BY_ID[i]['nav'], home, i)

    def test_each_cites_only_odp_2026_and_the_leave_rules(self):
        for i in IDS:
            cite = BY_ID[i]['cite']
            self.assertTrue(cite.strip(), i)
            self.assertRegex(cite, r'ODP\)? 2026|Leave Rules for the Services', i)

    def test_nothing_shipped_names_the_letter(self):
        # why: owner decision (a), 2026-11-06 - the app never names the source letter or its number.
        for i in IDS:
            card = BUILT[i]
            text = json.dumps(card)
            self.assertIsNone(LETTER_MARKERS.search(text), f'{i}: {LETTER_MARKERS.search(text)}')

    def test_the_cards_carry_no_rupee_figure(self):
        for i in IDS:
            self.assertNotRegex(json.dumps(BUILT[i]), r'₹|Rs\.? ?\d', i)

    def test_each_is_within_the_compilers_limits(self):
        lim = compiler.LIMITS
        for i in IDS:
            c = BY_ID[i]
            self.assertLessEqual(compiler.wc(c['title']), lim['title'], i)
            self.assertLessEqual(compiler.wc(c['answer']), lim['answer'], i)
            self.assertLessEqual(len(c['key']), lim['bullets'], i)
            self.assertLessEqual(compiler.wc(c['details']), lim['details'], i)

    def test_the_split_card_states_the_180_day_full_pay_rule(self):
        # why: this is the number an officer plans around; losing it would silently void the card.
        text = json.dumps(BY_ID['RB-HS-03'])
        self.assertIn('180', text)
        self.assertIn('furlough', text.lower())

    def test_the_orders_card_names_the_regularising_codes(self):
        text = json.dumps(BY_ID['RB-HS-02'])
        for code in ('HOSADM', 'HOSDISCH', 'RPDUHSP', 'SLC', 'REGAL', 'REGALMS'):
            self.assertIn(code, text, code)

    def test_the_time_bar_is_on_the_consequence_card(self):
        text = json.dumps(BY_ID['RB-HS-01']).lower()
        self.assertIn('two years', text)
        self.assertIn('recover', text)


if __name__ == '__main__':
    unittest.main()

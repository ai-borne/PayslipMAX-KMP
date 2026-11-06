"""Pins for the hospital-stay cards (RB-HS-01..03 rules, RB-HS-04..06 worked examples).

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
IDS = ['RB-HS-01', 'RB-HS-02', 'RB-HS-03', 'RB-HS-04', 'RB-HS-05', 'RB-HS-06']
EXAMPLES = ['RB-HS-04', 'RB-HS-05', 'RB-HS-06']
BUILT = {c['id']: c for c in bundle.build(RULEBOOK)['cards']}
LETTER_MARKERS = re.compile(r'B/25451|PS-6\(D\)|ADG PS|AG.s Branch|17 ?Nov|Nov 2025', re.I)


class HospitalCards(unittest.TestCase):
    def test_all_the_cards_exist_and_ship(self):
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

    @staticmethod
    def _parts(card_id):
        """The 'CODE days' pairs a worked example lists in its key points, in order."""
        text = ' '.join(BY_ID[card_id]['key'])
        return [(code, int(n)) for code, n in re.findall(r'\b(GTDALHOS|GTDSLHOS|HOSFURPY|SLC)\b,? (\d+)', text)]

    def test_the_175_day_example_adds_up(self):
        # why: an example whose parts do not sum to the stay teaches the wrong split; 30 + 120 + 25 = 175.
        parts = dict(self._parts('RB-HS-04'))
        self.assertEqual(parts['GTDALHOS'] + parts['GTDSLHOS'] + parts['HOSFURPY'], 175)
        self.assertEqual(parts['GTDALHOS'] + parts['GTDSLHOS'], 150, 'full pay stops at the annual leave balance, not at 180')

    def test_the_two_year_example_adds_up_and_caps_full_pay_at_180(self):
        parts = self._parts('RB-HS-06')
        furlough = sum(n for c, n in parts if c == 'HOSFURPY')
        full = sum(n for c, n in parts if c != 'HOSFURPY')
        self.assertEqual(full, 180)
        self.assertEqual(full + furlough, 260)

    def test_the_shorter_stays_card_keeps_service_stays_free_of_furlough(self):
        # why: a stay due to military service stays at full pay throughout; a furlough part here would be wrong.
        text = ' '.join(BY_ID['RB-HS-05']['key'])
        self.assertIn('SLC', text)
        self.assertIn('REGALMS', text)
        self.assertNotIn('HOSFURPY', text)
        parts = dict(self._parts('RB-HS-05'))
        self.assertEqual(parts['GTDALHOS'] + parts['GTDSLHOS'], 150)

    def test_each_example_names_its_orders_and_a_home_title(self):
        for i in EXAMPLES:
            self.assertTrue(BY_ID[i]['title'].startswith('Example'), i)
            self.assertIn('HOSADM', ' '.join(BY_ID[i]['key']), i)


if __name__ == '__main__':
    unittest.main()

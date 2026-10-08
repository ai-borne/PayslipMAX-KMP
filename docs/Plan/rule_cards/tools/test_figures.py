"""Tests for figures.py, the validator behind the Claim Guide's "your figure" line (phase E6).

Why they matter: a wrong rupee figure on a rule card is a wrong claim, so a figure may only ship with a value, an
effective date, a primary letter and the owner's approval, and two figures may never both claim one level. Run:
python3 tools/test_figures.py
"""
import copy
import json
import os
import unittest

import figures
from config import CARDS_DIR

REAL = json.load(open(os.path.join(CARDS_DIR, 'figures.json'), encoding='utf-8'))
RULEBOOK = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))
CARDS = [{'id': c['id'], 'personal': c['personal']} for c in RULEBOOK['cards']]


def problems(data, cards=CARDS):
    return figures.validate(data, cards)


def fresh():
    return copy.deepcopy(REAL)


class RealFigures(unittest.TestCase):
    def test_the_authored_file_is_valid_and_approved(self):
        self.assertEqual(problems(REAL), [])
        self.assertEqual(figures.unapproved(REAL), [])

    def test_exactly_the_four_personal_cards_have_a_figure(self):
        self.assertEqual({f['card'] for f in REAL['figures'].values()},
                         {'RB-SS-T181', 'RB-SS-T254', 'RB-SS-P051-rates', 'RB-SS-P116-rates'})


class SourceRule(unittest.TestCase):
    """No figure without its primary letter, date, evidence level and approval."""

    def test_missing_authority_fails(self):
        d = fresh()
        d['figures']['food_rate']['authority'] = ''
        self.assertTrue(any('food_rate' in p and 'authority' in p for p in problems(d)))

    def test_bad_effective_date_fails(self):
        d = fresh()
        d['figures']['ctg']['effective_from'] = '01-07-2017'
        self.assertTrue(any('ctg' in p and 'effective_from' in p for p in problems(d)))

    def test_unknown_evidence_level_fails(self):
        d = fresh()
        d['figures']['ctg']['evidence'] = 'HEARSAY'
        self.assertTrue(any('ctg' in p and 'evidence' in p for p in problems(d)))

    def test_an_hra_step_needs_its_own_date(self):
        d = fresh()
        del d['figures']['hra']['classes']['X'][1]['effective_from']
        self.assertTrue(any('hra' in p and 'effective_from' in p for p in problems(d)))

    def test_unapproved_figure_is_listed_and_blocks_the_bundle(self):
        d = fresh()
        d['figures']['transport_allowance']['approved'] = None
        self.assertEqual(figures.unapproved(d), ['transport_allowance'])
        with self.assertRaises(SystemExit):
            figures.for_bundle(d)


class CardLinks(unittest.TestCase):
    def test_figure_for_an_unknown_card_fails(self):
        d = fresh()
        d['figures']['ctg']['card'] = 'RB-NOPE'
        self.assertTrue(any('RB-NOPE' in p for p in problems(d)))

    def test_figure_for_a_card_without_a_personal_spec_fails(self):
        d = fresh()
        d['figures']['ctg']['card'] = RULEBOOK['cards'][0]['id']
        self.assertTrue(any('personal' in p for p in problems(d)))

    def test_a_personal_card_with_no_figure_fails(self):
        d = fresh()
        del d['figures']['hra']
        self.assertTrue(any('RB-SS-P116-rates' in p for p in problems(d)))


class Shapes(unittest.TestCase):
    def test_two_bands_may_not_claim_one_level_and_city_class(self):
        d = fresh()
        d['figures']['food_rate']['bands'][1]['levels'].append('11')
        self.assertTrue(any('food_rate' in p and '11' in p for p in problems(d)))

    def test_an_any_class_band_may_not_share_a_level_with_a_named_class(self):
        d = fresh()
        d['figures']['transport_allowance']['bands'][2]['levels'].append('13A')
        self.assertTrue(any('transport_allowance' in p and '13A' in p for p in problems(d)))

    def test_unknown_mode_fails(self):
        d = fresh()
        d['figures']['ctg']['mode'] = 'magic'
        self.assertTrue(any('ctg' in p and 'mode' in p for p in problems(d)))

    def test_ctg_percent_must_be_a_sensible_percent(self):
        d = fresh()
        d['figures']['ctg']['percent'] = 180
        self.assertTrue(any('ctg' in p and 'percent' in p for p in problems(d)))

    def test_hra_steps_must_rise_with_da(self):
        d = fresh()
        d['figures']['hra']['classes']['Y'][2]['from_da_percent'] = 10
        self.assertTrue(any('hra' in p and 'ascending' in p for p in problems(d)))

    def test_hra_classes_must_share_the_same_da_steps(self):
        d = fresh()
        d['figures']['hra']['classes']['Z'][2]['from_da_percent'] = 60
        self.assertTrue(any('hra' in p and 'same' in p for p in problems(d)))

    def test_da_step_needs_positive_numbers(self):
        d = fresh()
        d['da_step']['per_da_percent'] = 0
        self.assertTrue(any('da_step' in p for p in problems(d)))


class Shipping(unittest.TestCase):
    def test_bundle_copy_keeps_only_what_the_app_needs(self):
        shipped = figures.for_bundle(REAL)
        self.assertEqual(set(shipped), {'da_step', 'figures'})
        self.assertEqual(shipped['da_step'], {'per_da_percent': 50, 'increase_percent': 25})
        for key, f in shipped['figures'].items():
            for private in ('authority', 'evidence', 'evidence_detail', 'approved', 'note', 'decided_by'):
                self.assertNotIn(private, f, key)
            # Every figure shows a date: rate tables carry one per step, the others carry one of their own.
            if f['mode'] != 'rate_table':
                self.assertIn('effective_from', f, key)
        for steps in shipped['figures']['hra']['classes'].values():
            for step in steps:
                self.assertEqual(set(step), {'from_da_percent', 'percent', 'effective_from'})


if __name__ == '__main__':
    unittest.main()

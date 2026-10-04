"""Tests for the nav checker: it must fail the build when a card has no home, a card is homed twice,
an area has too many cases, or a case is too small or too large. Run: python3 tools/test_nav.py"""
import copy
import json
import os
import unittest

import nav

HERE = os.path.dirname(os.path.abspath(__file__))
CARDS_DIR = os.path.abspath(os.path.join(HERE, '..'))
CARDS = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))['cards']
NAV = nav.load_nav(CARDS_DIR)


def run(mutate):
    n = copy.deepcopy(NAV)
    mutate(n)
    errs = []
    nav.build_nav(CARDS, n, errs)
    return errs


class NavChecks(unittest.TestCase):
    def test_committed_nav_is_clean(self):
        self.assertEqual(run(lambda n: None), [])

    def test_orphan_card_is_an_error(self):
        errs = run(lambda n: n['areas'][0]['cases'][0]['topics'].clear())
        self.assertTrue(any('has no home' in e for e in errs))

    def test_card_homed_twice_is_an_error(self):
        def dup(n):
            n['areas'][2]['cases'][1]['topics'].append(n['areas'][2]['cases'][0]['topics'][0])
        self.assertTrue(any('homed twice' in e for e in run(dup)))

    def test_too_many_cases_in_an_area_is_an_error(self):
        errs = run(lambda n: n['limits'].update(cases_per_area=3))
        self.assertTrue(any('max 3' in e for e in errs))

    def test_case_size_limits_are_enforced(self):
        errs = run(lambda n: n['limits'].update(cards_per_case=[9, 10]))
        self.assertTrue(any('allowed 9 to 10' in e for e in errs))

    def test_also_card_must_exist(self):
        errs = run(lambda n: n['areas'][0]['cases'][0].update(also=['RB-NOPE']))
        self.assertTrue(any('unknown also-card' in e for e in errs))


if __name__ == '__main__':
    unittest.main()

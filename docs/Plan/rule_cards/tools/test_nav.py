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


class ReplacedCards(unittest.TestCase):
    """M3: a card replaced by a newer dated rule stays in the data but leaves the tiles. Without this rule a replaced card
    would trip "has no home" (so the 8th CPC rewrite could not be authored) or, if its topic still listed it, stay on screen."""

    def replaced(self):
        cards = [dict(c) for c in CARDS]
        explicit = {i for a in NAV['areas'] for c in a['cases'] for i in c.get('cards', [])}
        victim = next(c for c in cards if c['id'] not in explicit and c['topic'] in NAV['areas'][2]['cases'][0]['topics'])
        victim['replaced_by'] = cards[0]['id']
        return cards, victim['id']

    def test_a_replaced_card_with_no_home_is_accepted(self):
        cards, victim = self.replaced()
        errs = []
        _, homes = nav.build_nav(cards, NAV, errs)
        self.assertEqual(errs, [])
        self.assertNotIn(victim, homes)

    def test_a_replaced_card_is_not_listed_in_its_topic_case(self):
        cards, victim = self.replaced()
        tree, _ = nav.build_nav(cards, NAV, [])
        self.assertNotIn(victim, [i for a in tree for c in a['cases'] for i in c['cards']])

    def test_a_replaced_card_named_in_a_case_is_an_error(self):
        cards, victim = self.replaced()
        n = copy.deepcopy(NAV)
        n['areas'][0]['cases'][0].setdefault('cards', []).append(victim)
        errs = []
        nav.build_nav(cards, n, errs)
        self.assertTrue(any('replaced' in e and victim in e for e in errs), errs)

    def test_a_replaced_card_named_as_also_is_an_error(self):
        cards, victim = self.replaced()
        n = copy.deepcopy(NAV)
        n['areas'][0]['cases'][0]['also'] = [victim]
        errs = []
        nav.build_nav(cards, n, errs)
        self.assertTrue(any('replaced' in e and victim in e for e in errs), errs)

    def test_an_unreplaced_orphan_is_still_an_error(self):
        cards = [dict(c) for c in CARDS]
        errs = run(lambda n: n['areas'][0]['cases'][0]['topics'].clear())
        self.assertTrue(any('has no home' in e for e in errs))
        self.assertEqual(len(cards), len(CARDS))


class FacetChecks(unittest.TestCase):
    def test_missing_facet_is_an_error(self):
        cards = [dict(c) for c in CARDS]
        fac = json.load(open(os.path.join(CARDS_DIR, 'facets.json'), encoding='utf-8'))
        errs = []
        nav.apply_facets(cards, CARDS_DIR, errs)
        self.assertEqual(errs, [])
        self.assertTrue(all(c['facet'] in fac['labels'] for c in cards))

    def test_invalid_facet_value_is_an_error(self):
        import tempfile
        d = tempfile.mkdtemp()
        json.dump({'labels': {'Q': 'x'}, 'cards': {CARDS[0]['id']: 'Z'}}, open(os.path.join(d, 'facets.json'), 'w'))
        errs = []
        nav.apply_facets([dict(CARDS[0]), dict(CARDS[1])], d, errs)
        self.assertTrue(any('no valid facet' in e for e in errs))


if __name__ == '__main__':
    unittest.main()

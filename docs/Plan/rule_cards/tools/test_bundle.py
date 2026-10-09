"""Tests for bundle.py, the step that turns rulebook.json into the file the app ships.

Why they matter: the bundle is the only Guide data a user ever sees, so internal provenance (`from`) and reviewer
notes (`open`) must never reach it, every open point must surface as the "Unverified point" chip, and a rulebook
edit that was not re-bundled must fail the build instead of shipping stale rates. Run: python3 tools/test_bundle.py
"""
import copy
import json
import os
import re
import unittest

import bundle
import figures
from config import BUNDLE_PATH, BUNDLE_VERSION, CARDS_DIR, RATES_AS_OF

RULEBOOK = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))
BUILT = bundle.build(RULEBOOK)


class BundleContents(unittest.TestCase):
    def test_internal_fields_never_ship(self):
        for top in ('topics', 'skipped', 'coverage', 'uncovered', 'pay_topics_open'):
            self.assertNotIn(top, BUILT)
        for card in BUILT['cards']:
            self.assertNotIn('from', card, card['id'])
            self.assertNotIn('open', card, card['id'])

    def test_a_new_private_card_field_is_not_shipped_by_default(self):
        rb = copy.deepcopy(RULEBOOK)
        rb['cards'][0]['reviewer_note'] = 'internal'
        self.assertNotIn('reviewer_note', bundle.build(rb)['cards'][0])

    def test_every_open_point_becomes_unverified(self):
        by_id = {c['id']: c for c in RULEBOOK['cards']}
        for card in BUILT['cards']:
            self.assertEqual(card['unverified'], bool(by_id[card['id']]['open']), card['id'])
        self.assertEqual(sum(c['unverified'] for c in BUILT['cards']), 37)

    def test_header_carries_version_and_rates_month(self):
        self.assertEqual(BUILT['version'], BUNDLE_VERSION)
        self.assertEqual(BUILT['rates_as_of'], RATES_AS_OF)
        self.assertRegex(RATES_AS_OF, r'^\d{4}-(0[1-9]|1[0-2])$')
        self.assertEqual(BUILT['generated'], RULEBOOK['generated'])

    def test_matches_the_compiled_dataset(self):
        cards = BUILT['cards']
        self.assertEqual(len(cards), 404)
        self.assertEqual(sum(c['domain'] == 'travel' for c in cards), 220)
        self.assertEqual(sum(c['domain'] == 'pay' for c in cards), 184)
        self.assertEqual(len(BUILT['nav']), 9)
        self.assertEqual(sum(len(a['cases']) for a in BUILT['nav']), 44)
        self.assertEqual(BUILT['facets'], RULEBOOK['facets'])

    def test_guidance_means_no_cite(self):
        for card in BUILT['cards']:
            self.assertEqual('GUIDANCE' in card['chips'], card['cite'] == '', card['id'])
        self.assertEqual(sum(c['cite'] == '' for c in BUILT['cards']), 31)


class BundleFigures(unittest.TestCase):
    """The "your figure" rupee values reach the app only through the bundle (phase E6), and only once approved."""

    def test_approved_figures_ship_for_the_four_personal_cards(self):
        shipped = BUILT['figures']['figures']
        self.assertEqual(set(shipped), {'food_rate', 'ctg', 'transport_allowance', 'hra'})
        personal = {c['id'] for c in BUILT['cards'] if c['personal']}
        self.assertEqual({f['card'] for f in shipped.values()}, personal)

    def test_an_unapproved_figure_stops_the_build(self):
        data = figures.load()
        data['figures']['ctg']['approved'] = None
        with self.assertRaises(SystemExit):
            bundle.build(RULEBOOK, data)

    def test_no_authoring_notes_ship(self):
        text = bundle.render(BUILT)
        for private in ('evidence', 'HANDBOOK_ONLY', 'LETTER_TEXT', 'decided_by'):
            self.assertNotIn(private, text)


class BundleFile(unittest.TestCase):
    def test_render_is_deterministic(self):
        self.assertEqual(bundle.render(BUILT), bundle.render(bundle.build(copy.deepcopy(RULEBOOK))))

    def test_committed_bundle_is_up_to_date(self):
        # Stale-bundle guard: fails when rulebook.json changed and bundle.py was not rerun.
        with open(BUNDLE_PATH, encoding='utf-8') as fh:
            committed = fh.read()
        self.assertEqual(committed, bundle.render(BUILT), 'run python3 docs/Plan/rule_cards/tools/bundle.py')

    def test_committed_bundle_has_no_internal_keys(self):
        with open(BUNDLE_PATH, encoding='utf-8') as fh:
            text = fh.read()
        self.assertIsNone(re.search(r'"(from|open)"\s*:', text))


if __name__ == '__main__':
    unittest.main()

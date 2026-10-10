"""Tests for bundle.py, the step that turns rulebook.json into the file the app ships.

Why they matter: the bundle is the only Guide data a user ever sees, so internal provenance (`from`) and reviewer
notes (`open`) must never reach it, every open point must surface as the "Unverified point" chip, and a rulebook
edit that was not re-bundled must fail the build instead of shipping stale rates. Run: python3 tools/test_bundle.py
"""
import copy
import hashlib
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


TEXT_FIELDS = ('title', 'answer', 'key', 'attach', 'watch', 'cite', 'details')


def edited(field):
    rb = copy.deepcopy(RULEBOOK)
    card = rb['cards'][0]
    card[field] = card[field] + ['changed'] if isinstance(card[field], list) else card[field] + ' changed'
    return rb


class CardRevision(unittest.TestCase):
    """`rev` lets a user's note (phase M6) notice that the card it was written on has since changed."""

    def test_rev_is_the_first_8_hex_of_sha256_over_the_shipped_text_fields(self):
        card = RULEBOOK['cards'][5]
        text = json.dumps([card[f] for f in TEXT_FIELDS], ensure_ascii=False, separators=(',', ':'))
        expected = hashlib.sha256(text.encode('utf-8')).hexdigest()[:8]
        self.assertEqual(BUILT['cards'][5]['rev'], expected)
        self.assertRegex(expected, r'^[0-9a-f]{8}$')

    def test_every_card_has_a_rev_and_it_is_stable_across_builds(self):
        self.assertTrue(all(re.fullmatch(r'[0-9a-f]{8}', c['rev']) for c in BUILT['cards']))
        again = bundle.build(copy.deepcopy(RULEBOOK))
        self.assertEqual([c['rev'] for c in BUILT['cards']], [c['rev'] for c in again['cards']])

    def test_rev_changes_when_any_shipped_text_changes(self):
        for field in TEXT_FIELDS:
            self.assertNotEqual(bundle.build(edited(field))['cards'][0]['rev'], BUILT['cards'][0]['rev'], field)

    def test_rev_ignores_what_does_not_change_the_words_a_user_reads(self):
        # why: a note must not be flagged "card updated" because of a reviewer note, a date or a source id.
        rb = copy.deepcopy(RULEBOOK)
        rb['generated'] = '2030-01-01'
        rb['cards'][0]['open'] = ['new reviewer question']
        rb['cards'][0]['from'] = ['SS-T999']
        self.assertEqual(bundle.build(rb)['cards'][0]['rev'], BUILT['cards'][0]['rev'])

    def test_a_text_change_is_visible_in_the_rendered_bundle(self):
        self.assertNotEqual(bundle.render(bundle.build(edited('answer'))), bundle.render(BUILT))


class RuleChangeFields(unittest.TestCase):
    """Rule-change fields are additive: BUNDLE_VERSION stays 1 and a bundle with no dated rule is the old shape plus `rev`."""

    def dated_rulebook(self):
        rb = copy.deepcopy(RULEBOOK)
        old, new = rb['cards'][0], rb['cards'][1]
        new.update(effective='2026-11-15', replaces=old['id'])
        old.update(replaced_by=new['id'], until='2026-11-15', effective='2020-01-01')
        rb['changes'] = [{'date': f'2026-{m:02d}-01', 'items': [{'text': f'entry {m}', 'cards': [new['id']]}]} for m in range(12, 0, -1)]
        rb['changes'].append({'date': '2025-06-01', 'items': [{'text': 'too old', 'cards': []}]})
        return rb

    def test_the_version_is_unchanged(self):
        self.assertEqual(BUNDLE_VERSION, 1)
        self.assertEqual(BUILT['version'], 1)

    def test_cards_without_a_dated_rule_carry_none_of_the_new_fields(self):
        dated = {c['id'] for c in RULEBOOK['cards'] if c.get('effective') or c.get('replaced_by') or c.get('until') or c.get('replaces')}
        for card in BUILT['cards']:
            if card['id'] in dated:
                continue
            for field in ('effective', 'replaced_by', 'until', 'replaces'):
                self.assertNotIn(field, card, card['id'])

    def test_dated_cards_ship_effective_replaced_by_and_until_but_not_replaces(self):
        built = bundle.build(self.dated_rulebook())['cards']
        old, new = built[0], built[1]
        self.assertEqual((old['replaced_by'], old['until'], old['effective']), (new['id'], '2026-11-15', '2020-01-01'))
        self.assertEqual(new['effective'], '2026-11-15')
        self.assertNotIn('replaced_by', new)
        self.assertNotIn('replaces', new)

    def test_the_bundle_carries_the_newest_12_change_entries_newest_first(self):
        shipped = bundle.build(self.dated_rulebook())['changes']
        self.assertEqual(len(shipped), 12)
        self.assertEqual(shipped[0]['date'], '2026-12-01')
        self.assertEqual(shipped[-1]['date'], '2026-01-01')
        self.assertNotIn('too old', bundle.render(bundle.build(self.dated_rulebook())))

    def test_no_log_means_an_empty_list_not_a_missing_key(self):
        rb = copy.deepcopy(RULEBOOK)
        rb.pop('changes', None)
        self.assertEqual(bundle.build(rb)['changes'], [])
        rb['changes'] = []
        self.assertEqual(bundle.build(rb)['changes'], [])


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

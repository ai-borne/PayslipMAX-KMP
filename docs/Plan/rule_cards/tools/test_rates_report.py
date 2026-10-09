"""Tests for rates_report.py, the 8th CPC sweep list: every current card that could carry a rate, plus every shipped figure.

Why they matter: when a pay commission changes rates, a card that quotes a number and is missing from this list is a wrong
figure nobody re-checks. The list errs on the side of too many cards, never too few. Run: python3 tools/test_rates_report.py
"""
import copy
import json
import os
import unittest

import rates_report
from config import CARDS_DIR

with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
    RULEBOOK = json.load(fh)
with open(os.path.join(CARDS_DIR, 'figures.json'), encoding='utf-8') as fh:
    FIGURES = json.load(fh)


def card(**fields):
    base = {'id': 'RB-x', 'title': 'A title', 'answer': 'An answer', 'key': ['a point'], 'attach': [], 'watch': [], 'cite': '',
            'details': '', 'chips': []}
    return {**base, **fields}


class Flags(unittest.TestCase):
    def test_a_plain_card_is_not_listed(self):
        self.assertEqual(rates_report.flags(card()), [])

    def test_the_rates_chip_lists_a_card_even_with_no_number_in_its_text(self):
        self.assertEqual(rates_report.flags(card(chips=['RATES'])), ['RATES chip'])

    def test_a_digit_in_any_body_field_lists_the_card(self):
        for field, value in (('title', 'Within 30 days?'), ('answer', 'Within 30 days'), ('key', ['Up to 2 children']),
                             ('attach', ['Form 3']), ('watch', ['Claim in 6 months']), ('details', 'After 5 years')):
            self.assertIn('digit', rates_report.flags(card(**{field: value})), field)

    def test_a_rule_number_in_the_cite_alone_is_not_a_rate(self):
        # why: nearly every cite is "Rule 114 TR"; counting those would list the whole book and bury the real rates.
        self.assertEqual(rates_report.flags(card(cite='Rule 114 TR')), [])

    def test_percent_and_rupee_signs_count_in_the_cite_too(self):
        self.assertIn('%', rates_report.flags(card(cite='MoD letter, 25% rise')))
        self.assertIn('Rs', rates_report.flags(card(cite='MoF letter, Rs. fixed')))
        self.assertIn('Rs', rates_report.flags(card(answer='Pay ₹ per day')))

    def test_rs_is_a_word_not_a_substring(self):
        self.assertNotIn('Rs', rates_report.flags(card(answer='Parents and Hours')))

    def test_every_reason_is_reported_once(self):
        self.assertEqual(rates_report.flags(card(chips=['RATES'], answer='Rs 100 is 5%')), ['RATES chip', 'digit', '%', 'Rs'])


class Report(unittest.TestCase):
    def test_every_rates_chip_card_is_in_the_real_report(self):
        listed = {r['id'] for r in rates_report.report(RULEBOOK, FIGURES)['cards']}
        self.assertTrue({c['id'] for c in RULEBOOK['cards'] if 'RATES' in c['chips']} <= listed)

    def test_every_figure_is_in_the_real_report_with_its_card(self):
        rows = rates_report.report(RULEBOOK, FIGURES)['figures']
        self.assertEqual({r['key'] for r in rows}, set(FIGURES['figures']))
        self.assertEqual({r['key']: r['card'] for r in rows}, {k: f['card'] for k, f in FIGURES['figures'].items()})

    def test_a_card_with_a_figure_is_listed_even_if_its_text_has_no_number(self):
        rb = copy.deepcopy(RULEBOOK)
        key, fig = next(iter(FIGURES['figures'].items()))
        target = next(c for c in rb['cards'] if c['id'] == fig['card'])
        target.update(chips=[], answer='No numbers', key=['none'], attach=[], watch=[], details='', cite='', title='Plain')
        listed = {r['id']: r for r in rates_report.report(rb, FIGURES)['cards']}
        self.assertIn('figure', listed[fig['card']]['flags'], key)

    def test_a_replaced_card_is_not_on_the_sweep_list(self):
        rb = copy.deepcopy(RULEBOOK)
        old = next(c for c in rb['cards'] if 'RATES' in c['chips'])
        old['replaced_by'] = 'RB-new'
        self.assertNotIn(old['id'], {r['id'] for r in rates_report.report(rb, FIGURES)['cards']})

    def test_rows_follow_the_cards_in_the_book(self):
        ids = [r['id'] for r in rates_report.report(RULEBOOK, FIGURES)['cards']]
        book = [c['id'] for c in RULEBOOK['cards']]
        self.assertEqual(ids, [i for i in book if i in set(ids)])

    def test_the_text_rendering_names_every_listed_card_and_figure(self):
        rep = rates_report.report(RULEBOOK, FIGURES)
        text = rates_report.render_text(rep)
        for row in rep['cards']:
            self.assertIn(row['id'], text)
        for row in rep['figures']:
            self.assertIn(row['key'], text)


if __name__ == '__main__':
    unittest.main()

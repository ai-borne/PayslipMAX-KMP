"""Tests for the compiler's GUIDANCE rule (owner decision 2026-10-07): a card is guidance exactly when it has
no cite, so the app's "No official source" chip and the data can never disagree. Run: python3 tools/test_compile.py"""
import unittest

import compile as comp

TOPICS = {'RR-TD-01': {}}
SOURCES = {'SS-T001'}


def card(cite, chips=''):
    return {'attrs': {'topic': 'RR-TD-01', 'from': 'SS-T001', 'chips': chips}, 'T': 'A title', 'A': 'An answer',
            'key': ['A key point'], 'attach': [], 'watch': [], 'C': cite, 'D': '', 'open': [], 'where': 'x.txt:1'}


def errors_for(c):
    errs, warns = [], []
    comp.validate(c, TOPICS, SOURCES, errs, warns)
    return errs


class GuidanceRule(unittest.TestCase):
    def test_cited_card_without_guidance_is_clean(self):
        self.assertEqual(errors_for(card('Rule 114 TR')), [])

    def test_uncited_guidance_card_is_clean(self):
        self.assertEqual(errors_for(card('', 'GUIDANCE')), [])

    def test_missing_cite_needs_guidance(self):
        self.assertTrue(any('no C: cite' in e for e in errors_for(card(''))))

    def test_guidance_with_a_cite_is_an_error(self):
        self.assertTrue(any('GUIDANCE card has a cite' in e for e in errors_for(card('Rule 114 TR', 'GUIDANCE'))))


class Freshness(unittest.TestCase):
    # why: bundle --check only compares rulebook.json to the bundle; without this an authoring edit that was never
    # compiled would pass CI and ship the old text.
    def test_only_the_generation_date_is_ignored(self):
        a = {'generated': '2026-01-01', 'cards': [{'id': 'RB-a', 'title': 'x'}]}
        self.assertTrue(comp.same_content(a, dict(a, generated='2026-02-02')))
        self.assertFalse(comp.same_content(a, {'generated': '2026-01-01', 'cards': [{'id': 'RB-a', 'title': 'y'}]}))


if __name__ == '__main__':
    unittest.main()

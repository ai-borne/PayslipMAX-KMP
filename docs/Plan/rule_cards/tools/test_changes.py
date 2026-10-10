"""Tests for changes.py: rule replacement (`effective=`, `replaces=`) and the dated change log (authoring/changes.txt).

Why they matter: when the 8th CPC changes a rate the old card must stay in the data (arrears are worked out under the old
rule) but stop being shown as current, and the user-facing "what changed" list is written by hand, so a typo in a card id
or a date would ship a dead link or a wrong order. Run: python3 tools/test_changes.py
"""
import os
import unittest

import changes

IDS = {'RB-a', 'RB-b', 'RB-c', 'RB-d'}


def card(cid, effective='', replaces=''):
    c = {'id': cid}
    if effective:
        c['effective'] = effective
    if replaces:
        c['replaces'] = replaces
    return c


def replace(*cards):
    errs = []
    cards = [dict(c) for c in cards]
    changes.apply_replacements(cards, errs)
    return {c['id']: c for c in cards}, errs


class Replacement(unittest.TestCase):
    def test_the_old_card_keeps_its_data_and_gains_replaced_by_and_until(self):
        out, errs = replace(card('RB-a', '2020-01-01'), card('RB-b', '2026-11-15', 'RB-a'))
        self.assertEqual(errs, [])
        self.assertEqual((out['RB-a']['replaced_by'], out['RB-a']['until']), ('RB-b', '2026-11-15'))
        self.assertEqual(out['RB-a']['effective'], '2020-01-01')
        self.assertNotIn('replaced_by', out['RB-b'])

    def test_cards_that_replace_nothing_are_untouched(self):
        out, errs = replace(card('RB-a'), card('RB-b'))
        self.assertEqual((errs, out['RB-a'], out['RB-b']), ([], {'id': 'RB-a'}, {'id': 'RB-b'}))

    def test_replacing_a_card_that_does_not_exist_is_an_error(self):
        self.assertTrue(any('RB-zzz' in e and 'no such card' in e for e in replace(card('RB-b', '2026-11-15', 'RB-zzz'))[1]))

    def test_a_replacement_needs_an_effective_date_because_that_is_the_until_date(self):
        self.assertTrue(any('effective' in e for e in replace(card('RB-a'), card('RB-b', replaces='RB-a'))[1]))

    def test_the_new_rule_must_start_after_the_old_one(self):
        for new in ('2020-01-01', '2019-01-01'):
            errs = replace(card('RB-a', '2020-01-01'), card('RB-b', new, 'RB-a'))[1]
            self.assertTrue(any('order' in e for e in errs), new)

    def test_an_old_card_with_no_date_is_fine(self):
        self.assertEqual(replace(card('RB-a'), card('RB-b', '2026-11-15', 'RB-a'))[1], [])

    def test_a_card_cannot_replace_itself(self):
        self.assertTrue(any('cycle' in e for e in replace(card('RB-a', '2026-11-15', 'RB-a'))[1]))

    def test_a_loop_is_an_error(self):
        errs = replace(card('RB-a', '2026-01-01', 'RB-b'), card('RB-b', '2026-02-01', 'RB-a'))[1]
        self.assertTrue(any('cycle' in e for e in errs), errs)

    def test_a_longer_loop_is_found_too(self):
        cards = [card('RB-a', '2026-01-01', 'RB-c'), card('RB-b', '2026-02-01', 'RB-a'), card('RB-c', '2026-03-01', 'RB-b')]
        self.assertTrue(any('cycle' in e for e in replace(*cards)[1]))

    def test_a_chain_of_three_is_allowed_and_each_link_is_replaced(self):
        out, errs = replace(card('RB-a', '2015-01-01'), card('RB-b', '2020-01-01', 'RB-a'), card('RB-c', '2026-11-15', 'RB-b'))
        self.assertEqual(errs, [])
        self.assertEqual((out['RB-a']['replaced_by'], out['RB-b']['replaced_by']), ('RB-b', 'RB-c'))
        self.assertEqual(out['RB-b']['until'], '2026-11-15')

    def test_two_cards_cannot_replace_the_same_card(self):
        errs = replace(card('RB-a'), card('RB-b', '2026-01-01', 'RB-a'), card('RB-c', '2026-02-01', 'RB-a'))[1]
        self.assertTrue(any('already replaced' in e for e in errs), errs)

    def test_a_malformed_replaces_id_is_an_error_not_a_silent_no_op(self):
        self.assertTrue(replace(card('RB-b', '2026-11-15', 'not-an-id'))[1])


class ReplacementLeavesTheTiles(unittest.TestCase):
    def test_a_replaced_real_card_is_off_the_tiles_and_the_nav_check_stays_clean(self):
        # why: the compiler runs apply_replacements then build_nav; this proves the two agree end to end on the real tree.
        import json
        import nav
        from config import CARDS_DIR
        with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
            cards = json.load(fh)['cards']
        # two cards no rule change touches yet, so the test holds once the real data carries a replaced pair
        plain = [c for c in cards if not (c.get('replaced_by') or c.get('replaces') or c.get('effective'))]
        old, new = plain[0], plain[1]
        new.update(replaces=old['id'], effective='2026-11-15')
        errs = []
        changes.apply_replacements(cards, errs)
        tree, homes = nav.build_nav(cards, nav.load_nav(CARDS_DIR), errs)
        self.assertEqual(errs, [])
        self.assertEqual((old['replaced_by'], old['until']), (new['id'], '2026-11-15'))
        self.assertNotIn(old['id'], homes)
        self.assertIn(new['id'], homes)
        self.assertNotIn(old['id'], [i for a in tree for c in a['cases'] for i in c['cards']])


def parse(text, ids=IDS):
    errs = []
    return changes.parse_log(text.splitlines(), ids, errs, 'changes.txt'), errs


class ChangeLog(unittest.TestCase):
    TEXT = ('# comment\n== 2026-11-15\n- Food rate raised [RB-a, RB-b]\n- DA step noted\n\n== 2027-01-01\n- Later entry [RB-c]\n')

    def test_entries_are_newest_first_with_text_and_cards(self):
        entries, errs = parse(self.TEXT)
        self.assertEqual(errs, [])
        self.assertEqual([e['date'] for e in entries], ['2027-01-01', '2026-11-15'])
        self.assertEqual(entries[1]['items'], [{'text': 'Food rate raised', 'cards': ['RB-a', 'RB-b']}, {'text': 'DA step noted', 'cards': []}])

    def test_an_unknown_card_id_is_an_error(self):
        errs = parse('== 2026-11-15\n- Something [RB-nope]\n')[1]
        self.assertTrue(any('RB-nope' in e and 'changes.txt:2' in e for e in errs), errs)

    def test_a_bad_date_is_an_error(self):
        for bad in ('2026-13-01', '15-11-2026', '2026-11-5', 'soon'):
            self.assertTrue(parse(f'== {bad}\n- x\n')[1], bad)

    def test_a_rupee_figure_in_the_text_is_an_error_because_free_users_read_it(self):
        for text in ('Food rate is now Rs 5,000', 'Food rate is now Rs. 5000', 'Food rate is now \u20b95,000', 'Raised to rs 1200 a day'):
            errs = parse(f'== 2026-11-15\n- {text}\n')[1]
            self.assertTrue(any('rupee figure' in e and 'changes.txt:2' in e for e in errs), (text, errs))

    def test_words_that_only_look_like_figures_are_fine(self):
        for text in ('Food rate raised for the 8th CPC', 'Rule 177A wording made clearer', 'HRA classes reviewed (Rs is not followed by a number)', 'Parsed rows and cursors'):
            self.assertEqual(parse(f'== 2026-11-15\n- {text}\n')[1], [], text)

    def test_a_duplicate_date_is_an_error(self):
        self.assertTrue(any('twice' in e for e in parse('== 2026-11-15\n- a\n== 2026-11-15\n- b\n')[1]))

    def test_an_item_before_any_date_is_an_error(self):
        self.assertTrue(parse('- orphan item\n')[1])

    def test_an_entry_with_no_items_is_an_error(self):
        self.assertTrue(any('no items' in e for e in parse('== 2026-11-15\n')[1]))

    def test_an_empty_item_text_is_an_error(self):
        self.assertTrue(parse('== 2026-11-15\n- [RB-a]\n')[1])

    def test_an_unrecognised_line_is_an_error(self):
        self.assertTrue(parse('== 2026-11-15\nfree text\n')[1])

    def test_an_empty_file_is_an_empty_log(self):
        self.assertEqual(parse('# nothing yet\n'), ([], []))

    def test_latest_keeps_the_newest_n(self):
        entries = [{'date': f'2026-{m:02d}-01', 'items': []} for m in range(12, 0, -1)] + [{'date': '2025-01-01', 'items': []}]
        self.assertEqual(len(changes.latest(entries)), 12)
        self.assertEqual(changes.latest(entries, 2), entries[:2])

    def test_the_committed_log_is_valid_against_the_real_cards(self):
        import json
        from config import AUTH_DIR, CARDS_DIR
        with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
            ids = {c['id'] for c in json.load(fh)['cards']}
        errs = []
        changes.load_log(os.path.join(AUTH_DIR, 'changes.txt'), ids, errs)
        self.assertEqual(errs, [])

    def test_a_missing_file_is_an_empty_log(self):
        self.assertEqual(changes.load_log('/no/such/changes.txt', IDS, []), [])


if __name__ == '__main__':
    unittest.main()

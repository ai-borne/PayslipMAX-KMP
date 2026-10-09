"""Tests for ids.py, the guard that keeps shipped card IDs stable.

Why they matter: pins (GuidePins), later personal notes and the expert's replies all refer to a card by ID.
An ID that silently disappears or changes (e.g. a card's `from=` list is reordered) orphans them with no error.
Run: python3 tools/test_ids.py
"""
import json
import os
import unittest

import compile as comp
import ids
from config import CARDS_DIR, LOCK_PATH


class RemovalRule(unittest.TestCase):
    def test_unchanged_ids_are_clean(self):
        self.assertEqual(ids.check({'RB-a', 'RB-b'}, {'RB-a', 'RB-b'}, {}), [])

    def test_a_vanished_id_is_an_error(self):
        errs = ids.check({'RB-a'}, {'RB-a', 'RB-b'}, {})
        self.assertEqual(len(errs), 1)
        self.assertIn('RB-b', errs[0])

    def test_a_renamed_id_is_caught_as_a_removal(self):
        # reordering from= changes the derived ID: the old one disappears and must be reported
        self.assertTrue(ids.check({'RB-SS-T002'}, {'RB-SS-T001'}, {}))

    def test_a_retired_id_may_disappear(self):
        self.assertEqual(ids.check({'RB-a'}, {'RB-a', 'RB-b'}, {'RB-b': 'merged into RB-a'}), [])

    def test_retiring_an_id_that_is_still_a_card_is_an_error(self):
        self.assertTrue(ids.check({'RB-a', 'RB-b'}, {'RB-a', 'RB-b'}, {'RB-b': 'x'}))

    def test_retiring_an_id_that_never_shipped_is_an_error(self):
        self.assertTrue(ids.check({'RB-a'}, {'RB-a'}, {'RB-zzz': 'typo'}))

    def test_a_retired_id_cannot_be_reused_later(self):
        self.assertTrue(ids.check({'RB-a', 'RB-b'}, {'RB-a', 'RB-b'}, {'RB-b': 'x'}))


class LockMaintenance(unittest.TestCase):
    def test_new_ids_are_reported_stale_not_silently_accepted(self):
        self.assertEqual(ids.stale({'RB-a', 'RB-c'}, {'RB-a'}), ['RB-c'])

    def test_update_adds_new_and_keeps_retired(self):
        self.assertEqual(ids.updated({'RB-a', 'RB-c'}, {'RB-a', 'RB-b'}), ['RB-a', 'RB-b', 'RB-c'])

    def test_render_is_sorted_and_deterministic(self):
        self.assertEqual(ids.render(['RB-b', 'RB-a']), ids.render(['RB-a', 'RB-b']))
        self.assertEqual(json.loads(ids.render(['RB-b', 'RB-a'])), ['RB-a', 'RB-b'])


class AuthoringSyntax(unittest.TestCase):
    def test_retire_line_is_parsed_with_its_reason(self):
        path = os.path.join(os.environ.get('TMPDIR', '/tmp'), 'retire_syntax.txt')
        with open(path, 'w', encoding='utf-8') as fh:
            fh.write('--- retire RB-SS-T999 merged into RB-SS-T001\n')
        errors = []
        cards, skips, retires = comp.parse(path, errors)
        os.remove(path)
        self.assertEqual((cards, skips, errors), ([], [], []))
        self.assertEqual(retires, {'RB-SS-T999': 'merged into RB-SS-T001'})

    def test_retire_without_reason_is_an_error(self):
        path = os.path.join(os.environ.get('TMPDIR', '/tmp'), 'retire_bad.txt')
        with open(path, 'w', encoding='utf-8') as fh:
            fh.write('--- retire RB-SS-T999\n')
        errors = []
        comp.parse(path, errors)
        os.remove(path)
        self.assertTrue(any('retire' in e for e in errors))


class CommittedLock(unittest.TestCase):
    def test_every_shipped_card_is_in_the_lock(self):
        with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
            shipped = {c['id'] for c in json.load(fh)['cards']}
        self.assertEqual(ids.stale(shipped, ids.load_lock(LOCK_PATH)), [], 'run tools/refresh.py to extend ids_lock.json')
        self.assertEqual(ids.check(shipped, ids.load_lock(LOCK_PATH), {}), [])


if __name__ == '__main__':
    unittest.main()

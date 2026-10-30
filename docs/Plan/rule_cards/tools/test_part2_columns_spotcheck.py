"""Tests for part2_columns_spotcheck.py: the owner's 25-row check of columns.json (plan 13, owner decision 11).

Why they matter: the table is only trusted after the owner has read 25 rows against the printed pages. If the draw could be steered, or
repeated differently on another day, or skipped the oddest rows, the check would prove less than it claims.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_columns_spotcheck.py'
"""
import json
import os
import unittest

import part2_columns_spotcheck as sc
from config import CARDS_DIR

COLUMNS = json.load(open(os.path.join(CARDS_DIR, 'part2', 'columns.json'), encoding='utf-8'))


class DrawTest(unittest.TestCase):
    def setUp(self):
        self.draw = sc.draw(COLUMNS, sc.SEED)

    def test_twenty_five_distinct_rows_ten_from_the_owners_pages_and_fifteen_from_the_rest(self):
        self.assertEqual(len(self.draw['owner_pages']), 10)
        self.assertEqual(len(self.draw['rest']), 15)
        self.assertEqual(len(set(self.draw['owner_pages']) | set(self.draw['rest'])), 25)

    def test_every_pick_comes_from_its_own_pool(self):
        self.assertTrue(set(self.draw['owner_pages']) <= sc.OWNER_PAGES)
        self.assertTrue(set(self.draw['rest']).isdisjoint(sc.OWNER_PAGES | sc.SIBLINGS))

    def test_the_oddest_rows_are_always_in(self):
        picked = set(self.draw['owner_pages']) | set(self.draw['rest'])
        odd = set(sc.oddest(COLUMNS))
        self.assertTrue({'13.31', '13.32', '3.53', '5.27', '3.23'} <= odd)
        self.assertTrue(odd - sc.SIBLINGS <= picked, sorted(odd - picked))

    def test_the_same_seed_gives_the_same_draw_and_another_seed_a_different_one(self):
        self.assertEqual(sc.draw(COLUMNS, sc.SEED), self.draw)
        self.assertNotEqual(sc.draw(COLUMNS, sc.SEED + 1), self.draw)

    def test_the_owners_thirty_pages_are_thirty(self):
        self.assertEqual(len(sc.OWNER_PAGES), 30)
        serials = {r['serial'] for r in COLUMNS['rows']}
        self.assertTrue((sc.OWNER_PAGES | sc.SIBLINGS) <= serials)
        self.assertEqual(len(sc.OWNER_PAGES | sc.SIBLINGS), 48, 'the 30 pages cover 48 serials (plan 13, owner decision 8)')


if __name__ == '__main__':
    unittest.main()

"""Tests for part2/related.json and part2_related_check.py: "Usually published with" links between casualties (Orders Bank, phase 1).

Why they matter: a link tells the officer that another entry normally comes with this one. A link to a code that does not exist, a
manual link with no page to check it by, or a practice link dressed as the manual's rule would mislead him about what the manual requires.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_related.py'
"""
import json
import os
import unittest

import part2_related_check as chk
from config import CARDS_DIR

CODES = json.load(open(os.path.join(CARDS_DIR, 'part2', 'codes.json'), encoding='utf-8'))
PATH = os.path.join(CARDS_DIR, 'part2', 'related.json')


def good():
    return {'version': 1, 'related': [{'code': 'GTDAL', 'with': [{'code': 'SORS', 'basis': 'manual', 'page': 97},
                                                                   {'code': 'STPRH', 'basis': 'practice'}]}]}


class RefusalTest(unittest.TestCase):
    def refused(self, edit, *fragments):
        data = good()
        edit(data['related'][0])
        found = chk.problems(data, CODES)
        self.assertTrue(any(all(f in line for f in fragments) for line in found), f'wanted {fragments} in {found}')

    def test_a_good_file_passes(self):
        self.assertEqual(chk.problems(good(), CODES), [])

    def test_unknown_source_code(self):
        self.refused(lambda r: r.update(code='NOPE'), 'NOPE', 'codes.json')

    def test_unknown_target_code(self):
        self.refused(lambda r: r['with'][0].update(code='NOPE'), 'NOPE', 'codes.json')

    def test_a_manual_link_needs_a_page(self):
        self.refused(lambda r: r['with'][0].pop('page'), 'manual', 'page')

    def test_a_practice_link_carries_no_page(self):
        self.refused(lambda r: r['with'][1].update(page=5), 'practice', 'page')

    def test_unknown_basis(self):
        self.refused(lambda r: r['with'][0].update(basis='rumour'), 'basis')

    def test_a_page_outside_the_manual(self):
        self.refused(lambda r: r['with'][0].update(page=9999), 'page')

    def test_a_link_to_itself(self):
        self.refused(lambda r: r['with'][0].update(code='GTDAL'), 'itself')

    def test_a_duplicate_target(self):
        self.refused(lambda r: r['with'].append(dict(r['with'][0])), 'twice')

    def test_an_empty_list(self):
        self.refused(lambda r: r.update({'with': []}), 'empty')

    def test_the_same_source_twice(self):
        data = good()
        data['related'].append(good()['related'][0])
        self.assertTrue(any('twice' in p for p in chk.problems(data, CODES)))

    def test_an_unknown_version(self):
        data = good()
        data['version'] = 2
        self.assertTrue(any('version' in p for p in chk.problems(data, CODES)))


class CommittedFileTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(os.path.exists(PATH), 'part2/related.json is missing (a missing file must fail, never skip)')
        self.data = json.load(open(PATH, encoding='utf-8'))

    def test_the_file_passes_every_check(self):
        self.assertEqual(chk.problems(self.data, CODES), [])

    def test_only_casualties_that_have_data_are_listed(self):
        # why: the plan says "Usually published with" only where data exists (the casualties the old work gave links to), never guessed.
        # Only the 15 leave casualties had links, not the "about 60" the plan estimated; the test pins what is true.
        self.assertEqual(len(self.data['related']), 15)

    def test_the_file_is_exactly_what_render_writes(self):
        with open(PATH, encoding='utf-8') as fh:
            self.assertEqual(fh.read(), chk.render(self.data))


if __name__ == '__main__':
    unittest.main()

"""Tests for part2/about.json and part2_about_check.py: the 40 plain-English casualty pages (30 + 10 hospital) (Part II Orders Bank, phase 1).

Why they matter: these 30 pages are the only text the app writes itself about an order; every other word is the manual's. A page that
quotes a rate, carries markup, runs long, or covers the wrong serials would be shipped to every officer. Each rule is broken on
purpose below. Whether the words are TRUE is not testable here; the owner reads the pages once and the sources sit in a local sheet.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_about.py'
"""
import copy
import json
import os
import unittest

import part2_about_check as chk
from config import CARDS_DIR
from part2_columns_spotcheck import OWNER_PAGES, SIBLINGS

CODES = json.load(open(os.path.join(CARDS_DIR, 'part2', 'codes.json'), encoding='utf-8'))
PATH = os.path.join(CARDS_DIR, 'part2', 'about.json')


def good():
    page = {'id': '5.1', 'serials': ['5.1'], 'what': 'What it is.', 'when': 'When you see it.', 'ready': 'What to keep ready.',
            'watch': 'One thing to watch.'}
    return {'version': 1, 'pages': [page]}


class RefusalTest(unittest.TestCase):
    def refused(self, edit, *fragments):
        data = good()
        edit(data['pages'][0])
        found = chk.problems(data, CODES, whole=False)
        self.assertTrue(any(all(f in line for f in fragments) for line in found), f'wanted {fragments} in {found}')

    def test_a_good_page_passes(self):
        self.assertEqual(chk.problems(good(), CODES, whole=False), [])

    def test_unknown_serial(self):
        self.refused(lambda p: p.update(serials=['9.99']), '9.99', 'codes.json')

    def test_id_must_be_the_first_serial(self):
        self.refused(lambda p: p.update(id='5.2'), 'id')

    def test_a_missing_part(self):
        self.refused(lambda p: p.pop('watch'), 'watch')

    def test_an_empty_part(self):
        self.refused(lambda p: p.update(what='  '), 'what')

    def test_angle_brackets(self):
        self.refused(lambda p: p.update(when='see <b>this</b>'), 'angle')

    def test_a_link(self):
        self.refused(lambda p: p.update(ready='go to https://x.example'), 'link')

    def test_an_amount_or_rate(self):
        for text in ('about Rs 500 a month', 'pays ₹ 500', 'at 25% of pay'):
            self.refused(lambda p, t=text: p.update(watch=t), 'amount')

    def test_a_part_that_is_too_long(self):
        self.refused(lambda p: p.update(what='word ' * 100), 'what', 'over')

    def test_a_page_that_is_too_long(self):
        self.refused(lambda p: p.update(what='w ' * 40, when='w ' * 40, ready='w ' * 40, watch='w ' * 40), 'words')

    def test_control_characters(self):
        self.refused(lambda p: p.update(what='a\x00b'), 'control')

    def test_the_same_serial_on_two_pages(self):
        data = good()
        data['pages'].append(copy.deepcopy(data['pages'][0]))
        self.assertTrue(any('twice' in p for p in chk.problems(data, CODES, whole=False)))

    def test_an_unknown_key_is_refused(self):
        self.refused(lambda p: p.update(source='ODP p.114'), 'key')

    def test_an_unknown_version(self):
        data = good()
        data['version'] = 2
        self.assertTrue(any('version' in p for p in chk.problems(data, CODES, whole=False)))


class CommittedFileTest(unittest.TestCase):
    def setUp(self):
        self.assertTrue(os.path.exists(PATH), 'part2/about.json is missing (a missing file must fail, never skip)')
        self.data = json.load(open(PATH, encoding='utf-8'))

    def test_the_file_passes_every_check(self):
        self.assertEqual(chk.problems(self.data, CODES), [])

    def test_exactly_the_owners_30_pages_and_the_10_hospital_pages_covering_62_serials(self):
        self.assertEqual({p['id'] for p in self.data['pages']}, set(OWNER_PAGES) | chk.HOSPITAL_PAGES)
        self.assertEqual(len(self.data['pages']), 40)
        covered = {s for p in self.data['pages'] for s in p['serials']}
        self.assertEqual(covered, set(OWNER_PAGES) | set(SIBLINGS) | chk.HOSPITAL_SERIALS)
        self.assertEqual(len(covered), 62)

    def test_the_hospital_pages_cover_chapter_6_serials_6_1_to_6_14_once_each(self):
        # why: the hospital-stay cards name these codes; an officer who taps one must land on a page, and no serial may sit on two.
        hospital = [s for p in self.data['pages'] if p['id'].startswith('6.') for s in p['serials']]
        self.assertEqual(sorted(hospital, key=lambda s: int(s.split('.')[1])), [f'6.{n}' for n in range(1, 15)])

    def test_the_hospital_pages_warn_that_unregularised_days_can_cost_pay(self):
        # why: this is the reason the pages exist; every hospital page says it where the officer looks.
        by_id = {p['id']: p for p in self.data['pages']}
        for pid in sorted(chk.HOSPITAL_PAGES):
            self.assertIn('recover', by_id[pid]['watch'].lower(), pid)

    def test_the_hospital_pages_name_no_source_letter(self):
        # why: owner decision 2026-11-06 - cite ODP 2026 only; the app never names the source letter.
        for p in self.data['pages']:
            if p['id'].startswith('6.'):
                self.assertNotRegex(json.dumps(p), r'B/25451|ADG PS|AG.s Branch|Nov 2025|letter', p['id'])

    def test_the_shared_pages_cover_their_siblings(self):
        by_id = {p['id']: p['serials'] for p in self.data['pages']}
        self.assertEqual(by_id['3.1'], [f'3.{n}' for n in range(1, 12)])
        self.assertEqual(by_id['3.55'], ['3.55', '3.56', '3.57'])
        self.assertEqual(by_id['8.2'], ['8.2', '8.3', '8.4', '8.5'])
        self.assertEqual(by_id['8.6'], ['8.6', '8.7', '8.8', '8.9'])

    def test_about_never_repeats_the_codes_json_name(self):
        # why: the manual's name lives in codes.json only (one source of truth); a copy here would drift.
        names = {s['name'].lower() for s in CODES['serials']}
        for page in self.data['pages']:
            for part in ('what', 'when', 'ready', 'watch'):
                self.assertNotIn(page[part].lower(), names, page['id'])

    def test_the_file_is_exactly_what_render_writes(self):
        with open(PATH, encoding='utf-8') as fh:
            self.assertEqual(fh.read(), chk.render(self.data))


if __name__ == '__main__':
    unittest.main()

"""Tests for part2_casualty.py: the casualty slice (columns, about, related) of the bundle's `part2` section.

Why they matter: this is the one door between the authored files and the APK. It must ship only what the app reads, never the internal
`hand` audit note or a duplicate of codes.json's codes and pages, and it must stop the build when any of the three files is damaged.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_casualty.py'
"""
import copy
import unittest

import part2_about_check
import part2_codes_check
import part2_columns_check
import part2_related_check
import part2_casualty

CODES = part2_codes_check.load()
COLUMNS = part2_columns_check.load()
ABOUT = part2_about_check.load()
RELATED = part2_related_check.load()


class ForBundleTest(unittest.TestCase):
    def setUp(self):
        self.section = part2_casualty.for_bundle(COLUMNS, ABOUT, RELATED, CODES)

    def test_303_rows_30_pages_15_related_casualties(self):
        self.assertEqual((len(self.section['columns']), len(self.section['about']), len(self.section['related'])), (303, 30, 15))

    def test_a_column_row_ships_only_what_the_app_reads(self):
        # why: codes and page already ship once, in the serial list; a second copy could drift from it.
        self.assertEqual(set(self.section['columns'][0]), {'serial', 'from', 'to', 'data'})
        self.assertTrue(all('hand' not in row for row in self.section['columns']))

    def test_authored_order_is_kept(self):
        self.assertEqual([r['serial'] for r in self.section['columns']], [s['serial'] for s in CODES['serials']])
        self.assertEqual([p['id'] for p in self.section['about']], [p['id'] for p in ABOUT['pages']])

    def test_the_caps_the_validator_reads_come_from_the_checkers(self):
        self.assertEqual(part2_casualty.limits(), {'caption': part2_columns_check.CAPTION_MAX, 'about_part': part2_about_check.PART_MAX})

    def test_a_broken_file_stops_the_build(self):
        bad = copy.deepcopy(COLUMNS)
        bad['rows'][0]['from']['need'] = 'maybe'
        with self.assertRaises(SystemExit):
            part2_casualty.for_bundle(bad, ABOUT, RELATED, CODES)
        bad_about = copy.deepcopy(ABOUT)
        bad_about['pages'][0]['what'] = '<b>'
        with self.assertRaises(SystemExit):
            part2_casualty.for_bundle(COLUMNS, bad_about, RELATED, CODES)
        bad_related = copy.deepcopy(RELATED)
        bad_related['related'][0]['with'][0]['code'] = 'NOPE'
        with self.assertRaises(SystemExit):
            part2_casualty.for_bundle(COLUMNS, ABOUT, bad_related, CODES)

    def test_check_is_clean_on_the_committed_files(self):
        self.assertEqual(part2_casualty.check(), [])


if __name__ == '__main__':
    unittest.main()

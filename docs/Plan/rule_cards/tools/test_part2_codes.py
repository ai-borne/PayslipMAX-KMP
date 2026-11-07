"""Tests for part2_codes.py (the generator) and part2_codes_check.py (the shape checks), Part II Orders phase 0.

Why they matter: every code page, search result and ribbon tile in the Part II feature comes from codes.json. A missing serial
means a code the officer cannot look up; a wrong group sends a Group B claim into an order with A/C codes (para 7.1); a wrong page
sends the officer to the wrong place in the manual. The synthetic tests prove the extractor's rules on invented text (no manual
text is committed); the shape tests prove the committed file; the local test proves the file still matches the manual.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_codes.py'
"""
import copy
import json
import os
import unittest

import part2_codes
import part2_codes_check
from config import CARDS_DIR, SOURCES_DIR

CODES_PATH = os.path.join(CARDS_DIR, 'part2', 'codes.json')
REAL = json.load(open(CODES_PATH, encoding='utf-8')) if os.path.exists(CODES_PATH) else None

MASTER = (
    '1.1\tGTDPC\tA\tGranted Permanent Commission\n'
    '3.27\tGTDTLA1/GTDTLA2/GTDTLA3\tA\tTough Location Allce 1/2/3\n'
    '3.28\tSTPTLA\t?\tCeased TLA\n'
)


class ParseMasterTest(unittest.TestCase):
    def test_variants_split_and_blank_group_is_none(self):
        rows = part2_codes.parse_master(MASTER)
        self.assertEqual([r['serial'] for r in rows], ['1.1', '3.27', '3.28'])
        self.assertEqual(rows[1]['codes'], ['GTDTLA1', 'GTDTLA2', 'GTDTLA3'])
        self.assertIsNone(rows[2]['group'], 'a group the manual does not print is never inferred (Round 8)')

    def test_rejects_a_duplicate_code(self):
        with self.assertRaises(ValueError):
            part2_codes.parse_master('1.1\tGTDPC\tA\tx\n1.2\tGTDPC\tA\ty\n')

    def test_rejects_a_bad_code_or_group(self):
        for bad in ('1.1\tgtdpc\tA\tx\n', '1.1\tGTD PC\tA\tx\n', '1.1\tGTDPC\tZ\tx\n'):
            with self.assertRaises(ValueError, msg=bad):
                part2_codes.parse_master(bad)


INDEX = [
    'CHAPTER – 1 : COMMISSIONING',
    '      Sr No.     Nature of Occurrence                    Occurrence   Group   Page',
    '                                                            Code               No',
    '        1.1      Granted Permanent Commission             GTDPC        A',
    '                                                                              28',
    '        1.2      Granted Short Service Commission Men    GTDSSCMT      A',
    '                 Non-Technical                                                29',
    ' 1.3 Reimbursement of Transit Accommodation /           REIMACCN         B    165-',
    '                 Guest House',
]
INDEX_ROWS = [
    {'serial': '1.1', 'codes': ['GTDPC']},
    {'serial': '1.2', 'codes': ['GTDSSCMT']},
    {'serial': '1.3', 'codes': ['REIMACCN']},
]


class IndexNamesTest(unittest.TestCase):
    def setUp(self):
        self.names = part2_codes.index_names(INDEX, INDEX_ROWS)

    def test_page_number_lines_are_not_part_of_a_name(self):
        self.assertEqual(self.names['1.1'], 'Granted Permanent Commission')

    def test_a_wrapped_name_is_joined_and_a_name_starting_with_No_is_kept(self):
        self.assertEqual(self.names['1.2'], 'Granted Short Service Commission Men Non-Technical')

    def test_single_space_after_the_serial_still_matches(self):
        self.assertEqual(self.names['1.3'], 'Reimbursement of Transit Accommodation / Guest House')


class IndexNamesWrapTest(unittest.TestCase):
    """Names that broke the first version: the name holds its own code, text spills under the code column, footers, variant lists."""

    def names(self, lines, rows):
        return part2_codes.index_names(lines, rows)

    def test_a_name_that_contains_its_own_code_is_not_cut_there(self):
        lines = ['  5.10   Granted EOL without pay and allowances           EOL        A      118']
        self.assertEqual(self.names(lines, [{'serial': '5.10', 'codes': ['EOL']}])['5.10'], 'Granted EOL without pay and allowances')

    def test_a_continuation_that_runs_under_the_code_column_is_kept_whole(self):
        lines = ['  8.10   Reimbursement of Guest             REIMACCN     B',
                 '         Room / Army Guest House / Holiday Homes                 165']
        self.assertEqual(self.names(lines, [{'serial': '8.10', 'codes': ['REIMACCN']}])['8.10'],
                         'Reimbursement of Guest Room / Army Guest House / Holiday Homes')

    def test_variant_codes_and_a_page_footer_are_not_part_of_a_name(self):
        lines = ['  3.53   Granted Transport Allowance 1 (lower) /       GTDTRAN1 /       A',
                 '         2 (higher) / 3 (disability)                    GTDTRAN2 /',
                 'Joining/ Intake / Commencement of Pay & Allce']
        got = self.names(lines, [{'serial': '3.53', 'codes': ['GTDTRAN1', 'GTDTRAN2']}])['3.53']
        self.assertEqual(got, 'Granted Transport Allowance 1 (lower) / 2 (higher) / 3 (disability)')

    def test_a_code_joined_to_the_name_by_one_space_is_removed_with_its_slash(self):
        lines = ['  2.14   Proceeded on deputation to AR(Assam Rifles) DEPUOTHR  A  43',
                 '         22(Estt No. 22) NSG (NSG)']
        got = self.names(lines, [{'serial': '2.14', 'codes': ['DEPUOTHR']}])['2.14']
        self.assertEqual(got, 'Proceeded on deputation to AR(Assam Rifles) 22(Estt No. 22) NSG (NSG)')

    def test_a_short_code_that_is_also_a_word_in_the_name_is_kept(self):
        lines = ['  5.10   Granted EOL without pay         EOL     A']
        self.assertIn('EOL', self.names(lines, [{'serial': '5.10', 'codes': ['EOL']}])['5.10'])

    def test_page_ranges_and_header_lines_are_not_part_of_a_name(self):
        lines = ['  11.16  Notification of challenged child    DIVYNCHL    C',
                 '         disability                                     190-191',
                 'SrNo. Nature of Occurrence Occurrence Group Page',
                 'Srl No. Nature of Occurrence Occurrence Group Page No']
        self.assertEqual(self.names(lines, [{'serial': '11.16', 'codes': ['DIVYNCHL']}])['11.16'],
                         'Notification of challenged child disability')

    def test_a_repeated_column_heading_is_not_part_of_a_name(self):
        lines = ['  8.10   Reimbursement of Guest             REIMACCN     B',
                 'Sr         Nature of Occurrence        Occurrence    Group   Page  No']
        self.assertEqual(self.names(lines, [{'serial': '8.10', 'codes': ['REIMACCN']}])['8.10'], 'Reimbursement of Guest')

    def test_a_name_may_begin_with_Sr_like_Sr_Adviser(self):
        lines = ['  3.61   Granted Special Allowance     GTDSPLP3    A', '         Sr Adviser / Professor']
        self.assertIn('Sr Adviser', self.names(lines, [{'serial': '3.61', 'codes': ['GTDSPLP3']}])['3.61'])


DETAIL = [
    'ODP- 2026',
    '                                                                  28',
    '    1.1      GTDPC        Granted Permanent         Required as',
    '    3.27     GTDTLA1 /    Granted Tough Location    Required',
    '                                                                  29',
    '   3.28      STPTLA       Ceased to draw Tough      Required',
    '   3.1     The officer has applied for Govt accommodation',
    '                                                                  30',
    '   11.8     REMAROF      Re-married to Central/',
    '   13.1     ALOTPRN     Allotted',
]
PAGE_ROWS = [
    {'serial': '1.1', 'codes': ['GTDPC']},
    {'serial': '3.27', 'codes': ['GTDTLA1', 'GTDTLA2']},
    {'serial': '3.28', 'codes': ['STPTLA']},
    {'serial': '3.1', 'codes': ['GTDRH11']},
    {'serial': '11.8', 'codes': ['REMAROF']},
    {'serial': '13.14', 'codes': ['ALOTPRNO']},
]


class DetailPagesTest(unittest.TestCase):
    def test_page_is_the_last_page_number_seen_before_the_row(self):
        pages = part2_codes.detail_pages(DETAIL, PAGE_ROWS)
        self.assertEqual((pages['1.1'], pages['3.27'], pages['3.28'], pages['11.8']), (28, 28, 29, 30))

    def test_a_row_must_start_with_its_serial_and_code_so_a_note_numbered_3_1_is_not_a_serial(self):
        pages = part2_codes.detail_pages(DETAIL, PAGE_ROWS)
        self.assertNotIn('3.1', pages, 'the line "3.1 The officer ..." is a certificate step, not serial 3.1 GTDRH11')

    def test_a_wrapped_code_is_found_by_its_first_six_letters(self):
        self.assertEqual(part2_codes.detail_pages(DETAIL, PAGE_ROWS + [{'serial': '13.1', 'codes': ['ALOTPRNO']}])['13.1'], 30)

    def test_a_serial_cut_by_a_narrow_column_is_matched_by_its_code(self):
        lines = ['    13.1      CHNGRELI     Changed religion', '    13.1      POSIN      Taken on strength']
        rows = [{'serial': '13.10', 'codes': ['CHNGRELI']}, {'serial': '13.1', 'codes': ['POSIN']}]
        pages = part2_codes.detail_pages(lines, rows)
        self.assertEqual(sorted(pages), ['13.1', '13.10'], 'the printed "13.1" must not be taken for 13.1x, and 13.10 must still be found')

    def test_a_code_cut_to_five_letters_is_the_last_resort(self):
        pages = part2_codes.detail_pages(['    5.22     REJAW     Rejoined   from'], [{'serial': '5.22', 'codes': ['REJAWL']}])
        self.assertEqual(pages, {'5.22': 0})

    def test_a_longer_prefix_wins_so_one_grant_code_is_not_taken_for_another(self):
        lines = ['  3.1  GTDRH11  Granted', '  3.10 GTDRH77  Granted']
        rows = [{'serial': '3.1', 'codes': ['GTDRH11']}, {'serial': '3.10', 'codes': ['GTDRH77']}]
        self.assertEqual(sorted(part2_codes.detail_pages(lines, rows)), ['3.1', '3.10'])


class CrossCheckTest(unittest.TestCase):
    def test_reports_a_code_that_is_missing_and_not_known(self):
        misses = part2_codes.cross_check('GTDPC', 'GTDPC', [{'serial': '1.1', 'codes': ['GTDPC', 'GTDXXX']}])
        self.assertEqual(misses, [('1.1', 'GTDXXX', 'index'), ('1.1', 'GTDXXX', 'detail')])

    def test_a_partial_word_does_not_count_as_a_match(self):
        misses = part2_codes.cross_check('GTDPCX', 'GTDPCX', [{'serial': '1.1', 'codes': ['GTDPC']}])
        self.assertEqual(len(misses), 2)


class KnownMissesTest(unittest.TestCase):
    def test_the_list_is_the_twelve_wrapped_codes_and_nothing_loose(self):
        self.assertEqual(len(part2_codes.KNOWN_MISSES), 12)
        self.assertIn(('3.27', 'GTDTLA2', 'index'), part2_codes.KNOWN_MISSES)
        self.assertIn(('10.7', 'TADISCHG', 'detail'), part2_codes.KNOWN_MISSES)


@unittest.skipIf(REAL is None, 'part2/codes.json is not generated yet')
class CommittedFileShapeTest(unittest.TestCase):
    def test_the_committed_file_has_no_problems(self):
        self.assertEqual(part2_codes_check.problems(REAL), [])

    def test_headline_counts(self):
        rows = REAL['serials']
        self.assertEqual(len(rows), 303)
        self.assertEqual(sum(len(r['codes']) for r in rows), 321)
        self.assertEqual([sum(1 for r in rows if r['chapter'] == c) for c in range(1, 15)],
                         [19, 16, 102, 5, 30, 15, 13, 10, 20, 10, 21, 6, 34, 2])
        groups = {}
        for r in rows:
            groups[r['group']] = groups.get(r['group'], 0) + 1
        self.assertEqual(groups, {'A': 204, 'AM': 23, 'B': 11, 'C': 53, 'CM': 10, None: 2})
        self.assertEqual({r['serial'] for r in rows if r['group'] is None}, {'3.28', '11.8'})

    def test_variant_serials_and_named_facts(self):
        variants = {r['serial']: r['codes'] for r in REAL['serials'] if len(r['codes']) > 1}
        self.assertEqual(sorted(variants), ['10.9', '3.27', '3.51', '3.53', '3.67', '3.80', '3.90', '3.94'])
        self.assertEqual(variants['3.53'], ['GTDTRAN1', 'GTDTRAN2', 'GTDTRAN3', 'GTDTRAN4', 'GTDTRAN5'])
        by = {r['serial']: r for r in REAL['serials']}
        for missing_from_flawed_json in ('2.14', '2.15', '2.16', '3.25', '3.26', '3.28', '3.53', '3.67', '3.80', '3.97',
                                         '3.98', '3.99', '11.8', '13.12'):
            self.assertIn(missing_from_flawed_json, by)
        self.assertEqual((by['9.7']['codes'], by['9.7']['page']), (['WTHDPAY'], 171))
        self.assertEqual(by['5.23']['codes'], ['REJOINAL'], 'the manual says REJOINAL; the slip prints REJOIN by a template override')
        self.assertEqual((by['5.1']['codes'], by['5.1']['page'], by['5.1']['group']), (['GTDAL'], 114, 'A'))

    def test_group_meanings_are_the_five_in_para_3(self):
        self.assertEqual(sorted(REAL['groups']), ['A', 'AM', 'B', 'C', 'CM'])

    def test_the_flawed_occurrence_json_is_never_in_the_repo(self):
        root = os.path.abspath(os.path.join(CARDS_DIR, '..', '..', '..'))
        for folder, dirs, files in os.walk(root):
            dirs[:] = [d for d in dirs if d not in ('.git', 'build', 'node_modules', '.gradle')]
            self.assertNotIn('part2_occurrences_2026.json', files, folder)


@unittest.skipIf(REAL is None, 'part2/codes.json is not generated yet')
class DeliberateBreaksTest(unittest.TestCase):
    """Each guarded rule is broken on purpose; the check must name it."""

    def broken(self, edit):
        data = copy.deepcopy(REAL)
        edit(data)
        return part2_codes_check.problems(data)

    def test_missing_serial(self):
        self.assertTrue(self.broken(lambda d: d['serials'].pop(10)))

    def test_duplicate_code(self):
        self.assertTrue(self.broken(lambda d: d['serials'][1]['codes'].append(d['serials'][0]['codes'][0])))

    def test_lowercase_or_slash_in_a_code(self):
        self.assertTrue(self.broken(lambda d: d['serials'][0]['codes'].__setitem__(0, 'gtdpc')))
        self.assertTrue(self.broken(lambda d: d['serials'][0]['codes'].__setitem__(0, 'GTD/PC')))

    def test_unknown_group_and_inferred_blank_group(self):
        self.assertTrue(self.broken(lambda d: d['serials'][0].__setitem__('group', 'Z')))
        by = {r['serial']: r for r in REAL['serials']}
        idx = REAL['serials'].index(by['3.28'])
        self.assertTrue(self.broken(lambda d: d['serials'][idx].__setitem__('group', 'A')))

    def test_empty_name_angle_brackets_and_bad_page(self):
        self.assertTrue(self.broken(lambda d: d['serials'][0].__setitem__('name', '')))
        self.assertTrue(self.broken(lambda d: d['serials'][0].__setitem__('name', 'a <b> c')))
        self.assertTrue(self.broken(lambda d: d['serials'][0].__setitem__('page', 0)))

    def test_chapter_must_match_the_serial(self):
        self.assertTrue(self.broken(lambda d: d['serials'][0].__setitem__('chapter', 2)))

    def test_unknown_version(self):
        self.assertTrue(self.broken(lambda d: d.__setitem__('version', 2)))


@unittest.skipUnless(os.path.isdir(os.path.join(SOURCES_DIR, 'part2')) and REAL is not None,
                     'local manual sources not present (outside git; CI skips this)')
class LocalRegenerationTest(unittest.TestCase):
    def test_regenerating_from_the_manual_gives_the_committed_file(self):
        self.assertEqual(part2_codes.generate(), REAL)


if __name__ == '__main__':
    unittest.main()

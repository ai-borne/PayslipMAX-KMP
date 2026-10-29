"""Tests for part2_nav.py and part2/nav.json, the ribbon layout behind the Part II code lookup (Part II plan, phase 1).

Why they matter: the layout is what an officer browses, and codes.json is what they look up. A serial with no home, or two,
is a code that cannot be found or that appears under two ribbons; a tile marked buildable before its ribbon exists promises
a draft the app cannot make; and the bundle slice is the only Part II data the app ever reads, so nothing private may ride in
it. Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_nav.py'
"""
import copy
import unittest

import part2_codes_check
import part2_nav

CODES = part2_codes_check.load()
NAV = part2_nav.load()
SERIALS = [row['serial'] for row in CODES['serials']]

# Build order of RIBBON_LAYOUT.md, with (cases, homed serials) per ribbon as its coverage check printed them.
RIBBONS = {'leave': (7, 41), 'moves': (4, 23), 'field': (4, 23), 'career': (4, 31), 'exit': (4, 20),
           'family': (6, 33), 'health': (4, 18), 'role': (3, 34), 'depu': (4, 28), 'records': (7, 52)}
# Lookup-only marks (DECISIONS Round 8): five cases the officer never drafts from, plus three codes inside draftable cases.
LOOKUP_CASES = {'exit_missing', 'records_commission', 'records_discipline_susp', 'records_discipline_trial', 'records_ta'}
LOOKUP_INSIDE = {'13.13', '13.32', '13.33'}


def cases(nav=NAV):
    return [case for ribbon in nav['ribbons'] for case in ribbon['cases']]


def broken(edit):
    nav = copy.deepcopy(NAV)
    edit(nav)
    return part2_nav.problems(nav, CODES)


def first_case(nav, ribbon='leave'):
    return next(r for r in nav['ribbons'] if r['id'] == ribbon)['cases'][0]


class RealLayout(unittest.TestCase):
    def test_the_agreed_limits_are_the_ones_in_the_file(self):
        # why: the checker reads its limits from nav.json, so loosening one there would pass every other test (Round 8, layout section 1).
        self.assertEqual(NAV['limits'], {'title_words': 7, 'ribbons_min': 8, 'ribbons_max': 10, 'cases_per_ribbon': 8,
                                         'case_serials_min': 2, 'case_serials_max': 16})

    def test_the_shipped_layout_is_valid_against_the_real_codes(self):
        self.assertEqual(part2_nav.problems(NAV, CODES), [])
        self.assertEqual(part2_nav.check(), [])

    def test_ten_ribbons_in_build_order_with_forty_seven_cases(self):
        # why: Phase 12 (plan item 49) gave the Leave ribbon a case for the area re-grants after hospital or a course; its serials came from two Field cases that
        # then held one serial each, so those cases went (48 - 2 + 1).
        self.assertEqual([r['id'] for r in NAV['ribbons']], list(RIBBONS))
        self.assertEqual(len(cases()), 47)
        for ribbon in NAV['ribbons']:
            counts = (len(ribbon['cases']), sum(len(c['serials']) for c in ribbon['cases']))
            self.assertEqual(counts, RIBBONS[ribbon['id']], ribbon['id'])

    def test_every_one_of_the_303_serials_is_homed_exactly_once(self):
        # why: search reaches all 303 code pages, and a serial homed twice would show under two ribbons.
        homed = [s for c in cases() for s in c['serials']]
        self.assertEqual(len(homed), 303)
        self.assertEqual(sorted(homed), sorted(SERIALS))

    def test_case_sizes_titles_and_ribbon_sizes_are_inside_the_limits(self):
        for case in cases():
            self.assertTrue(2 <= len(case['serials']) <= 16, case['id'])
            self.assertLessEqual(len(case['title'].split()), 7, case['id'])
        for ribbon in NAV['ribbons']:
            self.assertLessEqual(len(ribbon['cases']), 8, ribbon['id'])
            self.assertLessEqual(len(ribbon['title'].split()), 7, ribbon['id'])

    def test_the_leave_ribbon_homes_all_thirty_chapter_5_serials_and_the_eleven_area_serials_of_its_absence_case(self):
        leave = next(r for r in NAV['ribbons'] if r['id'] == 'leave')
        absence = ['3.13', '3.14', '3.15', '3.19', '3.20', '3.21', '3.22', '3.23', '3.24', '3.25', '3.26']
        self.assertEqual(sorted(s for c in leave['cases'] for s in c['serials']), sorted([f'5.{n}' for n in range(1, 31)] + absence))

    def test_only_leave_and_records_are_built(self):
        # why: tiles show only for built ribbons (Round 8). Each later release flips its ribbon here, on purpose.
        self.assertEqual({r['id'] for r in NAV['ribbons'] if r['built']}, {'leave', 'records'})

    def test_53_serials_are_lookup_only_and_the_five_lookup_cases_are_entirely_so(self):
        marks = [s for c in cases() for s in c['lookup']]
        self.assertEqual(len(marks), 53)
        whole = {c['id'] for c in cases() if sorted(c['lookup']) == sorted(c['serials'])}
        # The layout lists the two laptop codes as lookup too, so that case is lookup in full without being flagged as one.
        self.assertEqual(whole, LOOKUP_CASES | {'records_laptop'})
        inside = {s for c in cases() if c['id'] not in LOOKUP_CASES for s in c['lookup']}
        self.assertEqual(inside, LOOKUP_INSIDE)

    def test_cancel_and_cancels_are_not_lookup_only(self):
        # why: they ship with the Leave release (Round 9), so their case must not be marked as unbuildable.
        cancelling = next(c for c in cases() if c['serials'] == ['14.1', '14.2'])
        self.assertEqual(cancelling['lookup'], [])

    def test_also_links_point_at_codes_homed_elsewhere(self):
        for case in cases():
            self.assertFalse(set(case['also']) & set(case['serials']), case['id'])
            self.assertTrue(set(case['also']) <= set(SERIALS), case['id'])

    def test_back_from_leave_lists_every_area_regrant_the_leave_templates_can_link(self):
        # why: the 15 leave templates link these re-grants (phase 9), so an officer browsing "Back from leave" must find every code one can
        # print, STPTRAN (3.54) included, even though it is homed in Moves.
        import json
        import os
        from config import CARDS_DIR
        serial_of = {code: row['serial'] for row in CODES['serials'] for code in row['codes']}
        granted = set()
        for name in ('area.json', 'tran.json'):
            with open(os.path.join(CARDS_DIR, 'part2', 'templates', name), encoding='utf-8') as fh:
                data = json.load(fh)
            granted |= {serial_of[t['code']] for t in (data['templates'] if isinstance(data, dict) else data)}
        back = next(c for c in cases() if c['id'] == 'leave_back')
        absence = next(c for c in cases() if c['id'] == 'leave_absence')
        self.assertGreater(len(granted), 9, 'the scan found almost no area templates')
        # Phase 12: HAA, SCCIA and the field areas are homed in the sibling case that can draft them after hospital or a course.
        self.assertEqual(sorted(granted - set(back['serials']) - set(back['also']) - set(absence['serials'])), [])


class Rules(unittest.TestCase):
    """Each rule is broken on purpose: a checker that cannot fail would let a bad layout through."""

    def assertRefused(self, edit, *fragments):
        found = broken(edit)
        self.assertTrue(any(all(f in line for f in fragments) for line in found), f'wanted {fragments} in {found}')

    def test_a_serial_with_no_home_is_refused(self):
        self.assertRefused(lambda n: first_case(n)['serials'].pop(), 'serial 5.5', 'no home')

    def test_a_serial_homed_twice_is_refused(self):
        self.assertRefused(lambda n: first_case(n, 'moves')['serials'].append('5.1'), 'serial 5.1', 'homed twice')

    def test_an_unknown_serial_is_refused_in_every_list(self):
        self.assertRefused(lambda n: first_case(n)['serials'].append('99.9'), 'unknown serial 99.9')
        self.assertRefused(lambda n: first_case(n)['also'].append('99.9'), 'also 99.9', 'unknown serial')

    def test_case_size_limits_are_enforced(self):
        self.assertRefused(lambda n: first_case(n, 'records').update(serials=first_case(n, 'records')['serials'][:1], lookup=[]), 'allowed 2 to 16')
        self.assertRefused(lambda n: n['limits'].update(case_serials_max=4), 'allowed 2 to 4')

    def test_too_many_cases_or_ribbons_are_refused(self):
        self.assertRefused(lambda n: n['limits'].update(cases_per_ribbon=3), 'max 3')
        self.assertRefused(lambda n: n['ribbons'].__delitem__(slice(-3, None)), '7 ribbons, allowed 8 to 10')
        self.assertRefused(lambda n: n['limits'].update(ribbons_max=9), 'allowed 8 to 9')

    def test_a_long_title_is_refused(self):
        self.assertRefused(lambda n: first_case(n).update(title='one two three four five six seven eight'), 'title', 'over 7 words')
        self.assertRefused(lambda n: n['ribbons'][0].update(title='one two three four five six seven eight'), 'ribbon leave', 'over 7 words')

    def test_angle_brackets_and_empty_titles_are_refused(self):
        self.assertRefused(lambda n: first_case(n).update(title='Leave <b>'), 'angle brackets')
        self.assertRefused(lambda n: first_case(n).update(title=' '), 'title is empty')

    def test_ids_must_be_well_formed_and_unique(self):
        self.assertRefused(lambda n: first_case(n).update(id='Bad Id'), 'id must be lowercase')
        self.assertRefused(lambda n: first_case(n, 'moves').update(id=first_case(n)['id']), 'duplicate case id')
        self.assertRefused(lambda n: n['ribbons'][1].update(id='leave'), 'duplicate ribbon id')

    def test_lookup_marks_must_name_serials_of_that_case(self):
        self.assertRefused(lambda n: first_case(n)['lookup'].append('13.13'), 'lookup 13.13', 'not in the case')

    def test_an_also_link_may_not_point_at_the_cases_own_code(self):
        self.assertRefused(lambda n: first_case(n)['also'].append('5.1'), 'also 5.1', 'homed in this case')

    def test_a_repeated_entry_in_one_list_is_refused(self):
        self.assertRefused(lambda n: first_case(n)['also'].append(first_case(n)['also'][0]), 'repeats')

    def test_built_must_be_true_or_false(self):
        self.assertRefused(lambda n: n['ribbons'][0].update(built='yes'), 'built must be true or false')

    def test_an_unknown_version_is_refused(self):
        self.assertRefused(lambda n: n.update(version=2), 'version must be 1')


class BundleSection(unittest.TestCase):
    SECTION = part2_nav.for_bundle(NAV, CODES)

    def test_the_section_carries_every_code_page_and_the_layout(self):
        s = self.SECTION
        self.assertEqual((s['version'], len(s['serials']), sum(len(r['codes']) for r in s['serials']), len(s['ribbons'])), (1, 303, 321, 10))
        self.assertEqual(sum(len(r['cases']) for r in s['ribbons']), 47)
        self.assertEqual(list(s['groups']), ['A', 'AM', 'B', 'C', 'CM'])

    def test_the_limits_ship_so_the_app_holds_no_second_copy(self):
        self.assertEqual(self.SECTION['limits'], {**NAV['limits'], 'text': part2_codes_check.NAME_MAX})

    def test_a_group_the_manual_does_not_print_ships_without_one(self):
        # why: "Group not printed in ODP-2026" (Round 8) must reach the app as an absent group, never an inferred one.
        by = {r['serial']: r for r in self.SECTION['serials']}
        self.assertNotIn('group', by['3.28'])
        self.assertNotIn('group', by['11.8'])
        self.assertEqual(by['5.1']['group'], 'A')
        self.assertEqual(sum('group' not in r for r in self.SECTION['serials']), 2)

    def test_only_whitelisted_keys_ship(self):
        # why: a reviewer note or source path added to the authoring files must not reach the APK by default.
        codes, nav = copy.deepcopy(CODES), copy.deepcopy(NAV)
        codes['serials'][0]['reviewer_note'] = 'internal'
        codes['note'] = 'internal'
        nav['ribbons'][0]['cases'][0]['owner_note'] = 'internal'
        nav['ribbons'][0]['draft_order'] = 1
        text = repr(part2_nav.for_bundle(nav, codes))
        for private in ('reviewer_note', 'owner_note', 'draft_order', 'internal', 'Documentation Procedure'):
            self.assertNotIn(private, text)
        self.assertEqual(set(self.SECTION), {'version', 'limits', 'groups', 'serials', 'ribbons'})
        self.assertEqual(set(self.SECTION['ribbons'][0]), {'id', 'title', 'built', 'cases'})
        self.assertEqual(set(first_case(self.SECTION)), {'id', 'title', 'serials', 'also', 'lookup'})

    def test_the_section_is_deterministic(self):
        again = part2_nav.for_bundle(copy.deepcopy(NAV), copy.deepcopy(CODES))
        self.assertEqual(repr(self.SECTION), repr(again))

    def test_an_invalid_layout_is_not_bundled(self):
        nav = copy.deepcopy(NAV)
        first_case(nav)['serials'].pop()
        with self.assertRaises(SystemExit):
            part2_nav.for_bundle(nav, CODES)


if __name__ == '__main__':
    unittest.main()

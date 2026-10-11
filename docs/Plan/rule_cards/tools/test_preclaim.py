"""Tests for preclaim.py and preclaim.json, the TA/DA pre-claim worksheet content (plan 22, P3).

Why they matter: an officer files a real advance from this worksheet, so the field order must match the portal (or the officer
copies a value into the wrong box), the LTC rule picker must link only to cards that really exist (a dead link is a dead button),
and nothing the app cannot verify may look verified. Run: python3 tools/test_preclaim.py
"""
import copy
import json
import os
import unittest

import bundle
import preclaim
from config import CARDS_DIR

RULEBOOK = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))
CARDS = RULEBOOK['cards']
REAL = preclaim.load()

# Portal order, from 21_ta_da_preclaim_plan.md (blank create forms, owner's screenshots 2026-10-11). Type of Move is the chooser, not a field.
PORTAL_ORDER = {
    'PD': ['pcda_account', 'officer_name', 'spr', 're_employed', 'marital_status', 'spouse_in_army', 'basic_pay', 'npa', 'msp',
           'sanction_id', 'sanction_date', 'from_station', 'to_station', 'andaman_nicobar', 'from_date'],
    'TD': ['pcda_account', 'officer_name', 'spr', 'marital_status', 'basic_pay', 'npa', 'msp', 'sanction_id', 'sanction_date',
           'from_station', 'to_station', 'andaman_nicobar', 'from_date', 'to_date', 'long_course', 'tr_rule', 'mode_of_journey',
           'total_fare', 'total_absence', 'food_charges', 'rma', 'accommodation', 'amount_required', 'dts_advance_certificate'],
    'LTC': ['pcda_account', 'officer_name', 'home_station', 'spr', 're_employed', 'marital_status', 'spouse_in_army', 'basic_pay',
            'npa', 'msp', 'sanction_id', 'sanction_date', 'from_station', 'to_station', 'andaman_nicobar', 'from_date', 'to_date',
            'self', 'spouse', 'children', 'dependents', 'persons_travelling', 'tr_rule', 'visit_to', 'mode_of_journey', 'year',
            'nature_of_leave', 'total_fare'],
}


def fresh():
    return copy.deepcopy(REAL)


def problems(data):
    return preclaim.validate(data, CARDS)


def field(data, move, field_id):
    return next(f for f in data['moves'][move]['fields'] if f['id'] == field_id)


class RealContent(unittest.TestCase):
    def test_the_shipped_file_is_valid_against_the_real_rulebook(self):
        self.assertEqual(problems(REAL), [])

    def test_field_order_equals_the_portal_order_for_every_move(self):
        self.assertEqual(set(REAL['moves']), {'PD', 'TD', 'LTC'})
        for move, order in PORTAL_ORDER.items():
            self.assertEqual([f['id'] for f in REAL['moves'][move]['fields']], order, move)

    def test_the_sanction_id_holds_at_most_forty_characters_on_every_move(self):
        for move in PORTAL_ORDER:
            self.assertEqual(field(REAL, move, 'sanction_id')['max_chars'], 40, move)

    def test_who_fills_each_field_matches_what_the_portal_shows(self):
        # Locked or greyed on the portal: the officer never types them, so the app must never offer to copy them.
        for move in PORTAL_ORDER:
            for fid in ('pcda_account', 'officer_name'):
                self.assertEqual(field(REAL, move, fid)['source'], 'portal_filled', f'{move} {fid}')
        for fid in ('total_absence', 'amount_required'):
            self.assertEqual(field(REAL, 'TD', fid)['source'], 'portal_filled', fid)
        self.assertEqual(field(REAL, 'LTC', 'persons_travelling')['source'], 'portal_filled')
        # The officer types the fare on TD and LTC (F5); the app only estimates food, RMA and lodging.
        self.assertEqual(field(REAL, 'TD', 'total_fare')['source'], 'officer_types')
        for fid in ('food_charges', 'rma', 'accommodation'):
            self.assertEqual(field(REAL, 'TD', fid)['source'], 'app_suggests', fid)

    def test_what_was_not_confirmed_on_the_portal_is_marked_unverified(self):
        unverified = {(m, f['id']) for m, move in REAL['moves'].items() for f in move['fields'] if f.get('unverified')}
        self.assertEqual(unverified, {('TD', 'long_course'), ('LTC', 'children'), ('LTC', 'dependents'), ('LTC', 'visit_to'), ('LTC', 'year'),
                                      ('PD', 'marital_status'), ('TD', 'marital_status'), ('LTC', 'marital_status')})
        for move, fid in unverified:
            self.assertTrue(field(REAL, move, fid)['hint'], f'{move} {fid} must say what is unconfirmed')

    def test_every_move_lists_documents_and_the_upload_rule_is_the_portals(self):
        for move in REAL['moves'].values():
            self.assertTrue(move['documents'])
        self.assertEqual(REAL['upload'], {'files': 1, 'more_than_kb': 50, 'less_than_mb': 2, 'format': 'PDF'})
        ltc_docs = {d['id']: d for d in REAL['moves']['LTC']['documents']}
        self.assertTrue(ltc_docs['leave_certificate']['when'], 'the leave certificate is needed only when the officer travels')

    def test_ltc_rule_picker_has_the_eight_portal_options_each_linked_to_a_live_card(self):
        picker = REAL['options']['ltc_rule']
        self.assertTrue(picker['card_linked'])
        self.assertEqual(len(picker['options']), 8)
        homed = {c['id'] for c in CARDS if not c.get('replaced_by')}
        for option in picker['options']:
            self.assertIn(option['card'], homed, option['id'])
            self.assertTrue(option['use_when'], option['id'])

    def test_td_rule_options_are_the_four_portal_rules_and_carry_no_card_link(self):
        group = REAL['options']['td_rule']
        self.assertEqual([o['id'] for o in group['options']], ['tr4i', 'tr4ii', 'tr119', 'tr124'])
        self.assertFalse(group['card_linked'])
        self.assertIn('DA not admissible', group['options'][3]['label'])

    def test_rule_124_hint_names_every_line_the_calculator_makes_nil(self):
        # The calculator gives food, hotel and local taxi nil under Rule 124 (owner, 2026-10-11); a hint that names fewer lines tells
        # the officer to expect a taxi cap the estimate then refuses to show.
        hint = REAL['options']['td_rule']['options'][3]['use_when'].lower()
        for line in ('food', 'hotel', 'taxi'):
            self.assertIn(line, hint)

    def test_mode_of_journey_is_shared_by_td_and_ltc(self):
        self.assertEqual([o['label'] for o in REAL['options']['mode_of_journey']['options']], ['Air', 'Rail', 'Road', 'Sea', 'Multiple'])
        for move in ('TD', 'LTC'):
            self.assertEqual(field(REAL, move, 'mode_of_journey')['options'], 'mode_of_journey')

    def test_the_td_certificate_is_the_last_field_and_a_tick(self):
        last = REAL['moves']['TD']['fields'][-1]
        self.assertEqual((last['id'], last['kind']), ('dts_advance_certificate', 'tick'))


class Validation(unittest.TestCase):
    def assertProblem(self, data, *fragments):
        found = problems(data)
        self.assertTrue(any(all(f in p for f in fragments) for p in found), f'wanted {fragments} in {found}')

    def test_an_unknown_move_type_is_rejected(self):
        d = fresh()
        d['moves']['XX'] = d['moves']['PD']
        self.assertProblem(d, 'unknown move', 'XX')

    def test_a_missing_move_is_rejected(self):
        d = fresh()
        del d['moves']['LTC']
        self.assertProblem(d, 'LTC', 'missing')

    def test_a_move_without_documents_is_rejected(self):
        d = fresh()
        d['moves']['TD']['documents'] = []
        self.assertProblem(d, 'TD', 'documents')

    def test_a_field_without_a_label_is_rejected(self):
        for bad in ('', '   ', None):
            d = fresh()
            d['moves']['PD']['fields'][2]['label'] = bad
            self.assertProblem(d, 'PD', 'spr', 'label')

    def test_a_label_over_the_length_limit_is_rejected(self):
        d = fresh()
        d['moves']['PD']['fields'][2]['label'] = 'x' * (preclaim.LIMITS['label'] + 1)
        self.assertProblem(d, 'spr', 'label')
        d = fresh()
        d['moves']['PD']['fields'][2]['hint'] = 'x' * (preclaim.LIMITS['hint'] + 1)
        self.assertProblem(d, 'spr', 'hint')

    def test_markup_in_any_text_is_rejected(self):
        for where in (lambda d: d['moves']['PD']['fields'][2].update(label='<b>SPR</b>'),
                      lambda d: d['moves']['PD']['documents'][0].update(label='a > b'),
                      lambda d: d['moves']['PD'].update(title='<i>x</i>'),
                      lambda d: d['options']['ltc_rule']['options'][0].update(use_when='<script>')):
            d = fresh()
            where(d)
            self.assertProblem(d, 'angle brackets')

    def test_a_picker_option_without_a_card_is_rejected(self):
        d = fresh()
        del d['options']['ltc_rule']['options'][0]['card']
        self.assertProblem(d, 'ltc_rule', 'needs a card')

    def test_a_picker_option_naming_a_missing_or_replaced_card_is_rejected(self):
        d = fresh()
        d['options']['ltc_rule']['options'][0]['card'] = 'RB-NOPE'
        self.assertProblem(d, 'ltc_rule', 'RB-NOPE')
        replaced = copy.deepcopy(CARDS)
        replaced[[c['id'] for c in replaced].index(REAL['options']['ltc_rule']['options'][0]['card'])]['replaced_by'] = 'RB-X'
        self.assertTrue(any('replaced' in p for p in preclaim.validate(REAL, replaced)))

    def test_a_group_that_is_not_card_linked_may_not_name_a_card(self):
        d = fresh()
        d['options']['td_rule']['options'][0]['card'] = CARDS[0]['id']
        self.assertProblem(d, 'td_rule', 'card')

    def test_a_choice_field_must_name_an_existing_option_group(self):
        d = fresh()
        field(d, 'TD', 'tr_rule')['options'] = 'nope'
        self.assertProblem(d, 'tr_rule', 'nope')
        d = fresh()
        del field(d, 'TD', 'tr_rule')['options']
        self.assertProblem(d, 'tr_rule', 'options')

    def test_only_a_choice_field_takes_options_and_only_a_text_field_takes_max_chars(self):
        d = fresh()
        field(d, 'TD', 'basic_pay')['options'] = 'mode_of_journey'
        self.assertProblem(d, 'basic_pay', 'options')
        d = fresh()
        field(d, 'TD', 'basic_pay')['max_chars'] = 10
        self.assertProblem(d, 'basic_pay', 'max_chars')

    def test_a_text_limit_must_be_a_sane_whole_number(self):
        for bad in (0, -1, 201, 4.5, True):
            d = fresh()
            field(d, 'PD', 'sanction_id')['max_chars'] = bad
            self.assertProblem(d, 'sanction_id', 'max_chars')

    def test_unknown_kind_or_source_is_rejected(self):
        d = fresh()
        field(d, 'PD', 'spr')['kind'] = 'slider'
        self.assertProblem(d, 'spr', 'kind')
        d = fresh()
        field(d, 'PD', 'spr')['source'] = 'magic'
        self.assertProblem(d, 'spr', 'source')

    def test_duplicate_ids_are_rejected(self):
        d = fresh()
        d['moves']['PD']['fields'][3]['id'] = 'spr'
        self.assertProblem(d, 'PD', 'duplicate', 'spr')
        d = fresh()
        d['options']['mode_of_journey']['options'][1]['id'] = 'air'
        self.assertProblem(d, 'mode_of_journey', 'duplicate')

    def test_ids_are_short_lowercase_words(self):
        d = fresh()
        d['moves']['PD']['fields'][2]['id'] = 'Spr Field'
        self.assertProblem(d, 'id')

    def test_the_upload_rule_is_checked(self):
        for key, bad in (('files', 2), ('more_than_kb', 0), ('less_than_mb', -1), ('format', 'DOCX')):
            d = fresh()
            d['upload'][key] = bad
            self.assertProblem(d, 'upload', key)

    def test_a_version_other_than_one_is_rejected(self):
        d = fresh()
        d['version'] = 2
        self.assertProblem(d, 'version')


class OwnWordsGuard(unittest.TestCase):
    def test_every_sentence_a_user_reads_is_offered_to_the_copy_guard(self):
        # copycheck.py scans preclaim.texts(); a sentence missing here would escape the own-words rule (plan 22, P3).
        texts = set(preclaim.texts(REAL))
        for grp in REAL['options'].values():
            for opt in grp['options']:
                self.assertIn(opt.get('use_when', ''), texts | {''}, opt['id'])
        for move in REAL['moves'].values():
            for field in move['fields']:
                self.assertIn(field.get('hint', ''), texts | {''}, field['id'])
            for doc in move['documents']:
                self.assertIn(doc['label'], texts)
            for note in move['notes']:
                self.assertIn(note, texts)
        self.assertIn(REAL['options']['ltc_rule']['options'][0]['use_when'], texts)
        self.assertNotIn('', texts)


class Shipping(unittest.TestCase):
    def test_only_whitelisted_fields_ship_and_repo_only_notes_stay_home(self):
        d = fresh()
        d['provenance'] = ['owner screenshots']
        d['moves']['PD']['fields'][2]['reviewer_note'] = 'internal'
        d['moves']['PD']['internal'] = 'x'
        shipped = preclaim.for_bundle(d)
        self.assertNotIn('provenance', shipped)
        self.assertNotIn('internal', shipped['moves']['PD'])
        self.assertNotIn('reviewer_note', shipped['moves']['PD']['fields'][2])

    def test_a_field_ships_in_authored_order_with_only_the_keys_it_has(self):
        shipped = preclaim.for_bundle(REAL)
        self.assertEqual([f['id'] for f in shipped['moves']['TD']['fields']], PORTAL_ORDER['TD'])
        self.assertEqual(set(shipped['moves']['PD']['fields'][0]), {'id', 'label', 'kind', 'source'})

    def test_the_length_limits_ship_with_the_section_so_the_app_has_no_second_copy(self):
        self.assertEqual(preclaim.for_bundle(REAL)['limits'], {'label': 60, 'hint': 160, 'text_chars': 200})

    def test_the_bundle_carries_the_section_and_stays_version_one(self):
        built = bundle.build(RULEBOOK)
        self.assertEqual(built['preclaim'], preclaim.for_bundle(REAL))
        self.assertEqual(built['version'], 1)

    def test_a_bundle_build_stops_on_invalid_content(self):
        d = fresh()
        d['moves']['PD']['documents'] = []
        with self.assertRaises(SystemExit):
            bundle.build(RULEBOOK, preclaim_data=d)


if __name__ == '__main__':
    unittest.main()

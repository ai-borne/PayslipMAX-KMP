"""Tests for content batch 2 of the Part II templates: 14.1 CANCEL and 14.2 CANCELS (Part II plan, phase 6c).

Why they matter: Part II orders cannot be amended (para 11, p.8), so a wrong order is cancelled and notified afresh, and a cancellation
that quotes the wrong columns is itself rejected. Each test names the manual's page and note it pins: the "ALL" in col 6, the cancelled
order's number and date in cols 8 and 9, the occurrence code and serial of CANCELS in cols 6 and 7, the certificate, and the checks.
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_cancel.py'
"""
import os
import unittest

import part2_codes_check
import part2_review
import part2_rules
import part2_templates
from config import CARDS_DIR

REAL = {t['code']: t for t in part2_templates.load_templates()}
RULES = {r['id']: r for r in part2_rules.load_rules()['rules']}
CANCEL_FILE = os.path.join(CARDS_DIR, 'part2', 'templates', 'cancel.json')


def cancel():
    return REAL['CANCEL']


def cancels():
    return REAL['CANCELS']


class Shape(unittest.TestCase):
    def test_the_batch_is_its_own_file_of_exactly_the_two_cancellation_codes(self):
        # why: one file per content batch keeps each under 300 lines and each review pack section findable.
        self.assertTrue(os.path.isfile(CANCEL_FILE))
        self.assertEqual(['CANCEL', 'CANCELS'], [t['code'] for t in part2_templates.load_templates() if t['page'] in (226, 227)])

    def test_the_real_rules_and_templates_pass_every_check(self):
        self.assertEqual([], part2_templates.check())

    def test_both_cite_the_printed_page_of_their_row(self):
        self.assertEqual(226, cancel()['page'])
        self.assertEqual(227, cancels()['page'])


class Cancel(unittest.TestCase):
    def test_all_occurrences_of_the_order_are_named_in_col_6_and_the_order_in_cols_8_and_9(self):
        # why: p.226 note 3 - "ALL" in col 6, the cancelled order's no. and date in cols 8 and 9; cols 4, 5 and 7 are not required.
        t = cancel()
        self.assertEqual({'need': 'none'}, t['from'])
        self.assertEqual({'need': 'none'}, t['to'])
        self.assertEqual([{'need': 'required', 'text': 'ALL'}, {'need': 'none'},
                          {'need': 'required', 'slot': 'cancelled.no'}, {'need': 'required', 'slot': 'cancelled.date'}], t['data'])

    def test_the_narrative_quotes_the_cancelled_order_as_the_manual_words_it(self):
        # why: p.226 note 2 - the number and date of the order being cancelled should invariably be quoted in col 3.
        self.assertEqual('Cancelled all occurrence of Part II Order Number {cancelled.no} dated {cancelled.date}', cancel()['narrative'])

    def test_the_certificate_says_the_previous_status_may_be_restored(self):
        self.assertEqual('Certified that the previous status may be restored due to cancellation of Part II Order No {cancelled.no} dated {cancelled.date}',
                         cancel()['certificate'])

    def test_it_asks_for_the_order_and_an_optional_occurrence_date_only_the_time_bar_reads_and_links_nothing(self):
        # why: the vault fills the reference; the occurrence date is optional and prints nowhere; no other order comes with it (the re-draft is a separate draft).
        self.assertEqual([('cancelled', 'ref'), ('occurrence_on', 'date')], [(s['id'], s['kind']) for s in cancel()['slots']])
        self.assertEqual([], cancel().get('links', []))

    def test_it_runs_the_quoted_and_time_bar_checks(self):
        self.assertEqual(['cancel_quoted', 'cancel_time_barred'], cancel()['rules'])


class CancelS(unittest.TestCase):
    def test_the_code_and_serial_go_in_cols_6_and_7_and_the_order_in_cols_8_and_9(self):
        # why: p.227 note 3 and 5 - the serial in col 7, the occurrence code in col 6, the order's no. and date in cols 8 and 9.
        t = cancels()
        self.assertEqual({'need': 'none'}, t['from'])
        self.assertEqual({'need': 'none'}, t['to'])
        self.assertEqual([{'need': 'required', 'slot': 'cancelled_code'}, {'need': 'required', 'slot': 'cancelled_serial'},
                          {'need': 'required', 'slot': 'cancelled.no'}, {'need': 'required', 'slot': 'cancelled.date'}], t['data'])

    def test_the_narrative_names_the_serial_and_the_order_as_the_manual_words_it(self):
        self.assertEqual('Cancelled occurrence at Ser No {cancelled_serial} of Pt II order No {cancelled.no} dated {cancelled.date}', cancels()['narrative'])

    def test_the_certificate_names_the_serial_and_the_order(self):
        self.assertEqual('Certified that the previous status may be restored due to cancellation of Sr No {cancelled_serial} of the Part II Order No {cancelled.no} dated {cancelled.date}',
                         cancels()['certificate'])

    def test_it_asks_for_the_order_the_code_and_the_serial_of_the_one_occurrence(self):
        # why: note 7 - CANCELS cancels one occurrence only; the officer says which, by the serial the original order published.
        self.assertEqual([('cancelled', 'ref'), ('cancelled_code', 'text'), ('cancelled_serial', 'number'), ('occurrence_on', 'date')], [(s['id'], s['kind']) for s in cancels()['slots']])
        self.assertEqual([], cancels().get('links', []))

    def test_it_runs_the_quoted_unknown_code_and_time_bar_checks(self):
        self.assertEqual(['cancels_quoted', 'cancels_code_unknown', 'cancels_time_barred'], cancels()['rules'])


class OccurrenceDate(unittest.TestCase):
    def slot(self, template):
        return next(s for s in template['slots'] if s['id'] == 'occurrence_on')

    def test_the_occurrence_date_is_read_only_by_the_time_bar_check_and_never_printed(self):
        # why: para 13 counts the 2 years from the occurrence; the officer may know it. It is optional (`for`), so it prints nowhere and never blocks a draft.
        self.assertEqual('cancel_time_barred', self.slot(cancel())['for'])
        self.assertEqual('cancels_time_barred', self.slot(cancels())['for'])
        for t in (cancel(), cancels()):
            printed = ' '.join([t['narrative'], t.get('certificate', '')] + [c.get('text', '') + (c.get('slot') or '') for c in t['data']])
            self.assertNotIn('occurrence_on', printed)

    def test_its_words_say_it_is_optional_and_what_it_is_for(self):
        for t in (cancel(), cancels()):
            slot = self.slot(t)
            self.assertIn('Optional', slot['help'])
            self.assertIn('time-bar', slot['help'])


class Checks(unittest.TestCase):
    def test_each_check_cites_its_page_and_note_and_is_a_warning(self):
        # why: the manual says the time-bar sanction "may be enclosed" and the order "should invariably" be quoted: neither says the order will be
        # rejected, which is what an `ack` is reserved for (DECISIONS Round 11).
        expected = {'cancel_quoted': (226, '2'), 'cancel_time_barred': (226, '4'), 'cancels_quoted': (227, '2'), 'cancels_time_barred': (227, '6'), 'cancels_code_unknown': (227, '5')}
        for rid, (page, note) in expected.items():
            rule = RULES[rid]
            self.assertEqual(('check', 'warn', 'manual', page, note), (rule['kind'], rule['severity'], rule['basis'], rule['page'], rule['note']), rid)


class ReviewPack(unittest.TestCase):
    def test_the_pack_has_a_section_per_code_and_names_pages_226_and_227(self):
        text = part2_review.render(part2_templates.load_templates(), part2_rules.load_rules(), part2_codes_check.load())
        for code in ('CANCEL', 'CANCELS'):
            self.assertEqual(1, text.count(f'## {code} '), code)
        self.assertIn('226', text.split('and check the narrative', 1)[0])
        self.assertIn('227', text.split('and check the narrative', 1)[0])

    def test_the_judgment_calls_name_the_manuals_own_slip_over_the_code_para_12_3_misprints(self):
        # why: para 12.3 (p.8) says "CANCELCS", the annexure and HRMS say CANCELS; the owner should see which one the app follows and why.
        self.assertTrue(any('CANCELCS' in p and 'CANCELS' in p for p in part2_review.OPEN_POINTS))

    def test_the_judgment_calls_name_the_time_bar_basis(self):
        # why: para 13 counts the two years from the occurrence; the app knows only the cancelled order's date, so the owner must see that it checks that.
        self.assertTrue(any('time bar' in p.lower() and 'order' in p.lower() and '2 years' in p and 'occurrence date' in p for p in part2_review.OPEN_POINTS))

    def test_the_judgment_calls_name_the_unknown_code_warning(self):
        # why: the typed code is checked against the manual's list as a warning only; the owner should see that and what it cites.
        self.assertTrue(any('cancels_code_unknown' in p and 'warn' in p for p in part2_review.OPEN_POINTS))


if __name__ == '__main__':
    unittest.main()

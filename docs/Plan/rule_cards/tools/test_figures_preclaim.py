"""Tests for the TA/DA pre-claim figures in figures.json (plan 22, P2): td_hotel, td_taxi, baggage_road and the per-state rma table.

Why they matter: these four feed a worksheet an officer files a real advance from, so each must be approved, dated, honestly labelled
and complete (every state present). The card-linked figures are tested in test_figures.py. Run: python3 tools/test_figures_preclaim.py
"""
import os
import unittest

import figures
from config import CARDS_DIR
from test_figures import PRECLAIM, REAL, fresh, problems


class PreClaimFigures(unittest.TestCase):
    """The four figures behind the TA/DA pre-claim worksheet (plan 22, P2). Each was approved by the owner on 2026-10-11 (register 23)."""

    def test_each_is_approved_dated_and_honestly_labelled(self):
        for key in PRECLAIM:
            fig = REAL['figures'][key]
            self.assertEqual(fig['approved'], '2026-10-11', key)
            self.assertEqual(fig['use'], 'preclaim', key)
            self.assertTrue(fig['authority'], key)
        # The Army letter text behind hotel, taxi and baggage was never read (register 23): the label must say so.
        for key in ('td_hotel', 'td_taxi', 'baggage_road'):
            self.assertEqual(REAL['figures'][key]['evidence'], 'HANDBOOK_ONLY', key)
        self.assertEqual(REAL['figures']['rma']['evidence'], 'LETTER_TEXT')

    def test_the_da_step_date_is_recorded_as_the_owners_decision(self):
        first = REAL['da_step']['first_step']
        self.assertEqual((first['effective_from'], first['evidence'], first['confirmed']), ('2024-01-01', 'OWNER_CONFIRMED', '2026-10-11'))

    def test_a_step_date_decision_needs_a_date_a_known_label_and_a_confirmation(self):
        for field, value in (('effective_from', '01-01-2024'), ('evidence', 'HEARSAY'), ('confirmed', None)):
            d = fresh()
            d['da_step']['first_step'][field] = value
            self.assertTrue(any('first_step' in p for p in problems(d)), field)

    def test_bases_are_the_handbook_figures_before_the_step(self):
        # Handbook TA 2023 rates (7th CPC, 1 Jul 2017). The x1.25 at DA 50 to 99% is da_step's job, so none of these is pre-multiplied.
        def bases(key):
            return {tuple(b['levels']): b.get('base') for b in REAL['figures'][key]['bands']}
        self.assertEqual(sorted(v for v in bases('td_hotel').values()), [2250, 4500, 7500])
        self.assertEqual(bases('baggage_road'), {('10', '10A', '10B', '11', '12', '12A', '12B', '13', '13A', '13B', '14', '15', '16', '17', '18'): 50})
        taxi = REAL['figures']['td_taxi']['bands']
        self.assertEqual([(b['levels'][0], b.get('base')) for b in taxi], [('9', 338), ('12', None), ('14', None)])

    def test_taxi_above_level_11_has_a_limit_text_and_no_amount(self):
        # Register 23: AC taxi up to 50 km a day (12-13B) and at actual (14+) have no rupee cap, so the app may never show one.
        for band in REAL['figures']['td_taxi']['bands'][1:]:
            self.assertNotIn('base', band)
            self.assertTrue(band['limit'])

    def test_every_pre_claim_figure_that_scales_with_da_covers_the_levels_it_claims_once(self):
        for key in ('td_hotel', 'td_taxi', 'baggage_road'):
            self.assertEqual(REAL['figures'][key]['mode'], 'da_step', key)
            self.assertEqual(problems(REAL), [], key)

    def test_a_card_less_figure_must_say_it_is_for_the_pre_claim(self):
        d = fresh()
        del d['figures']['td_hotel']['use']
        self.assertTrue(any('td_hotel' in p and 'use' in p for p in problems(d)))

    def test_a_pre_claim_figure_may_not_also_name_a_card(self):
        d = fresh()
        d['figures']['td_hotel']['card'] = 'RB-SS-T181'
        self.assertTrue(any('td_hotel' in p and 'card' in p for p in problems(d)))

    def test_a_band_needs_a_base_or_a_limit_but_not_both(self):
        d = fresh()
        del d['figures']['td_taxi']['bands'][1]['limit']
        self.assertTrue(any('td_taxi' in p and 'limit' in p for p in problems(d)))
        d = fresh()
        d['figures']['td_taxi']['bands'][0]['limit'] = 'up to 50 km'
        self.assertTrue(any('td_taxi' in p and 'limit' in p for p in problems(d)))

    def test_a_limit_is_short_plain_text(self):
        for bad in ('x' * 121, '<b>AC taxi</b>', ''):
            d = fresh()
            d['figures']['td_taxi']['bands'][1]['limit'] = bad
            self.assertTrue(any('td_taxi' in p and 'limit' in p for p in problems(d)), repr(bad))

    def test_plus_da_bands_do_not_take_a_limit(self):
        d = fresh()
        band = d['figures']['transport_allowance']['bands'][0]
        del band['base']
        band['limit'] = 'text'
        self.assertTrue(any('transport_allowance' in p for p in problems(d)))


class RatePerStateMode(unittest.TestCase):
    """RMA (Wing Circular no. 10): rupees per km by the state the journey starts in. A state missing from the table would be a silent nil."""

    def rates(self, d):
        return d['figures']['rma']['rates']

    def test_the_real_table_has_the_34_states_and_territories_of_the_circular(self):
        self.assertEqual(len(figures.RMA_STATES), 34)
        self.assertEqual(set(self.rates(REAL)), set(figures.RMA_STATES))
        self.assertTrue(all(isinstance(v, int) and v > 0 for v in self.rates(REAL).values()))

    def test_the_owner_approved_values_are_kept_exactly(self):
        # Spot checks across the range: lowest (Punjab), a middle one, the top rate the rates sheet quotes, and a spelling kept from the circular.
        r = self.rates(REAL)
        self.assertEqual((r['Punjab'], r['Maharashtra'], r['Assam'], r['Gujrat']), (12, 20, 30, 18))

    def test_a_missing_state_is_rejected(self):
        d = fresh()
        del self.rates(d)['Goa']
        self.assertTrue(any('rma' in p and 'Goa' in p for p in problems(d)))

    def test_an_extra_or_misspelt_state_is_rejected(self):
        d = fresh()
        self.rates(d)['Gujarat'] = 18
        self.assertTrue(any('rma' in p and 'Gujarat' in p for p in problems(d)))

    def test_a_rate_must_be_a_positive_whole_number_within_bounds(self):
        for bad in (0, -5, 12.5, '12', True, 101, None):
            d = fresh()
            self.rates(d)['Goa'] = bad
            self.assertTrue(any('rma' in p and 'Goa' in p for p in problems(d)), repr(bad))

    def test_it_needs_an_effective_date_unlike_the_hra_table(self):
        d = fresh()
        d['figures']['rma']['effective_from'] = 'soon'
        self.assertTrue(any('rma' in p and 'effective_from' in p for p in problems(d)))

    def test_a_per_state_figure_with_no_rates_is_rejected(self):
        d = fresh()
        del d['figures']['rma']['rates']
        self.assertTrue(any('rma' in p and 'rates' in p for p in problems(d)))

    def test_the_old_standalone_table_is_gone_so_the_table_has_one_home(self):
        self.assertFalse(os.path.exists(os.path.join(CARDS_DIR, 'rma_rates.json')))



class CtgCases(unittest.TestCase):
    """CTG cases (plan 22, P4): island 100% and local move one-third live in the ctg figure, so the calculator holds no rate of its own."""

    def test_the_real_ctg_figure_carries_the_island_percent_and_the_local_move_divisor(self):
        ctg = REAL['figures']['ctg']
        self.assertEqual((ctg['percent'], ctg['island_percent'], ctg['local_move_divisor']), (80, 100, 3))

    def test_both_are_shipped_to_the_app(self):
        shipped = figures.for_bundle(REAL)['figures']['ctg']
        self.assertEqual((shipped['island_percent'], shipped['local_move_divisor']), (100, 3))

    def test_the_island_percent_is_a_whole_percent_not_below_the_normal_one(self):
        for bad in (0, -1, 101, 79, 100.5, '100', True, None):
            d = fresh()
            d['figures']['ctg']['island_percent'] = bad
            self.assertTrue(any('ctg' in p and 'island_percent' in p for p in problems(d)), repr(bad))

    def test_the_local_move_divisor_is_a_whole_number_from_2_to_10(self):
        for bad in (0, 1, 11, -3, 2.5, '3', True, None):
            d = fresh()
            d['figures']['ctg']['local_move_divisor'] = bad
            self.assertTrue(any('ctg' in p and 'local_move_divisor' in p for p in problems(d)), repr(bad))

    def test_only_a_percent_of_basic_figure_may_carry_them(self):
        for key in ('food_rate', 'td_hotel', 'rma'):
            for field, value in (('island_percent', 100), ('local_move_divisor', 3)):
                d = fresh()
                d['figures'][key][field] = value
                self.assertTrue(any(key in p and field in p for p in problems(d)), (key, field))

    def test_a_figure_may_leave_both_out_and_the_app_then_offers_only_the_normal_case(self):
        d = fresh()
        del d['figures']['ctg']['island_percent']
        del d['figures']['ctg']['local_move_divisor']
        self.assertEqual(problems(d), [])
        self.assertNotIn('island_percent', figures.for_bundle(d)['figures']['ctg'])


if __name__ == '__main__':
    unittest.main()

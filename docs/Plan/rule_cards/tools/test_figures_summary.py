"""Tests for the TD summary card's CEA and hostel-subsidy figures in figures.json (plan 24, P1): cea, hostel_subsidy.

Why they matter: the summary card prints these two amounts to every officer, so each must be approved, dated, honestly labelled, and
its base times the DA step must equal what PCDA(O) itself prints (FAQ, Dec 2025: CEA 33,750 and hostel 1,01,250 a year per child).
They belong to no card and no pre-claim worksheet, so they carry their own `use`. Run: python3 tools/test_figures_summary.py
"""
import unittest

import figures
from test_figures import REAL, SUMMARY, fresh, problems

# What the PCDA(O) FAQ prints at DA 50 to 99%, per child per academic year.
FAQ_AMOUNT = {'cea': 33750, 'hostel_subsidy': 101250}


class SummaryFigures(unittest.TestCase):
    def test_each_is_approved_dated_and_honestly_labelled(self):
        for key in SUMMARY:
            fig = REAL['figures'][key]
            self.assertEqual(fig['approved'], '2026-11-07', key)
            self.assertEqual(fig['use'], 'summary', key)
            self.assertNotIn('card', fig, key)
            self.assertIn('1(23)/2017/D(Pay/Services)', fig['authority'], key)
            # The MoD letter text itself was not read; the handbook prints the base and the 25% rise.
            self.assertEqual(fig['evidence'], 'HANDBOOK_ONLY', key)

    def test_the_base_times_the_da_step_is_what_pcdao_prints(self):
        step = REAL['da_step']
        multiplier = 100 + step['increase_percent'] * (50 // step['per_da_percent'])
        for key, printed in FAQ_AMOUNT.items():
            bands = REAL['figures'][key]['bands']
            self.assertEqual(len(bands), 1, key)
            self.assertEqual(bands[0]['base'] * multiplier // 100, printed, key)

    def test_the_bases_are_the_handbook_monthly_rates_for_a_year(self):
        # P&A Handbook 2023 ch. 19: CEA Rs 2,250 and hostel ceiling Rs 6,750 a month, so 27,000 and 81,000 a year before the step.
        self.assertEqual({k: REAL['figures'][k]['bands'][0]['base'] for k in SUMMARY}, {'cea': 27000, 'hostel_subsidy': 81000})
        self.assertEqual(REAL['figures']['cea']['bands'][0]['base'] * 3, REAL['figures']['hostel_subsidy']['bands'][0]['base'])

    def test_the_real_file_is_still_valid(self):
        self.assertEqual(problems(REAL), [])

    def test_a_summary_figure_may_not_name_a_card(self):
        d = fresh()
        d['figures']['cea']['card'] = 'RB-SS-T181'
        self.assertTrue(any('cea' in p and 'card' in p for p in problems(d)))

    def test_a_card_less_figure_with_an_unknown_use_fails(self):
        d = fresh()
        d['figures']['cea']['use'] = 'worksheet'
        self.assertTrue(any('cea' in p and 'use' in p for p in problems(d)))

    def test_a_card_less_figure_with_no_use_fails(self):
        d = fresh()
        del d['figures']['hostel_subsidy']['use']
        self.assertTrue(any('hostel_subsidy' in p and 'use' in p for p in problems(d)))

    def test_an_unapproved_summary_figure_blocks_the_bundle(self):
        d = fresh()
        d['figures']['cea']['approved'] = None
        self.assertEqual(figures.unapproved(d), ['cea'])

    def test_they_ship_without_a_card_and_without_the_private_notes(self):
        shipped = figures.for_bundle(REAL)['figures']
        for key in SUMMARY:
            self.assertEqual(shipped[key]['use'], 'summary', key)
            self.assertNotIn('card', shipped[key], key)
            for private in ('authority', 'evidence', 'evidence_detail', 'approved'):
                self.assertNotIn(private, shipped[key], key)


if __name__ == '__main__':
    unittest.main()

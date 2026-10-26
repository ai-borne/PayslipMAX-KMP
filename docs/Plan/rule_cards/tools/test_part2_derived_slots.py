"""Tests for the slots Part II content batch 3 needed (Part II plan, phase 8): a `derived` slot, whose value the app works out with a named
derive rule from the slots it `reads`, and a `years` slot, a year-by-year list the officer types (the ENCASH break-up).

Why they matter: the accumulation of annual leave is arithmetic (previous total, less leave encashed on LTC, plus the year's leave, capped at
300), and the cap and the sum must live in one place in Kotlin, not be retyped into a template. A template that wires a derived slot wrongly
(an unknown derive rule, no inputs, a ghost input) would print a hole or a guess in the order a clerk types into HRMS, so the format refuses
each wrong wiring. Invented content only (part2_synthetic.py).
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_derived_slots.py'
"""
import copy
import unittest

import part2_synthetic as synthetic
import part2_templates

CODES = synthetic.codes()
S = synthetic._slot
DERIVE = {'id': 'sum_it', 'kind': 'derive', 'basis': 'manual', 'page': 133, 'note': '(a)'}


def delta(extra_slots, rules=None, **changes):
    """Problems of DELTANOTE with extra slots, field changes and an extra derive rule declared (its own slots stay, so it starts valid)."""
    templates = copy.deepcopy(synthetic.templates())
    t = next(x for x in templates if x['code'] == 'DELTANOTE')
    t['slots'] += extra_slots
    t.update(changes)
    data = copy.deepcopy(synthetic.rules())
    data['rules'] += rules if rules is not None else [copy.deepcopy(DERIVE)]
    return part2_templates.problems(templates, data, CODES)


INPUTS = [S('first', 'number', 'First figure', 'The first figure.'), S('second', 'number', 'Second figure', 'The second figure.')]


def total(**extra):
    return S('total', 'derived', 'Total', 'Worked out for you.', **{'by': 'sum_it', 'reads': ['first', 'second'], **extra})


class DerivedSlots(unittest.TestCase):
    def test_a_derived_slot_reading_declared_inputs_is_valid_and_counts_as_using_them(self):
        self.assertEqual(delta(INPUTS + [total()], narrative='Noted {noted_on} {remark} total {total}'), [])

    def test_a_derive_rule_is_a_third_kind_with_no_severity(self):
        self.assertEqual(delta([], rules=[{'id': 'sum_it', 'kind': 'derive', 'basis': 'practice'}]), [])
        found = delta([], rules=[{'id': 'sum_it', 'kind': 'derive', 'severity': 'warn', 'basis': 'manual', 'page': 133}])
        self.assertIn('rule sum_it: a derive rule has no severity', found)

    def test_the_derived_slot_must_name_a_derive_rule(self):
        # why: an unknown or wrong-kind rule would leave the slot with no way to be worked out.
        for by, message in (('ghost', 'by names an unknown rule'), ('cap_days', 'by must name a derive rule')):
            found = delta(INPUTS + [total(by=by)], narrative='Noted {noted_on} {remark} total {total}')
            self.assertIn(f'template DELTANOTE: slot total: {message}', found, by)
        gone = {k: v for k, v in total().items() if k != 'by'}
        self.assertIn('template DELTANOTE: slot total: a derived slot needs by', delta(INPUTS + [gone], narrative='Noted {noted_on} {remark} total {total}'))

    def test_the_derived_slot_must_read_declared_plain_slots(self):
        text = 'Noted {noted_on} {remark} total {total}'
        self.assertIn('template DELTANOTE: slot total: reads must name declared slots', delta(INPUTS + [total(reads=[])], narrative=text))
        self.assertIn('template DELTANOTE: slot total: reads must name declared slots', delta(INPUTS + [total(reads=['first', 'ghost'])], narrative=text))
        self.assertIn('template DELTANOTE: slot total: reads must name declared slots', delta(INPUTS + [total(reads=['total'])], narrative=text), 'a slot cannot read itself')
        two = [S('other', 'derived', 'Other', 'Worked out.', **{'by': 'sum_it', 'reads': ['total']})]
        self.assertIn('template DELTANOTE: slot other: reads must name declared slots', delta(INPUTS + [total()] + two, narrative='Noted {noted_on} {remark} {total} {other}'), 'no chains of derived slots')

    def test_by_and_reads_belong_to_derived_slots_only(self):
        text = 'Noted {noted_on} {remark} {first} {second}'
        self.assertIn('template DELTANOTE: slot first: by is only for a derived slot', delta([INPUTS[0] | {'by': 'sum_it'}, INPUTS[1]], narrative=text))
        self.assertIn('template DELTANOTE: slot first: reads is only for a derived slot', delta([INPUTS[0] | {'reads': ['second']}, INPUTS[1]], narrative=text))

    def test_a_derived_slot_takes_none_of_the_other_slot_features(self):
        text = 'Noted {noted_on} {remark} total {total}'
        for extra in ({'between': ['first', 'second']}, {'if_missing': 'Nil'}, {'for': 'cap_days'}):
            found = delta(INPUTS + [total(**extra)], narrative=text, rules=[copy.deepcopy(DERIVE)])
            self.assertTrue(any('slot total' in line and key in line for line in found for key in extra), (extra, found))

    def test_an_input_nothing_reads_or_prints_is_still_refused(self):
        self.assertIn('template DELTANOTE: slot second declared but never used', delta(INPUTS + [total(reads=['first'])], narrative='Noted {noted_on} {remark} total {total} {first}'))

    def test_a_derived_slot_is_not_asked_and_may_be_used_in_a_column(self):
        data = [{'need': 'required', 'slot': 'total'}, {'need': 'none'}, {'need': 'none'}, {'need': 'none'}]
        self.assertEqual(delta(INPUTS + [total()], data=data), [])


class YearsSlots(unittest.TestCase):
    def test_a_years_slot_is_a_plain_input_a_derived_slot_may_read(self):
        years = S('breakup', 'years', 'Year by year', 'One line per year.')
        reader = S('sum', 'derived', 'Sum', 'Worked out.', **{'by': 'sum_it', 'reads': ['breakup']})
        self.assertEqual(delta([years, reader], narrative='Noted {noted_on} {remark} sum {sum}'), [])

    def test_a_years_slot_nothing_reads_is_refused_and_takes_no_optional_features(self):
        years = S('breakup', 'years', 'Year by year', 'One line per year.')
        self.assertIn('template DELTANOTE: slot breakup declared but never used', delta([years]))
        self.assertIn('template DELTANOTE: slot breakup: if_missing is only for a date, number or text slot',
                      delta([S('breakup', 'years', 'Year by year', 'One line per year.', if_missing='Nil')], narrative='Noted {noted_on} {remark} {breakup}'))
        self.assertIn('template DELTANOTE: slot breakup: only a days or computed slot has between',
                      delta([S('breakup', 'years', 'Year by year', 'One line per year.', between=['noted_on'])], narrative='Noted {noted_on} {remark} {breakup}'))


if __name__ == '__main__':
    unittest.main()

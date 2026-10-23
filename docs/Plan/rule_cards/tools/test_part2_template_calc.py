"""Tests for the template features Part II content batch 1 needed (Part II plan, phase 2b): computed date slots, optional answers, check-only
slots and composite column text.

Why they matter: the manual's rows are built from dates the officer never types directly (the first day away is the earlier of the first
prefix day and the leave start; the rejoining day follows the later of the leave end and the last suffix day), from answers that may be
absent ('Nil' when there is no prefix), from inputs only a check reads (leave already taken this year) and from columns that join words
and dates (Data 4 'Part II order no and date'). A template that wires any of these wrongly would print a wrong date or a hole in the order
a clerk types into HRMS, so the format refuses each wrong wiring. Invented content only (part2_synthetic.py).
Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_template_calc.py'
"""
import copy
import unittest

import part2_synthetic as synthetic
import part2_templates

CODES = synthetic.codes()
RULES = synthetic.rules()
TEMPLATES = synthetic.templates()
S = synthetic._slot


def delta(extra_slots, **changes):
    """Problems of DELTANOTE with extra slots and field changes (its own slots stay, so it starts valid)."""
    templates = copy.deepcopy(TEMPLATES)
    t = next(x for x in templates if x['code'] == 'DELTANOTE')
    t['slots'] += extra_slots
    t.update(changes)
    return part2_templates.problems(templates, copy.deepcopy(RULES), CODES)


def calc(kind, between, sid='calc'):
    return S(sid, kind, 'Worked out', 'Worked out from other dates.', between=between)


DATES = [S('d1', 'date', 'One', 'A first date.'), S('d2', 'date', 'Two', 'A second date.')]


class ComputedDates(unittest.TestCase):
    def test_earliest_latest_and_next_day_are_valid_and_count_as_using_their_inputs(self):
        slots = DATES + [calc('earliest', ['d1', 'd2'], 'first'), calc('latest', ['d1', 'd2'], 'last'), calc('next_day', ['last'], 'after')]
        self.assertEqual(delta(slots, narrative='Noted {noted_on} as {remark} from {first} to {last}, back {after}'), [])

    def test_a_computed_slot_may_name_the_from_and_to_columns(self):
        slots = DATES + [calc('earliest', ['d1', 'd2'], 'first'), calc('next_day', ['first'], 'after')]
        self.assertEqual(delta(slots, **{'from': {'need': 'required', 'slot': 'first'}, 'to': {'need': 'required', 'slot': 'after'}}), [])

    def test_a_computed_slot_must_name_declared_date_valued_slots(self):
        # why: a ghost or a non-date input would make the engine compute a date from nothing and print it.
        ghost = delta(DATES + [calc('earliest', ['d1', 'ghost'])], narrative='Noted {noted_on} {remark} {calc}')
        self.assertIn('template DELTANOTE: slot calc: between must name 2 to 4 different date slots', ghost)
        text = delta(DATES + [calc('latest', ['d1', 'remark'])], narrative='Noted {noted_on} {remark} {calc}')
        self.assertIn('template DELTANOTE: slot calc: between must name 2 to 4 different date slots', text)

    def test_the_number_of_inputs_fits_the_kind(self):
        self.assertIn('template DELTANOTE: slot calc: between must name 2 to 4 different date slots',
                      delta(DATES + [calc('earliest', ['d1'])], narrative='Noted {noted_on} {remark} {calc}'))
        self.assertIn('template DELTANOTE: slot calc: between must name 2 to 4 different date slots',
                      delta(DATES + [calc('earliest', ['d1', 'd1'])], narrative='Noted {noted_on} {remark} {calc}'))
        self.assertIn('template DELTANOTE: slot calc: between must name one date slot',
                      delta(DATES + [calc('next_day', ['d1', 'd2'])], narrative='Noted {noted_on} {remark} {calc}'))

    def test_a_computed_slot_may_only_read_slots_declared_before_it(self):
        # why: this is what makes a loop (a reads b reads a) impossible, so the engine never has to detect one.
        loop = [calc('next_day', ['later'], 'early'), calc('next_day', ['early'], 'later')]
        self.assertIn('template DELTANOTE: slot early: between must name one date slot',
                      delta(DATES + loop, narrative='Noted {noted_on} {remark} {early} {later}'))

    def test_only_computed_and_days_slots_have_between(self):
        self.assertIn('template DELTANOTE: slot remark: only a days or computed slot has between',
                      delta([], slots=[S('noted_on', 'date', 'Noted on', 'The day it was noted.'), S('remark', 'text', 'Remark', 'Words.', between=['noted_on', 'noted_on'])]))

    def test_the_kind_list_names_the_computed_kinds(self):
        self.assertIn('template DELTANOTE: slot remark: kind must be date, number, text, days, ref, earliest, latest or next_day',
                      delta([], slots=[S('noted_on', 'date', 'Noted on', 'The day.'), S('remark', 'money', 'Remark', 'Words.')]))


class OptionalAnswers(unittest.TestCase):
    def test_if_missing_makes_a_date_number_or_text_optional_and_is_bounded_plain_text(self):
        for kind in ('date', 'number', 'text'):
            slot = S('extra', kind, 'Extra', 'An optional answer.', if_missing='Nil')
            self.assertEqual(delta([slot], narrative='Noted {noted_on} as {remark} with {extra}'), [], kind)

    def test_if_missing_is_refused_where_there_is_nothing_to_leave_out(self):
        for kind, extra in (('ref', {}), ('days', {'between': ['d1', 'd2']}), ('earliest', {'between': ['d1', 'd2']})):
            slot = S('extra', kind, 'Extra', 'An answer.', if_missing='Nil', **extra)
            found = delta(DATES + [slot], narrative='Noted {noted_on} as {remark} with {extra}' if kind != 'ref' else 'Noted {noted_on} {remark} {extra.no}')
            self.assertIn('template DELTANOTE: slot extra: if_missing is only for a date, number or text slot', found, kind)

    def test_if_missing_text_is_plain_and_short(self):
        for bad, message in (('<b>', 'has angle brackets (plain text only)'), ('', 'is empty'), ('x' * (RULES['limits']['note'] + 1), f"is over {RULES['limits']['note']} characters")):
            found = delta([S('extra', 'date', 'Extra', 'Optional.', if_missing=bad)], narrative='Noted {noted_on} {remark} {extra}')
            self.assertIn(f'template DELTANOTE: slot extra: if_missing {message}', found, bad)


class CheckOnlySlots(unittest.TestCase):
    def test_a_slot_for_a_rule_the_template_runs_needs_no_other_use(self):
        slot = S('earlier', 'number', 'Taken already', 'Days already taken.', **{'for': 'cap_days'})
        self.assertEqual(delta([slot], rules=['cap_days']), [])

    def test_a_slot_for_a_rule_the_template_does_not_run_is_refused(self):
        # why: an input nobody reads is a question the officer answers for nothing.
        slot = S('earlier', 'number', 'Taken already', 'Days already taken.', **{'for': 'grant_quoted'})
        self.assertIn('template DELTANOTE: slot earlier: for names a rule the template does not run', delta([slot], rules=['cap_days']))
        self.assertIn('template DELTANOTE: slot earlier: for names a rule the template does not run', delta([slot]))

    def test_a_slot_for_a_rule_is_a_plain_answer_not_a_computed_one(self):
        slot = S('calc', 'next_day', 'Worked out', 'Worked out.', between=['noted_on'], **{'for': 'cap_days'})
        self.assertIn('template DELTANOTE: slot calc: for is only for a date, number or text slot', delta([slot], rules=['cap_days']))


class ColumnText(unittest.TestCase):
    def column(self, text, need='required', where=('data', 0)):
        templates = copy.deepcopy(TEMPLATES)
        t = next(x for x in templates if x['code'] == 'DELTANOTE')
        col = {'need': need, 'text': text}
        if where[0] == 'data':
            t['data'][where[1]] = col
        else:
            t[where[0]] = col
        return part2_templates.problems(templates, copy.deepcopy(RULES), CODES)

    def test_a_column_may_join_words_and_slots(self):
        self.assertEqual(self.column('{remark} on {noted_on}'), [])
        self.assertEqual(self.column('R1H1', where=('data', 0)), [], 'a fixed word needs no slot')

    def test_text_counts_as_using_the_slots_it_names(self):
        self.assertEqual(self.column('{remark}'), [])

    def test_a_column_takes_a_slot_or_text_never_both_and_not_when_empty(self):
        found = part2_templates.problems(self._with({'need': 'required', 'slot': 'remark', 'text': 'x {remark}'}), copy.deepcopy(RULES), CODES)
        self.assertIn('template DELTANOTE: column data1 names a slot and text, use one', found)
        self.assertIn('template DELTANOTE: column data1 is not required but has text', self.column('x {remark}', need='none'))

    def _with(self, column):
        templates = copy.deepcopy(TEMPLATES)
        next(x for x in templates if x['code'] == 'DELTANOTE')['data'][0] = column
        return templates

    def test_the_from_and_to_columns_are_dates_so_they_take_no_text(self):
        self.assertIn('template DELTANOTE: column from must name a date slot, not text', self.column('{noted_on}', where=('from', 0)))

    def test_text_is_held_to_the_placeholder_rules_and_the_text_limit(self):
        self.assertIn('template DELTANOTE: column data1 has a malformed placeholder', self.column('{remark'))
        self.assertIn('template DELTANOTE: slot ghost used but not declared', self.column('{remark} {ghost}'))
        self.assertIn('template DELTANOTE: column data1 has angle brackets (plain text only)', self.column('<b>{remark}</b>'))
        self.assertIn('template DELTANOTE: column data1 is over 300 characters', self.column('{remark} ' + 'x' * 300))

    def test_an_order_reference_is_used_by_part_in_column_text_too(self):
        templates = copy.deepcopy(TEMPLATES)
        t = next(x for x in templates if x['code'] == 'REJOINAL')
        t['data'][0] = {'need': 'required', 'text': '{grant}'}
        found = part2_templates.problems(templates, copy.deepcopy(RULES), CODES)
        self.assertIn('template REJOINAL: slot grant is a reference, so use grant.no or grant.date', found)


class Shipping(unittest.TestCase):
    def test_the_new_slot_and_column_keys_ship_and_change_the_revision(self):
        templates = copy.deepcopy(TEMPLATES)
        t = next(x for x in templates if x['code'] == 'DELTANOTE')
        base = part2_templates.rev(t)
        t['slots'].append(S('extra', 'text', 'Extra', 'Optional.', if_missing='Nil'))
        t['narrative'] += ' {extra}'
        t['data'][1] = {'need': 'required', 'text': 'on {noted_on}'}
        shipped = part2_templates.for_bundle(templates, RULES, CODES)
        out = next(x for x in shipped['templates'] if x['code'] == 'DELTANOTE')
        self.assertEqual(out['slots'][-1]['if_missing'], 'Nil')
        self.assertEqual(out['data'][1], {'need': 'required', 'text': 'on {noted_on}'})
        self.assertNotEqual(base, out['rev'])

    def test_column_text_is_the_manuals_own_wording_so_copycheck_does_not_read_it(self):
        templates = copy.deepcopy(TEMPLATES)
        next(x for x in templates if x['code'] == 'DELTANOTE')['data'][1] = {'need': 'required', 'text': 'Words from the manual {noted_on}'}
        self.assertFalse(any('Words from the manual' in text for text in part2_templates.texts(templates)))


if __name__ == '__main__':
    unittest.main()

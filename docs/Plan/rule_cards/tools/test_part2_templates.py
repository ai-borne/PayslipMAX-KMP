"""Tests for part2_templates.py, the format and checks for the hand-written Part II order templates (Part II plan, phase 2a).

Why they matter: a template is what turns an officer's answers into the rows a clerk types into HRMS, so a template that names a
code the manual does not have, fills a column the manual says is not required, cites no page, or lets a rule id typo through would
put a wrong or uncited row on a printed slip. These tests use invented templates (part2_synthetic.py); the real content arrives in
phase 2b. Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_templates.py'
"""
import copy
import json
import os
import unittest

import copycheck
import part2_rules
import part2_synthetic as synthetic
import part2_templates

CODES = synthetic.codes()
RULES = synthetic.rules()
TEMPLATES = synthetic.templates()


def template(code, templates=None):
    return next(t for t in (templates or TEMPLATES) if t['code'] == code)


def broken(edit, rules=None):
    """Problems after `edit(templates, rules)` has damaged deep copies of the synthetic content."""
    templates, rule_data = copy.deepcopy(TEMPLATES), copy.deepcopy(RULES if rules is None else rules)
    edit(templates, rule_data)
    return part2_templates.problems(templates, rule_data, CODES)


class SyntheticContent(unittest.TestCase):
    def test_the_synthetic_content_is_valid_and_exercises_every_feature(self):
        self.assertEqual(part2_templates.problems(TEMPLATES, RULES, CODES), [])
        severities = {r['severity'] for r in RULES['rules'] if r['kind'] == 'check'}
        self.assertEqual(severities, {'warn', 'ack'}, 'the rule model must carry both severities (owner decision, phase 1)')
        self.assertEqual({r['basis'] for r in RULES['rules']}, {'manual', 'practice'})
        self.assertTrue(any(t.get('authority') for t in TEMPLATES) and any(t.get('certificate') for t in TEMPLATES))

    def test_the_real_rules_and_templates_are_valid(self):
        self.assertEqual(part2_templates.check(), [])


class Codes(unittest.TestCase):
    def test_a_template_for_an_unknown_code_is_refused(self):
        found = broken(lambda ts, rs: template('DELTANOTE', ts).update(code='NOSUCHCODE'))
        self.assertIn('template NOSUCHCODE: unknown code', found)

    def test_two_templates_for_one_code_are_refused(self):
        found = broken(lambda ts, rs: ts.append(copy.deepcopy(template('DELTANOTE', ts))))
        self.assertIn('template DELTANOTE: duplicate template', found)

    def test_a_printed_code_override_other_than_rejoinal_to_rejoin_is_refused(self):
        # why: the slip prints REJOIN as HRMS does (Round 9); any other renamed code would hide the manual's own code (Round 2).
        self.assertIn('template DELTANOTE: print override must be REJOINAL to REJOIN',
                      broken(lambda ts, rs: template('DELTANOTE', ts).update(print='NOTE')))
        self.assertIn('template REJOINAL: print override must be REJOINAL to REJOIN',
                      broken(lambda ts, rs: template('REJOINAL', ts).update(print='REJOINING')))
        self.assertEqual(broken(lambda ts, rs: template('REJOINAL', ts).pop('print')), [], 'the override is optional')

    def test_an_unknown_key_is_refused_so_a_typo_cannot_silently_drop_a_field(self):
        found = broken(lambda ts, rs: template('DELTANOTE', ts).update(narative='typo'))
        self.assertIn('template DELTANOTE: unknown key narative', found)


class Citations(unittest.TestCase):
    def test_a_missing_or_impossible_page_citation_is_refused(self):
        self.assertIn('template DELTANOTE: page must be a whole number from 1 to 400', broken(lambda ts, rs: template('DELTANOTE', ts).pop('page')))
        self.assertIn('template DELTANOTE: page must be a whole number from 1 to 400', broken(lambda ts, rs: template('DELTANOTE', ts).update(page=401)))

    def test_a_link_without_a_manual_or_practice_basis_is_refused(self):
        found = broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][0].pop('basis'))
        self.assertIn('template ALPHAGRANT: link to BETARATION: basis must be manual or practice', found)
        found = broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][0].update(basis='hearsay'))
        self.assertIn('template ALPHAGRANT: link to BETARATION: basis must be manual or practice', found)

    def test_a_manual_link_needs_a_page_and_a_practice_link_takes_none(self):
        found = broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][0].pop('page'))
        self.assertIn('template ALPHAGRANT: link to BETARATION: a manual link needs a page', found)
        found = broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][1].update(page=12))
        self.assertIn('template ALPHAGRANT: link to BETASTOP: a practice link cites no page', found)

    def test_a_link_to_an_unknown_code_or_to_itself_is_refused(self):
        self.assertIn('template ALPHAGRANT: link to NOSUCHCODE: unknown code',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][1].update(code='NOSUCHCODE')))
        self.assertIn('template ALPHAGRANT: link to ALPHAGRANT: a template cannot link to itself',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][1].update(code='ALPHAGRANT')))


class Rules(unittest.TestCase):
    def test_an_unknown_rule_id_is_refused_in_a_check_and_in_a_link_condition(self):
        self.assertIn('template ALPHAGRANT: rule nosuchrule is unknown',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts).update(rules=['nosuchrule'])))
        self.assertIn('template ALPHAGRANT: link to BETARATION: condition nosuchrule is unknown',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][0].update(when='nosuchrule')))

    def test_a_check_cannot_stand_in_for_a_condition_or_the_other_way_round(self):
        self.assertIn('template ALPHAGRANT: rule on_ration is not a check',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts).update(rules=['on_ration'])))
        self.assertIn('template ALPHAGRANT: link to BETARATION: condition cap_days is not a condition',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts)['links'][0].update(when='cap_days')))

    def test_a_template_cannot_repeat_a_rule_or_exceed_the_limit(self):
        self.assertIn('template ALPHAGRANT: rule cap_days repeats',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts).update(rules=['cap_days', 'cap_days'])))
        self.assertIn('template ALPHAGRANT: has 5 rules, max 4',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts).update(rules=['cap_days', 'grant_quoted', 'a', 'b', 'c'])))


class Slots(unittest.TestCase):
    def test_a_slot_used_but_not_declared_is_refused_wherever_it_is_used(self):
        self.assertIn('template DELTANOTE: slot ghost used but not declared',
                      broken(lambda ts, rs: template('DELTANOTE', ts).update(narrative='Noted on {noted_on} as {ghost}')))
        self.assertIn('template DELTANOTE: slot ghost used but not declared',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['data'].__setitem__(1, {'need': 'required', 'slot': 'ghost'})))
        self.assertIn('template DELTANOTE: slot ghost used but not declared',
                      broken(lambda ts, rs: template('DELTANOTE', ts).update(authority='By {ghost}')))
        self.assertIn('template DELTANOTE: slot ghost used but not declared',
                      broken(lambda ts, rs: template('DELTANOTE', ts).update(certificate='Seen {ghost}')))

    def test_a_declared_slot_nothing_uses_is_refused(self):
        found = broken(lambda ts, rs: template('DELTANOTE', ts)['slots'].append(synthetic._slot('spare', 'text', 'Spare', 'Not used anywhere.')))
        self.assertIn('template DELTANOTE: slot spare declared but never used', found)

    def test_a_filled_not_required_column_is_refused(self):
        # why: Annexure III marks columns the unit must leave empty; a value there is a row HRMS rejects.
        found = broken(lambda ts, rs: template('DELTANOTE', ts)['data'].__setitem__(1, {'need': 'none', 'slot': 'remark'}))
        self.assertIn('template DELTANOTE: column data2 is not required but names a slot', found)

    def test_a_required_or_optional_column_must_name_a_slot(self):
        self.assertIn('template DELTANOTE: column from needs a slot', broken(lambda ts, rs: template('DELTANOTE', ts).update({'from': {'need': 'required'}})))
        self.assertIn('template DELTANOTE: column data1 needs a slot',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['data'].__setitem__(0, {'need': 'optional'})))
        self.assertIn('template DELTANOTE: column data1 need must be required, optional or none',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['data'].__setitem__(0, {'need': 'maybe', 'slot': 'remark'})))

    def test_data_is_exactly_four_columns_and_from_to_are_dates(self):
        self.assertIn('template DELTANOTE: data must have exactly 4 columns', broken(lambda ts, rs: template('DELTANOTE', ts)['data'].pop()))
        self.assertIn('template DELTANOTE: column from must name a date slot',
                      broken(lambda ts, rs: template('DELTANOTE', ts).update({'from': {'need': 'required', 'slot': 'remark'}})))

    def test_a_slot_id_is_well_formed_and_unique_and_its_kind_is_known(self):
        self.assertIn('template DELTANOTE: slot Remark: id must be lowercase letters, digits and underscores',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][1].update(id='Remark')))
        self.assertIn('template DELTANOTE: duplicate slot id remark',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][0].update(id='remark')))
        self.assertIn('template DELTANOTE: slot remark: kind must be date, number, text, days, ref, years, derived, earliest, latest or next_day',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][1].update(kind='money')))

    def test_a_days_slot_counts_between_two_declared_date_slots(self):
        self.assertIn('template ALPHAGRANT: slot days: between must name two different date slots',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts)['slots'][2].update(between=['leave_from'])))
        self.assertIn('template ALPHAGRANT: slot days: between must name two different date slots',
                      broken(lambda ts, rs: template('ALPHAGRANT', ts)['slots'][2].update(between=['leave_from', 'year'])))
        self.assertIn('template DELTANOTE: slot remark: only a days or computed slot has between',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][1].update(between=['noted_on', 'noted_on'])))

    def test_an_order_reference_is_used_by_its_number_or_its_date_and_nothing_else_is(self):
        self.assertIn('template REJOINAL: slot grant is a reference, so use grant.no or grant.date',
                      broken(lambda ts, rs: template('REJOINAL', ts).update(narrative='Rejoined {rejoin_on} vide {grant}')))
        self.assertIn('template DELTANOTE: slot remark is not a reference, so it has no .no',
                      broken(lambda ts, rs: template('DELTANOTE', ts).update(narrative='Noted {noted_on} as {remark.no}')))

    def test_one_slot_id_means_the_same_kind_in_every_template(self):
        # why: the officer answers each id once and every linked template reads that answer.
        def clash(ts, rs):
            delta = template('DELTANOTE', ts)
            delta['slots'].append(synthetic._slot('leave_from', 'text', 'Other', 'Same id as a date elsewhere.'))
            delta['narrative'] += ' {leave_from}'
        self.assertIn('slot leave_from: declared with different kinds (date, text)', broken(clash))


class Text(unittest.TestCase):
    def test_angle_brackets_are_refused_in_every_shipped_text(self):
        for field in ('narrative', 'authority', 'certificate'):
            found = broken(lambda ts, rs, f=field: template('DELTANOTE', ts).update({f: 'Noted <b>{noted_on}</b> {remark}'}))
            self.assertIn(f'template DELTANOTE: {field} has angle brackets (plain text only)', found)
        self.assertIn('template DELTANOTE: slot remark: label has angle brackets (plain text only)',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][1].update(label='<i>Remark</i>')))
        self.assertIn('template DELTANOTE: slot remark: help has angle brackets (plain text only)',
                      broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][1].update(help='<script>')))

    def test_an_empty_or_over_long_text_is_refused_using_the_shipped_limits(self):
        self.assertIn('template DELTANOTE: narrative is empty', broken(lambda ts, rs: template('DELTANOTE', ts).update(narrative=' ')))
        self.assertIn(f"template DELTANOTE: narrative is over {LIMIT('narrative')} characters",
                      broken(lambda ts, rs: template('DELTANOTE', ts).update(narrative='{noted_on} {remark} ' + 'x' * LIMIT('narrative'))))
        self.assertIn(f"template DELTANOTE: slot remark: help is over {LIMIT('help')} characters",
                      broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][1].update(help='x' * (LIMIT('help') + 1))))
        self.assertIn('template DELTANOTE: slot remark: label is empty', broken(lambda ts, rs: template('DELTANOTE', ts)['slots'][1].update(label='')))

    def test_a_placeholder_must_be_closed_and_named(self):
        for bad in ('Noted {noted_on', 'Noted noted_on} {remark}', 'Noted {} {remark}', 'Noted {{noted_on}} {remark}'):
            found = broken(lambda ts, rs, b=bad: template('DELTANOTE', ts).update(narrative=b))
            self.assertIn('template DELTANOTE: narrative has a malformed placeholder', found, bad)

    def test_a_template_cannot_exceed_the_slot_and_link_limits(self):
        many = [synthetic._slot(f's{i}', 'text', 'L', 'H') for i in range(LIMIT('slots') + 1)]
        found = broken(lambda ts, rs: template('DELTANOTE', ts).update(slots=many))
        self.assertIn(f"template DELTANOTE: has {LIMIT('slots') + 1} slots, max {LIMIT('slots')}", found)
        links = [{'code': 'BETASTOP', 'basis': 'practice'}] * (LIMIT('links') + 1)
        self.assertIn(f"template DELTANOTE: has {LIMIT('links') + 1} links, max {LIMIT('links')}", broken(lambda ts, rs: template('DELTANOTE', ts).update(links=links)))


def LIMIT(key):
    return RULES['limits'][key]


class Copycheck(unittest.TestCase):
    def test_the_texts_checked_for_copying_are_the_help_and_labels_not_the_manuals_narratives(self):
        # why: narratives keep the manual's own wording on purpose (Round 10), so the own-words guard skips them; help text is ours.
        texts = part2_templates.texts(TEMPLATES)
        self.assertIn('The first day of the invented thing.', texts)
        self.assertIn('Counted for you, both days included.', texts)
        joined = ' '.join(texts)
        for narrative in ('Granted the first thing for', 'Certified that the second thing', 'Sanction {sanction.no}'):
            self.assertNotIn(narrative, joined)

    def test_copycheck_asks_for_those_texts(self):
        self.assertEqual(copycheck.part2_texts(TEMPLATES), part2_templates.texts(TEMPLATES))
        words = copycheck.words('The first day of the invented thing and a few more plain words')
        self.assertTrue(copycheck.grams(words), 'help text long enough to be compared yields 8-grams')


class Shipping(unittest.TestCase):
    def test_the_section_ships_whitelisted_keys_with_a_revision_and_the_limits(self):
        shipped = part2_templates.for_bundle(TEMPLATES, RULES, CODES)
        self.assertEqual(sorted(shipped), ['rules', 'template_limits', 'templates'])
        self.assertEqual(shipped['template_limits'], RULES['limits'])
        self.assertEqual([t['code'] for t in shipped['templates']], [t['code'] for t in TEMPLATES], 'authored order is kept')
        for t in shipped['templates']:
            self.assertRegex(t['rev'], r'^[0-9a-f]{8}$')

    def test_the_revision_is_stable_and_changes_with_any_shipped_text(self):
        base = part2_templates.rev(template('DELTANOTE'))
        self.assertEqual(base, part2_templates.rev(copy.deepcopy(template('DELTANOTE'))))
        for edit in (lambda t: t.update(narrative=t['narrative'] + '.'), lambda t: t['slots'][1].update(help='Other words.'), lambda t: t.update(page=41)):
            changed = copy.deepcopy(template('DELTANOTE'))
            edit(changed)
            self.assertNotEqual(base, part2_templates.rev(changed))

    def test_a_template_with_problems_stops_the_build_instead_of_shipping(self):
        bad = copy.deepcopy(TEMPLATES)
        template('DELTANOTE', bad)['page'] = 0
        with self.assertRaises(SystemExit):
            part2_templates.for_bundle(bad, RULES, CODES)

    def test_the_output_is_deterministic(self):
        self.assertEqual(json.dumps(part2_templates.for_bundle(TEMPLATES, RULES, CODES)), json.dumps(part2_templates.for_bundle(TEMPLATES, RULES, CODES)))

    def test_the_committed_synthetic_section_is_current(self):
        # why: the Kotlin engine tests load this file as if the shipping tools had written it; a stale copy would test old behaviour.
        with open(synthetic.SECTION_PATH, encoding='utf-8') as fh:
            self.assertEqual(fh.read(), synthetic.render(), 'run: python3 docs/Plan/rule_cards/tools/part2_synthetic.py')

    def test_the_real_templates_folder_loads_and_each_file_has_a_version(self):
        self.assertIsInstance(part2_templates.load_templates(), list)
        self.assertEqual(part2_templates.file_problems(), [])
        self.assertTrue(os.path.isfile(part2_rules.RULES_PATH))


if __name__ == '__main__':
    unittest.main()

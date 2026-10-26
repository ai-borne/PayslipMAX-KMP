"""Tests for part2_rules.py and part2/rules.json, the declarations behind the Kotlin rule registry (Part II plan, phase 2a).

Why they matter: a rule's severity decides whether the officer can print past it (`ack`: the manual says it will be rejected, so they
must acknowledge it) or merely sees it (`warn`), and its page or `practice` label is what keeps an app opinion from reading as the
manual's. Run: python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_part2_rules.py'
"""
import copy
import unittest

import part2_rules
import part2_synthetic as synthetic

RULES = synthetic.rules()


class RuleDeclarations(unittest.TestCase):
    def rule_problems(self, edit):
        rule_data = copy.deepcopy(RULES)
        edit(rule_data)
        return part2_rules.rule_problems(rule_data)

    def test_both_severities_are_accepted_and_nothing_else(self):
        self.assertEqual(self.rule_problems(lambda d: d['rules'][1].update(severity='ack')), [])
        self.assertIn('rule cap_days: a check needs severity warn or ack', self.rule_problems(lambda d: d['rules'][0].update(severity='error')))
        self.assertIn('rule cap_days: a check needs severity warn or ack', self.rule_problems(lambda d: d['rules'][0].pop('severity')))

    def test_a_condition_has_no_severity(self):
        self.assertIn('rule on_ration: a condition has no severity', self.rule_problems(lambda d: d['rules'][2].update(severity='warn')))

    def test_a_manual_rule_cites_a_page_and_a_practice_rule_cites_none(self):
        self.assertIn('rule cap_days: a manual rule needs a page', self.rule_problems(lambda d: d['rules'][0].pop('page')))
        self.assertIn('rule in_hill_station: a practice rule cites no page', self.rule_problems(lambda d: d['rules'][3].update(page=9)))

    def test_ids_are_unique_and_well_formed_and_the_kind_is_known(self):
        self.assertIn('duplicate rule id cap_days', self.rule_problems(lambda d: d['rules'][1].update(id='cap_days')))
        self.assertIn('rule Bad-Id: id must be lowercase letters, digits and underscores', self.rule_problems(lambda d: d['rules'][1].update(id='Bad-Id')))
        self.assertIn('rule cap_days: kind must be check, condition or derive', self.rule_problems(lambda d: d['rules'][0].update(kind='hint')))

    def test_the_limits_are_exactly_the_agreed_keys_and_positive(self):
        self.assertTrue(self.rule_problems(lambda d: d['limits'].pop('help')))
        self.assertTrue(self.rule_problems(lambda d: d['limits'].update(slots=0)))
        self.assertTrue(self.rule_problems(lambda d: d.update(version=2)))


class RealRules(unittest.TestCase):
    def test_the_shipped_declarations_are_valid_and_carry_every_limit_a_template_needs(self):
        real = part2_rules.load_rules()
        self.assertEqual(part2_rules.rule_problems(real), [])
        self.assertEqual(sorted(real['limits']), sorted(part2_rules.LIMIT_KEYS))


if __name__ == '__main__':
    unittest.main()

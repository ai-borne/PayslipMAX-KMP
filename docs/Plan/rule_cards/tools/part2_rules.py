"""The rules Part II templates cite (Part II plan, phase 2a): part2/rules.json, the declarations behind the Kotlin rule registry.

Each rule has an id, a kind (`check`: a finding the officer sees, severity `warn` or `ack`; `condition`: decides whether a linked order
applies; `derive`: works out the value of a derived slot from the slots it reads), and a basis: the manual page it comes from, or `practice`. The file also holds the limits every template text must respect,
which ship in the section so the app holds no second copy. Logic stays in Kotlin; a contract test keeps the two id lists equal.
part2_templates.py checks templates against these; problems() style as part2_nav.py (readable lines naming ids only).
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

RULES_PATH = os.path.join(CARDS_DIR, 'part2', 'rules.json')
SUPPORTED_VERSION = 1
ID = re.compile(r'^[a-z][a-z0-9_]{0,31}$')
LIMIT_KEYS = ('narrative', 'label', 'help', 'slots', 'links', 'rules', 'note')
RULE_KEYS = ('id', 'kind', 'severity', 'basis', 'page', 'note')
BASES = ('manual', 'practice')
PAGE_MAX = 400


def load_rules():
    with open(RULES_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def valid_page(value):
    return isinstance(value, int) and not isinstance(value, bool) and 1 <= value <= PAGE_MAX


def text_problems(where, field, value, limit):
    if not isinstance(value, str) or not value.strip():
        return [f'{where}: {field} is empty']
    errs = []
    if len(value) > limit:
        errs.append(f'{where}: {field} is over {limit} characters')
    if '<' in value or '>' in value:
        errs.append(f'{where}: {field} has angle brackets (plain text only)')
    return errs


def rule_problems(data):
    errs = []
    if data.get('version') != SUPPORTED_VERSION:
        errs.append(f'part2 rules: version must be {SUPPORTED_VERSION}')
    limits = data.get('limits') or {}
    if sorted(limits) != sorted(LIMIT_KEYS) or not all(isinstance(v, int) and not isinstance(v, bool) and v > 0 for v in limits.values()):
        return errs + [f'part2 rules: limits must be exactly {list(LIMIT_KEYS)}, each a positive whole number']
    seen = set()
    for rule in data.get('rules') or []:
        rid = rule.get('id')
        if rid in seen:
            errs.append(f'duplicate rule id {rid}')
        seen.add(rid)
        errs += _rule(rule, limits)
    return errs


def _rule(rule, limits):
    rid = rule.get('id')
    where = f'rule {rid}'
    errs = [f'{where}: unknown key {k}' for k in sorted(set(rule) - set(RULE_KEYS))]
    if not (isinstance(rid, str) and ID.match(rid)):
        errs.append(f'{where}: id must be lowercase letters, digits and underscores')
    kind = rule.get('kind')
    if kind not in ('check', 'condition', 'derive'):
        errs.append(f'{where}: kind must be check, condition or derive')
    elif kind == 'check' and rule.get('severity') not in ('warn', 'ack'):
        errs.append(f'{where}: a check needs severity warn or ack')
    elif kind == 'condition' and 'severity' in rule:
        errs.append(f'{where}: a condition has no severity')
    elif kind == 'derive' and 'severity' in rule:
        errs.append(f'{where}: a derive rule has no severity')
    page, basis = rule.get('page'), rule.get('basis')
    if basis not in BASES:
        errs.append(f'{where}: basis must be manual or practice')
    elif basis == 'manual' and not valid_page(page):
        errs.append(f'{where}: a manual rule needs a page')
    elif basis == 'practice' and page is not None:
        errs.append(f'{where}: a practice rule cites no page')
    if 'note' in rule:
        errs += text_problems(where, 'note', rule['note'], limits['note'])
    return errs

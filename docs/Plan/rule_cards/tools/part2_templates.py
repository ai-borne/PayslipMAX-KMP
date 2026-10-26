"""Validate and ship the hand-written Part II order templates and the rules they cite (Part II plan, phase 2a).

part2/rules.json (authored) declares every rule the app implements: its id, whether it is a `check` (a finding the officer sees,
severity `warn` or `ack`) or a `condition` (decides whether a linked order applies), the manual page or the word `practice` behind it,
and the limits every template text must respect. part2/templates/*.json (authored, hand-written, one file per content batch) holds one
template per code: the narrative with `{slot}` placeholders, the slots the officer answers, which columns (From, To, Data 1-4) the
manual requires, an optional authority line and certificate, the checks that run, and the linked orders. Logic lives in Kotlin and
only its ids live here; a contract test keeps the two lists equal. Narratives keep the manual's wording, so copycheck skips them and
checks `texts()` instead. compile.py calls check(); bundle.py calls for_bundle(). problems() style as part2_nav.py: a list of
readable problems naming ids only, empty means fit to use.
"""
import hashlib
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import part2_codes_check
import part2_rules
from part2_rules import BASES, LIMIT_KEYS, PAGE_MAX, RULE_KEYS, text_problems, valid_page
from part2_slots import SLOT_KEYS, column_uses, slot_table, text_uses
from config import CARDS_DIR

TEMPLATES_DIR = os.path.join(CARDS_DIR, 'part2', 'templates')
SUPPORTED_VERSION = 1
TEMPLATE_KEYS = ('code', 'print', 'page', 'narrative', 'slots', 'from', 'to', 'data', 'authority', 'certificate', 'rules', 'links')
REQUIRED_KEYS = ('code', 'page', 'narrative', 'slots', 'from', 'to', 'data')
LINK_KEYS = ('code', 'basis', 'page', 'note', 'when')
# The one code the slip prints differently from the manual's Annexure II (Round 9): HRMS publishes REJOIN.
PRINT_OVERRIDES = {'REJOINAL': 'REJOIN'}


def _template_files():
    if not os.path.isdir(TEMPLATES_DIR):
        return []
    return [os.path.join(TEMPLATES_DIR, n) for n in sorted(os.listdir(TEMPLATES_DIR)) if n.endswith('.json')]


def load_templates():
    """Every template of every batch file, in file-name then authored order. No folder means no templates yet."""
    found = []
    for path in _template_files():
        with open(path, encoding='utf-8') as fh:
            found += json.load(fh).get('templates', [])
    return found


def file_problems():
    errs = []
    for path in _template_files():
        name = os.path.basename(path)
        with open(path, encoding='utf-8') as fh:
            data = json.load(fh)
        if data.get('version') != SUPPORTED_VERSION:
            errs.append(f'templates {name}: version must be {SUPPORTED_VERSION}')
        if sorted(data) != ['templates', 'version'] or not isinstance(data.get('templates'), list):
            errs.append(f'templates {name}: must hold exactly version and a templates list')
    return errs


def check():
    """Every problem in the real rules.json and templates together. compile.py gates the build on this."""
    return file_problems() + problems(load_templates(), part2_rules.load_rules(), part2_codes_check.load())


def texts(templates):
    """The words that are ours, for the own-words copy check: slot labels and help. Narratives, authority lines and certificates keep
    the manual's wording on purpose, so they are not here."""
    return [value for t in templates for slot in t.get('slots', []) for value in (slot.get('label'), slot.get('help')) if value]


def rev(template):
    """First 8 hex characters of the SHA-256 of the shipped template, so a draft can tell it changed since it was printed."""
    text = json.dumps(_ship(template), ensure_ascii=False, sort_keys=True, separators=(',', ':'))
    return hashlib.sha256(text.encode('utf-8')).hexdigest()[:8]


def _ship(template):
    out = {k: template[k] for k in TEMPLATE_KEYS if template.get(k) is not None}
    out['slots'] = [{k: s[k] for k in SLOT_KEYS if k in s} for s in template['slots']]
    out['rules'] = list(template.get('rules') or [])
    out['links'] = [{k: link[k] for k in LINK_KEYS if k in link} for link in template.get('links') or []]
    return out


def for_bundle(templates, rules_data, codes):
    """The slice the app ships: the limits, the rule declarations and every template with its revision. Authored order is kept."""
    found = problems(templates, rules_data, codes)
    if found:
        sys.exit('part2 rules and templates are not fit to bundle:\n  ' + '\n  '.join(found))
    return {'template_limits': {k: rules_data['limits'][k] for k in LIMIT_KEYS},
            'rules': [{k: r[k] for k in RULE_KEYS if k in r} for r in rules_data['rules']],
            'templates': [{**_ship(t), 'rev': rev(t)} for t in templates]}


def problems(templates, rules_data, codes):
    errs = part2_rules.rule_problems(rules_data)
    if any(line.startswith('part2 rules: limits') for line in errs):
        return errs
    limits = rules_data['limits']
    rules = {r.get('id'): r for r in rules_data.get('rules') or []}
    known = {code for row in codes['serials'] for code in row['codes']}
    seen, kinds = set(), {}
    for t in templates:
        errs += _template(t, limits, rules, known, seen, kinds)
    errs += [f"slot {sid}: declared with different kinds ({', '.join(sorted(k))})" for sid, k in sorted(kinds.items()) if len(k) > 1]
    return errs


def _template(t, limits, rules, known, seen, kinds):
    code = t.get('code')
    where = f'template {code}'
    errs = [f'{where}: unknown key {k}' for k in sorted(set(t) - set(TEMPLATE_KEYS))]
    errs += [f'{where}: {k} is missing' for k in REQUIRED_KEYS if k not in t]
    if code not in known:
        errs.append(f'{where}: unknown code')
    if code in seen:
        errs.append(f'{where}: duplicate template')
    seen.add(code)
    if not valid_page(t.get('page')):
        errs.append(f'{where}: page must be a whole number from 1 to {PAGE_MAX}')
    if 'print' in t and PRINT_OVERRIDES.get(code) != t['print']:
        errs.append(f'{where}: print override must be REJOINAL to REJOIN')
    slots, slot_errs = slot_table(t, where, limits, kinds, rules)
    used = {p for s in slots.values() if isinstance(s.get('between'), list) for p in s['between']} | {sid for sid, s in slots.items() if 'for' in s} | {r for s in slots.values() if isinstance(s.get('reads'), list) for r in s['reads']}
    errs += slot_errs
    used |= text_uses(t, where, limits, slots, errs)
    used |= column_uses(t, where, limits, slots, errs)
    errs += [f'{where}: slot {sid} declared but never used' for sid in slots if sid not in used]
    errs += _checks(t, where, limits, rules)
    errs += _links(t, where, limits, rules, known)
    return errs


def _checks(t, where, limits, rules):
    ids = t.get('rules') or []
    errs = [f"{where}: has {len(ids)} rules, max {limits['rules']}"] if len(ids) > limits['rules'] else []
    for rid in ids:
        if rid not in rules:
            errs.append(f'{where}: rule {rid} is unknown')
        elif rules[rid].get('kind') != 'check':
            errs.append(f'{where}: rule {rid} is not a check')
    errs += [f'{where}: rule {rid} repeats' for rid in sorted({r for r in ids if ids.count(r) > 1})]
    return errs


def _links(t, where, limits, rules, known):
    links = t.get('links') or []
    errs = [f"{where}: has {len(links)} links, max {limits['links']}"] if len(links) > limits['links'] else []
    for link in links:
        target = link.get('code')
        at = f'{where}: link to {target}'
        errs += [f'{at}: unknown key {k}' for k in sorted(set(link) - set(LINK_KEYS))]
        if target not in known:
            errs.append(f'{at}: unknown code')
        elif target == t.get('code'):
            errs.append(f'{at}: a template cannot link to itself')
        basis = link.get('basis')
        if basis not in BASES:
            errs.append(f'{at}: basis must be manual or practice')
        elif basis == 'manual' and not valid_page(link.get('page')):
            errs.append(f'{at}: a manual link needs a page')
        elif basis == 'practice' and link.get('page') is not None:
            errs.append(f'{at}: a practice link cites no page')
        if 'note' in link:
            errs += text_problems(at, 'note', link['note'], limits['note'])
        when = link.get('when')
        if when is not None and when not in rules:
            errs.append(f'{at}: condition {when} is unknown')
        elif when is not None and rules[when].get('kind') != 'condition':
            errs.append(f'{at}: condition {when} is not a condition')
    return errs


if __name__ == '__main__':
    found = check()
    print('\n'.join(found) if found else 'part2 rules and templates OK')
    sys.exit(1 if found else 0)

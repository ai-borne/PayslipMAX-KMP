"""Validate and ship the Part II ribbon layout (part2/nav.json) together with the code pages (part2/codes.json) as the additive
`part2` bundle section (Part II plan, phase 1).

nav.json (authored) groups the 303 ODP-2026 serials into ribbons > cases, in build order (RIBBON_LAYOUT.md). A case homes its
serials, may link `also` serials homed elsewhere, and marks the serials the officer only looks up (`lookup`: no draft is ever
made). A ribbon is `built` once its drafts ship; tiles show only for built ribbons. Limits live in nav.json, as the Guide's nav.json
does, and ship in the section, so GuidePart2LookupValidator.kt holds no second copy. compile.py calls check(); bundle.py calls for_bundle().
validate style as preclaim.py: a list of readable problems, empty means the file is fit to use.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
import part2_codes_check
from config import CARDS_DIR

NAV_PATH = os.path.join(CARDS_DIR, 'part2', 'nav.json')
SUPPORTED_VERSION = 1
ID = re.compile(r'^[a-z][a-z0-9_]{0,31}$')
LIMIT_KEYS = ('title_words', 'ribbons_min', 'ribbons_max', 'cases_per_ribbon', 'case_serials_min', 'case_serials_max')
CASE_KEYS = ('id', 'title', 'serials', 'also', 'lookup')


def load():
    with open(NAV_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def check():
    """Every problem in the real codes.json and nav.json together. compile.py gates the build on this."""
    codes = part2_codes_check.load()
    return part2_codes_check.problems(codes) + problems(load(), codes)


def problems(nav, codes):
    errs = []
    if nav.get('version') != SUPPORTED_VERSION:
        errs.append(f'part2 nav: version must be {SUPPORTED_VERSION}')
    limits = nav.get('limits') or {}
    if sorted(limits) != sorted(LIMIT_KEYS) or not all(isinstance(limits[k], int) and limits[k] > 0 for k in limits):
        return errs + [f'part2 nav: limits must be exactly {list(LIMIT_KEYS)}, each a positive whole number']
    known = {row['serial'] for row in codes['serials']}
    ribbons = nav.get('ribbons') or []
    if not limits['ribbons_min'] <= len(ribbons) <= limits['ribbons_max']:
        errs.append(f"part2 nav: {len(ribbons)} ribbons, allowed {limits['ribbons_min']} to {limits['ribbons_max']}")
    ribbon_ids, case_ids, homes = set(), set(), {}
    for ribbon in ribbons:
        errs += _ribbon(ribbon, limits, known, ribbon_ids, case_ids, homes)
    errs += [f'part2 nav: serial {s} has no home' for s in sorted(known - set(homes), key=_order)]
    return errs


def _order(serial):
    chapter, number = serial.split('.')
    return int(chapter), int(number)


def _title(where, title, limits):
    if not isinstance(title, str) or not title.strip():
        return [f'{where}: title is empty']
    errs = []
    if len(title.split()) > limits['title_words']:
        errs.append(f"{where}: title over {limits['title_words']} words")
    if '<' in title or '>' in title:
        errs.append(f'{where}: title has angle brackets (plain text only)')
    return errs


def _id(kind, value, seen):
    where = f'part2 nav: {kind} {value}'
    errs = [] if isinstance(value, str) and ID.match(value) else [f'{where}: id must be lowercase letters, digits and underscores']
    if value in seen:
        errs.append(f'part2 nav: duplicate {kind} id {value}')
    seen.add(value)
    return errs


def _ribbon(ribbon, limits, known, ribbon_ids, case_ids, homes):
    rid = ribbon.get('id')
    where = f'part2 nav: ribbon {rid}'
    errs = _id('ribbon', rid, ribbon_ids) + _title(where, ribbon.get('title'), limits)
    if not isinstance(ribbon.get('built'), bool):
        errs.append(f'{where}: built must be true or false')
    cases = ribbon.get('cases') or []
    if not 1 <= len(cases) <= limits['cases_per_ribbon']:
        errs.append(f"{where} has {len(cases)} cases, max {limits['cases_per_ribbon']}")
    for case in cases:
        errs += _case(case, limits, known, case_ids, homes)
    return errs


def _case(case, limits, known, case_ids, homes):
    cid = case.get('id')
    where = f'part2 nav: case {cid}'
    errs = _id('case', cid, case_ids) + _title(where, case.get('title'), limits)
    serials = case.get('serials') or []
    lo, hi = limits['case_serials_min'], limits['case_serials_max']
    if not lo <= len(serials) <= hi:
        errs.append(f'{where} has {len(serials)} serials, allowed {lo} to {hi}')
    for serial in serials:
        if serial not in known:
            errs.append(f'{where}: unknown serial {serial}')
        elif serial in homes:
            errs.append(f'part2 nav: serial {serial} homed twice ({homes[serial]}, {cid})')
        else:
            homes[serial] = cid
    for key in ('also', 'lookup'):
        errs += _listed(where, key, case.get(key) or [], serials, known)
    return errs


def _listed(where, key, items, serials, known):
    errs = []
    for item in sorted({i for i in items if items.count(i) > 1}):
        errs.append(f'{where}: {key} {item} repeats')
    for item in items:
        if item not in known:
            errs.append(f'{where}: {key} {item}: unknown serial')
        elif key == 'also' and item in serials:
            errs.append(f'{where}: also {item} is homed in this case')
        elif key == 'lookup' and item not in serials:
            errs.append(f'{where}: lookup {item} is not in the case')
    return errs


def for_bundle(nav, codes):
    """The `part2` section the app ships: the code pages plus the layout, whitelisted keys only, authored order kept."""
    found = part2_codes_check.problems(codes) + problems(nav, codes)
    if found:
        sys.exit('part2 files are not fit to bundle:\n  ' + '\n  '.join(found))
    return {
        'version': SUPPORTED_VERSION,
        'limits': {**{k: nav['limits'][k] for k in LIMIT_KEYS}, 'text': part2_codes_check.NAME_MAX},
        **part2_codes_check.for_bundle(codes),
        'ribbons': [{'id': r['id'], 'title': r['title'], 'built': r['built'], 'cases': [{k: c[k] for k in CASE_KEYS} for c in r['cases']]}
                    for r in nav['ribbons']],
    }


if __name__ == '__main__':
    found = check()
    print('\n'.join(found) if found else 'part2 nav.json OK')
    sys.exit(1 if found else 0)

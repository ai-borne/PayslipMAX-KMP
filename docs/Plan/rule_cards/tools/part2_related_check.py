"""Shape checks for part2/related.json: which entries are usually published with a casualty (Part II Orders Bank, phase 1).

Needs no manual text, so CI runs it. Each link is tagged `manual` (with the printed page that says so) or `practice` (seen in practice,
no page). Only casualties that have data are listed; nothing is guessed. problems() as part2_codes_check.py.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

RELATED_PATH = os.path.join(CARDS_DIR, 'part2', 'related.json')
VERSION = 1
BASES = ('manual', 'practice')
LAST_PAGE = 227


def load():
    with open(RELATED_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def problems(data, codes):
    errs = []
    if data.get('version') != VERSION:
        errs.append(f'related: version must be {VERSION}')
    known = {c for s in codes['serials'] for c in s['codes']}
    seen = set()
    for row in data.get('related') or []:
        code = row.get('code')
        where = f'related {code}'
        if code not in known:
            errs.append(f'{where}: not a code in codes.json')
        if code in seen:
            errs.append(f'{where}: listed twice')
        seen.add(code)
        links = row.get('with')
        if not isinstance(links, list) or not links:
            errs.append(f'{where}: with is empty')
            continue
        targets = set()
        for link in links:
            errs += _link(where, code, link, known, targets)
    return errs


def _link(where, code, link, known, targets):
    target = link.get('code')
    errs = []
    if target not in known:
        errs.append(f'{where}: {target} is not a code in codes.json')
    if target == code:
        errs.append(f'{where}: links to itself')
    if target in targets:
        errs.append(f'{where}: {target} listed twice')
    targets.add(target)
    basis, page = link.get('basis'), link.get('page')
    if basis not in BASES:
        errs.append(f'{where} {target}: basis must be one of {list(BASES)}')
    elif basis == 'manual' and not (isinstance(page, int) and 1 <= page <= LAST_PAGE):
        errs.append(f'{where} {target}: a manual link needs a printed page from 1 to {LAST_PAGE}')
    elif basis == 'practice' and page is not None:
        errs.append(f'{where} {target}: a practice link carries no page')
    return errs


def render(data):
    """One casualty per line, so a review diff reads row by row."""
    rows = ',\n'.join(' ' + json.dumps(r, ensure_ascii=False, separators=(',', ':')) for r in data['related'])
    return f'{{"version": {data["version"]},\n "related": [\n{rows}\n ]}}\n'

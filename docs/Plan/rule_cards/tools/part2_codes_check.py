"""Shape checks for part2/codes.json, the 303 ODP-2026 occurrence serials behind the Part II code pages (Part II plan, phase 0).

Needs no manual text, so CI runs it (through the tool tests); the generator (part2_codes.py) needs the local manual and does not.
validate() style as preclaim.py: a list of readable problems, empty means the file is fit to use.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

CODES_PATH = os.path.join(CARDS_DIR, 'part2', 'codes.json')
SUPPORTED_VERSION = 1
CHAPTER_COUNTS = [19, 16, 102, 5, 30, 15, 13, 10, 20, 10, 21, 6, 34, 2]
GROUPS = ('A', 'AM', 'B', 'C', 'CM')
# Annexure II prints no group for these two; the app shows "Group not printed in ODP-2026" and never infers one (Round 8).
NO_GROUP = frozenset({'3.28', '11.8'})
SERIAL = re.compile(r'^(\d{1,2})\.(\d{1,3})$')
CODE = re.compile(r'^[A-Z0-9]{2,12}$')
NAME_MAX = 300
# The keys of a serial row the app reads. Anything else in the authoring file stays in the repo.
SERIAL_KEYS = ('serial', 'chapter', 'codes', 'group', 'name', 'page')


def load():
    with open(CODES_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def problems(data):
    errs = []
    if data.get('version') != SUPPORTED_VERSION:
        errs.append(f'codes: version must be {SUPPORTED_VERSION}')
    if sorted(data.get('groups') or {}) != sorted(GROUPS):
        errs.append(f'codes: groups must be exactly {list(GROUPS)}')
    for g, text in (data.get('groups') or {}).items():
        errs += _text(f'codes: group {g}', 'meaning', text)
    rows = data.get('serials') or []
    seen_serials, seen_codes = set(), set()
    for row in rows:
        errs += _row(row, seen_serials, seen_codes)
    errs += _counts(rows)
    return errs


def for_bundle(data):
    """The slice the app ships: group meanings and whitelisted serial keys, authored order kept. A group the manual does not print is
    left out of the row rather than shipped as null. Call problems() first."""
    return {'groups': dict(data['groups']),
            'serials': [{k: row[k] for k in SERIAL_KEYS if row.get(k) is not None} for row in data['serials']]}


def _text(where, field, value):
    if not isinstance(value, str) or not value.strip():
        return [f'{where}: {field} is empty']
    errs = []
    if len(value) > NAME_MAX:
        errs.append(f'{where}: {field} is over {NAME_MAX} characters')
    if '<' in value or '>' in value:
        errs.append(f'{where}: {field} has angle brackets (plain text only)')
    return errs


def _row(row, seen_serials, seen_codes):
    serial = row.get('serial')
    where = f'codes: serial {serial}'
    match = SERIAL.match(serial) if isinstance(serial, str) else None
    if not match:
        return [f'{where}: not a chapter.number serial']
    errs = []
    if serial in seen_serials:
        errs.append(f'{where}: duplicate serial')
    seen_serials.add(serial)
    if row.get('chapter') != int(match.group(1)):
        errs.append(f'{where}: chapter does not match the serial')
    codes = row.get('codes') or []
    if not codes:
        errs.append(f'{where}: no codes')
    for code in codes:
        if not (isinstance(code, str) and CODE.match(code)):
            errs.append(f'{where}: code {code!r} must be 2-12 capital letters or digits')
        if code in seen_codes:
            errs.append(f'{where}: duplicate code {code!r}')
        seen_codes.add(code)
    errs += _text(where, 'name', row.get('name'))
    group = row.get('group')
    if serial in NO_GROUP:
        if group is not None:
            errs.append(f'{where}: the manual prints no group, so none may be inferred')
    elif group not in GROUPS:
        errs.append(f'{where}: group must be one of {list(GROUPS)}')
    page = row.get('page')
    if not (isinstance(page, int) and not isinstance(page, bool) and 1 <= page <= 400):
        errs.append(f'{where}: page must be a whole number from 1 to 400')
    return errs


def _counts(rows):
    errs = []
    chapters = [sum(1 for r in rows if r.get('chapter') == c) for c in range(1, len(CHAPTER_COUNTS) + 1)]
    if chapters != CHAPTER_COUNTS:
        errs.append(f'codes: serials per chapter are {chapters}, expected {CHAPTER_COUNTS}')
    for c, count in enumerate(chapters, start=1):
        have = {r['serial'] for r in rows if r.get('chapter') == c and 'serial' in r}
        gaps = [f'{c}.{n}' for n in range(1, count + 1) if f'{c}.{n}' not in have]
        if gaps and count == CHAPTER_COUNTS[c - 1]:
            errs.append(f'codes: chapter {c} has gaps {gaps}')
    return errs


if __name__ == '__main__':
    found = problems(load())
    print('\n'.join(found) if found else 'codes.json OK')
    sys.exit(1 if found else 0)

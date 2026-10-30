"""Shape checks for part2/about.json, the 30 plain-English casualty pages (Part II Orders Bank, phase 1).

Needs no manual text, so CI runs it. The words are our own simple language, culled from the ODP and the PCDA handbooks (owner decision
10a): no quotes, no rates or amounts, no markup or links. Sources for each page are kept in a local audit sheet, never in this file.
problems() as part2_codes_check.py: a list of readable problems, empty means the file is fit to ship.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR
from part2_columns_spotcheck import OWNER_PAGES, SIBLINGS

ABOUT_PATH = os.path.join(CARDS_DIR, 'part2', 'about.json')
VERSION = 1
PARTS = ('what', 'when', 'ready', 'watch')
KEYS = {'id', 'serials'} | set(PARTS)
PART_MAX = 400
WORDS_MAX = 130
CONTROL = re.compile(r'[\x00-\x08\x0b-\x1f\x7f]')
LINK = re.compile(r'https?:|www\.|\w+@\w+', re.I)
AMOUNT = re.compile(r'\bRs\b|₹|%|\brupee', re.I)


def load():
    with open(ABOUT_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def problems(data, codes, whole=True):
    """`whole` also demands exactly the owner's 30 pages and their 48 serials; the unit tests turn it off for one invented page."""
    errs = []
    if data.get('version') != VERSION:
        errs.append(f'about: version must be {VERSION}')
    known = {s['serial'] for s in codes['serials']}
    seen = set()
    for page in data.get('pages') or []:
        errs += _page(page, known, seen)
    if whole:
        ids = {p.get('id') for p in data.get('pages') or []}
        if ids != set(OWNER_PAGES):
            errs.append('about: pages must be exactly the owner\'s 30')
        if seen != set(OWNER_PAGES) | set(SIBLINGS):
            errs.append('about: pages must cover exactly the 48 serials of those 30 pages')
    return errs


def _page(page, known, seen):
    where = f'about {page.get("id")}'
    errs = [f'{where}: unknown key {k}' for k in page if k not in KEYS]
    serials = page.get('serials')
    if not isinstance(serials, list) or not serials or page.get('id') != serials[0]:
        errs.append(f'{where}: id must be the first of its serials')
        serials = serials if isinstance(serials, list) else []
    for s in serials:
        if s not in known:
            errs.append(f'{where}: serial {s} is not in codes.json')
        if s in seen:
            errs.append(f'{where}: serial {s} is on two pages (twice)')
        seen.add(s)
    words = 0
    for part in PARTS:
        text = page.get(part)
        if not isinstance(text, str) or not text.strip():
            errs.append(f'{where}: {part} is empty')
            continue
        words += len(text.split())
        errs += _text(f'{where} {part}', text)
    if words > WORDS_MAX:
        errs.append(f'{where}: {words} words, over {WORDS_MAX}')
    return errs


def _text(where, text):
    errs = []
    if len(text) > PART_MAX:
        errs.append(f'{where}: over {PART_MAX} characters')
    if '<' in text or '>' in text:
        errs.append(f'{where}: angle bracket')
    if CONTROL.search(text):
        errs.append(f'{where}: control character')
    if LINK.search(text):
        errs.append(f'{where}: link or address')
    if AMOUNT.search(text):
        errs.append(f'{where}: amount or rate')
    return errs


def render(data):
    """One page per line, so a review diff reads page by page."""
    rows = ',\n'.join(' ' + json.dumps(p, ensure_ascii=False, separators=(',', ':')) for p in data['pages'])
    return f'{{"version": {data["version"]},\n "pages": [\n{rows}\n ]}}\n'

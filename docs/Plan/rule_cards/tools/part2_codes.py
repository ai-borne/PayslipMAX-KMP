#!/usr/bin/env python3
"""Generate part2/codes.json: every occurrence serial of ODP-2026 Annexure II, for the Part II code pages (plan phase 0).

Inputs stay outside git (copyrighted manual, owner's hand-checked list), read through config.src():
  part2/agent_layout_2026-10-22/master.tsv   serial, code(s) joined by '/', group ('?' = none printed), short label
  part2/full_text_layout.txt                 the manual's text dump
Output: part2/codes.json with, per serial: chapter, codes, group, the manual's own name (Annexure II wording), and the detail
page (the printed page of its Annexure III row, which is the index's Page column).
The flawed part2_occurrences_2026.json is never an input. Usage: part2_codes.py [--check]   (--check: is the committed file stale?)
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR, src

OUT_PATH = os.path.join(CARDS_DIR, 'part2', 'codes.json')
CODE = re.compile(r'^[A-Z0-9]{2,12}$')
GROUPS = ('A', 'AM', 'B', 'C', 'CM')
# Para 3 (p.4) of the manual, in short.
GROUP_MEANINGS = {
    'A': 'Financial (other than claims)',
    'AM': 'Financial (other than claims) required by MISO',
    'B': 'Financial (claims)',
    'C': 'Non-financial',
    'CM': 'Non-financial required by MISO / non-actionable for the O/o PCDA(O)',
}
WRAP_PREFIX = 6  # a code wrapped by a narrow column (TADISCH for TADISCHG) is found by its first six letters
# Every code that is not a whole word in the Annexure II index or in the Annexure III rows, because a column wrapped it. Each was
# read by hand at its row on 2026-10-23. Anything not listed fails, so a real gap can never hide in a loose tolerance.
KNOWN_MISSES = frozenset({
    ('3.27', 'GTDTLA2', 'index'), ('3.27', 'GTDTLA3', 'index'),
    ('7.10', 'MEDAL', 'detail'), ('10.7', 'TADISCHG', 'detail'), ('11.2', 'MARSEROF', 'detail'),
    ('11.10', 'DTHSPOUS', 'detail'), ('11.11', 'DTHFAMEM', 'detail'), ('11.18', 'CHNGNTSP', 'detail'),
    ('11.21', 'HOMCHNG', 'detail'), ('13.14', 'ALOTPRNO', 'detail'), ('13.17', 'WOUNDACT', 'detail'),
    ('13.31', 'LANGPROF', 'detail'),
})
SERIAL_LINE = re.compile(r'^\s*(\d{1,2}\.\d{1,3})\s+(\S.*)$')
PAGE_LINE = re.compile(r'^\s{20,}(\d{1,3})\s*$')
NOISE_LINE = re.compile(r'^\s*(CHAPTER\b|Srl?\s*No|Code\b|No\b)')
FOOTER = re.compile(r'ODP-|Intake / Commencement|Nature of Occurrence')


def parse_master(text):
    rows, seen = [], set()
    for line in text.strip().split('\n'):
        serial, codes, group, label = line.split('\t')[:4]
        codes = codes.split('/')
        for code in codes:
            if not CODE.match(code):
                raise ValueError(f'{serial}: bad code {code!r}')
            if code in seen:
                raise ValueError(f'{serial}: duplicate code {code!r}')
            seen.add(code)
        if group != '?' and group not in GROUPS:
            raise ValueError(f'{serial}: unknown group {group!r}')
        rows.append({'serial': serial, 'codes': codes, 'group': None if group == '?' else group, 'label': label})
    return rows


def index_names(index_lines, rows):
    """The manual's own Nature-of-Occurrence wording per serial. A line is split on column gaps (two or more spaces); the name is
    every leading piece up to the first one that belongs to another column (a code, a group letter, a page number), and the
    wrapped lines below the row are joined on."""
    heads = [(i, m.group(1), m.start(2)) for i, line in enumerate(index_lines) if (m := SERIAL_LINE.match(line))]
    by_serial = {r['serial']: r for r in rows}
    names = {}
    for k, (i, serial, col) in enumerate(heads):
        if serial not in by_serial:
            continue
        end = heads[k + 1][0] if k + 1 < len(heads) else len(index_lines)
        prefixes = tuple(c[:WRAP_PREFIX - 1] for c in by_serial[serial]['codes'])
        parts = _name_pieces(index_lines[i][col:], prefixes)
        for line in index_lines[i + 1:end]:
            if not (NOISE_LINE.match(line) or FOOTER.search(line)):
                parts += _name_pieces(line, prefixes)
        names[serial] = _tidy(' '.join(parts), by_serial[serial]['codes'])
    return names


def _tidy(name, codes):
    """Drops this row's own long codes (and the slash that follows one) from inside a name, and any page range."""
    for code in codes:
        if len(code) >= WRAP_PREFIX - 1:
            name = re.sub(re.escape(code[:WRAP_PREFIX - 1]) + r'[A-Z0-9]*\b\s*/?', ' ', name)
    name = re.sub(r'\b\d{2,3}-\d{2,3}\b', ' ', name)
    return re.sub(r'\s+', ' ', name).strip()


def _name_pieces(text, prefixes):
    pieces = []
    for chunk in re.split(r'\s{2,}', text.strip()):
        if not chunk or chunk in GROUPS or re.fullmatch(r'[\d\-]+', chunk) or chunk.startswith(prefixes) or set(chunk) <= {'/', ' '}:
            break
        pieces.append(re.sub(r'\s+', ' ', chunk))
    return pieces


def detail_pages(lines, rows):
    """Printed page of each serial's Annexure III row: the last page number seen before the row starts."""
    pages = {}
    for width in (WRAP_PREFIX, WRAP_PREFIX - 1):  # the longer code prefix is tried first; the shorter only for what is left
        pages.update(_scan_pages(lines, [r for r in rows if r['serial'] not in pages], width))
    return pages


def _scan_pages(lines, rows, width):
    """A serial can wrap too (13.10 prints as 13.1), so a row matches when the printed serial is the serial or its start."""
    wanted = {r['serial']: re.escape(r['codes'][0][:width]) for r in rows}
    pages, page = {}, 0
    for line in lines:
        m = PAGE_LINE.match(line)
        if m:
            page = int(m.group(1))
            continue
        row = SERIAL_LINE.match(line)
        if not row:
            continue
        for serial, code in wanted.items():
            if serial not in pages and serial.startswith(row.group(1)) and re.match(code, row.group(2)):
                pages[serial] = page
                break
    return pages


def _whole_word(code):
    return re.compile(r'(?<![A-Za-z0-9])' + re.escape(code) + r'(?![A-Za-z0-9])')


def cross_check(index_text, detail_text, rows):
    """(serial, code, 'index'|'detail') for each code that is not a whole word in that part of the manual."""
    misses = []
    for r in rows:
        for code in r['codes']:
            word = _whole_word(code)
            for where, text in (('index', index_text), ('detail', detail_text)):
                if not word.search(text):
                    misses.append((r['serial'], code, where))
    return misses


def build(master_text, manual_lines):
    rows = parse_master(master_text)
    index_start = next(i for i, line in enumerate(manual_lines) if 'ANNEXURE II' in line and 'INDEX' in line)
    detail_start = next(i for i, line in enumerate(manual_lines) if re.search(r'CHAPTER-1\s*:\s*COMMISSIONING', line))
    page_marker = next(i for i in range(detail_start, -1, -1) if PAGE_LINE.match(manual_lines[i]))  # chapter 1's page is printed above its heading
    index_lines, detail_lines = manual_lines[index_start:detail_start], manual_lines[page_marker:]
    unexpected = set(cross_check('\n'.join(index_lines), '\n'.join(detail_lines), rows)) ^ KNOWN_MISSES
    if unexpected:
        raise SystemExit(f'cross-check differs from the known wrap list: {sorted(unexpected)}')
    names, pages = index_names(index_lines, rows), detail_pages(detail_lines, rows)
    lost = [r['serial'] for r in rows if not names.get(r['serial']) or r['serial'] not in pages]
    if lost:
        raise SystemExit(f'no name or page found for {lost}')
    serials = [{'serial': r['serial'], 'chapter': int(r['serial'].split('.')[0]), 'codes': r['codes'], 'group': r['group'],
                'name': names[r['serial']], 'page': pages[r['serial']]} for r in rows]
    return {'version': 1, 'source': 'Documentation Procedure (Officers) 2026, Annexure II and III', 'groups': GROUP_MEANINGS,
            'serials': serials}


def generate():
    with open(src('part2/agent_layout_2026-10-22/master.tsv'), encoding='utf-8') as fh:
        master = fh.read()
    with open(src('part2/full_text_layout.txt'), encoding='utf-8') as fh:
        return build(master, fh.read().split('\n'))


def render(data):
    """One serial per line, so a review diff reads row by row."""
    head = json.dumps({k: v for k, v in data.items() if k != 'serials'}, ensure_ascii=False)[:-1]
    rows = ',\n'.join(' ' + json.dumps(r, ensure_ascii=False, separators=(',', ':')) for r in data['serials'])
    return f'{head},\n "serials": [\n{rows}\n ]}}\n'


def main(argv):
    text = render(generate())
    if '--check' in argv:
        current = open(OUT_PATH, encoding='utf-8').read() if os.path.exists(OUT_PATH) else ''
        if current != text:
            sys.exit(f'stale: run python3 {os.path.relpath(__file__)}')
        print('codes.json up to date')
        return
    os.makedirs(os.path.dirname(OUT_PATH), exist_ok=True)
    with open(OUT_PATH, 'w', encoding='utf-8') as fh:
        fh.write(text)
    print(f'wrote {os.path.relpath(OUT_PATH)} ({len(text.encode("utf-8"))} bytes)')


if __name__ == '__main__':
    main(sys.argv[1:])

#!/usr/bin/env python3
"""LOCAL ONLY (never run in CI, like part2_codes.py): read the Annexure III grid of the ODP-2026 PDF into raw cell text per serial.

Input stays outside git: the owner's PDF, path in config.src('part2/odp_2026.pdf') or the ODP_PDF environment variable.
For each page the table's own ruling lines (thin filled rectangles that pdftocairo writes to SVG) give the column and row borders;
every word from pdftotext -bbox is put in the cell whose borders contain its centre. A row without a serial in column 1 continues the
row above (a row split across a page). Output: raw rows {serial, codes, cells[0..5]} where cells = From, To, Data 1-4, text exactly as
printed except that words a narrow column cut in two ("Require d") are rejoined by REJOIN, a closed list, reviewed by hand.
Usage: part2_columns_extract.py <out.json> [first_page last_page]
"""
import json
import os
import re
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(__file__))
import part2_codes_check
import part2_columns
import part2_columns_check
from config import src
from part2_columns_hand import HAND

HEADER = re.compile(r'^\(?1\)?\s*Sr\.?|^\(?1\)?$|^S\.?\s?No|^Sr\.')
SERIAL = re.compile(r'^\d{1,2}\.\d{1,3}$')
WORD = re.compile(r'<word xMin="([\d.]+)" yMin="([\d.]+)" xMax="([\d.]+)" yMax="([\d.]+)">([^<]*)</word>')
RECT = re.compile(r'<path fill-rule="evenodd" fill="rgb\(0%, 0%, 0%\)" fill-opacity="1" d="M ([\d.]+) ([\d.]+) L ([\d.]+) ([\d.]+) L ([\d.]+) ([\d.]+) L ([\d.]+) ([\d.]+) Z')
FIRST_PAGE, LAST_PAGE = 28, 227


def pdf_path():
    return os.environ.get('ODP_PDF') or src('part2/odp_2026.pdf')


def run(*cmd):
    return subprocess.run(cmd, check=True, capture_output=True, text=True).stdout


def rules(pdf, page, tmp):
    """(vertical x positions, horizontal y positions with their x extent) of the table's lines."""
    out = os.path.join(tmp, 'p.svg')
    run('pdftocairo', '-svg', '-f', str(page), '-l', str(page), pdf, out)
    verticals, horizontals = [], []
    for m in RECT.finditer(open(out, encoding='utf-8').read()):
        x = [float(m.group(i)) for i in (1, 3, 5, 7)]
        y = [float(m.group(i)) for i in (2, 4, 6, 8)]
        w, h = max(x) - min(x), max(y) - min(y)
        if w < 1.0 and h > 4:
            verticals.append(((min(x) + max(x)) / 2, min(y), max(y)))
        elif h < 1.0 and w > 4:
            horizontals.append(((min(y) + max(y)) / 2, min(x), max(x)))
    return verticals, horizontals


def clusters(values, gap=2.0):
    out = []
    for v in sorted(values):
        if out and v - out[-1][-1] <= gap:
            out[-1].append(v)
        else:
            out.append([v])
    return [sum(c) / len(c) for c in out]


def page_rows(pdf, page, tmp):
    """Return (rows, odd): rows are 9-cell lists, one per ruled band; odd lists bands whose column count is not 9 (a stacked table
    on the same page has its own column positions, so the edges are taken from the vertical rules that cross each band)."""
    verticals, horizontals = rules(pdf, page, tmp)
    ys = clusters([h[0] for h in horizontals if h[2] - h[1] > 20])
    words = [(float(a), float(b), float(c), float(d), t) for a, b, c, d, t in
             WORD.findall(run('pdftotext', '-f', str(page), '-l', str(page), '-bbox', pdf, '-'))]
    rows, odd = [], []
    for top, bottom in zip(ys, ys[1:]):
        mid = (top + bottom) / 2
        xs = clusters([v[0] for v in verticals if v[1] - 1 <= mid <= v[2] + 1])
        if bottom - top < 6 or not xs:
            continue
        inside_band = [w for w in words if top < (w[1] + w[3]) / 2 < bottom]
        if len(xs) != 10:
            if inside_band:
                odd.append((round(top), len(xs)))
            continue
        cells = []
        for left, right in zip(xs, xs[1:]):
            inside = [w for w in inside_band if left < (w[0] + w[2]) / 2 < right]
            inside.sort(key=lambda w: (round(w[1] / 4), w[0]))
            cells.append(' '.join(w[4] for w in inside))
        if any(cells):
            rows.append(cells)
    return rows, odd


def is_header(cells):
    """The column-title bands: '(1) Sr. No.', 'S No', the '1 2 3 ... 9' numbering row, or a band that carries the 'From Date' title."""
    numbering = all(c.strip().strip('()') == str(i + 1) for i, c in enumerate(cells))
    return bool(HEADER.match(cells[0])) or numbering or cells[3].strip().startswith('From Date')


def extract(pdf, first=FIRST_PAGE, last=LAST_PAGE):
    result, skipped = [], []
    with tempfile.TemporaryDirectory() as tmp:
        for page in range(first, last + 1):
            rows, odd = page_rows(pdf, page, tmp)
            if odd:
                skipped.append((page, odd))
            for cells in rows:
                if is_header(cells):
                    continue
                serial = re.sub(r'\s+', '', cells[0])  # a narrow column wraps '13.15' as '13.1 5'
                if SERIAL.match(serial):
                    result.append({'serial': serial, 'page': page, 'code': re.sub(r'\s+', '', cells[1]), 'name': cells[2], 'cells': cells[3:]})
                elif result and any(cells[3:]):
                    prev = result[-1]
                    prev['code'] = f'{prev["code"]} {cells[1]}'.strip()
                    prev['name'] = f'{prev["name"]} {cells[2]}'.strip()
                    prev['cells'] = [f'{a} {b}'.strip() for a, b in zip(prev['cells'], cells[3:])]
                    prev.setdefault('continued_on', []).append(page)
    return result, skipped


def build(raw_rows, codes):
    """columns.json content from the raw cells: the rules of part2_columns, then the rows read by hand."""
    by_serial = {r['serial']: r for r in raw_rows}
    rows = [part2_columns.build_row(by_serial[s['serial']], s, HAND.get(s['serial'])) for s in codes['serials']]
    return {'version': part2_columns.VERSION, 'source': part2_columns.SOURCE, 'rows': rows}


def main(argv):
    """part2_columns_extract.py <raw.json> [first last]   write the raw cells (a local scratch file, never committed)
       part2_columns_extract.py --build <raw.json>        write part2/columns.json from a raw file"""
    if argv[0] == '--build':
        raw = json.load(open(argv[1], encoding='utf-8'))['rows']
        data = build(raw, part2_codes_check.load())
        found = part2_columns_check.problems(data, part2_codes_check.load())
        if found:
            sys.exit('\n'.join(found))
        with open(part2_columns_check.COLUMNS_PATH, 'w', encoding='utf-8') as fh:
            fh.write(part2_columns.render(data))
        print(f'wrote {os.path.relpath(part2_columns_check.COLUMNS_PATH)}: {len(data["rows"])} rows')
        return
    out = argv[0]
    first, last = (int(argv[1]), int(argv[2])) if len(argv) == 3 else (FIRST_PAGE, LAST_PAGE)
    rows, skipped = extract(pdf_path(), first, last)
    with open(out, 'w', encoding='utf-8') as fh:
        json.dump({'rows': rows, 'bands_without_ten_vertical_rules': skipped}, fh, ensure_ascii=False, indent=1)
    print(f'{len(rows)} rows; bands without ten vertical rules (page, [(y, count)]): {skipped}')


if __name__ == '__main__':
    main(sys.argv[1:])

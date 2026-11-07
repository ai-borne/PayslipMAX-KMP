"""The owner's 25-row check of columns.json (plan 13, owner decision 11): 10 rows from the 30 plain-English pages plus 15 seeded-random
from the rest, with the oddest rows always in. The draw is pure and reproducible from the committed table; only the sheet needs the
local raw cells. The sheet is a local text file beside the ODP PDF, never uploaded or published.
Usage: part2_columns_spotcheck.py <sheet.txt>
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from part2_columns import FIGURE_REASON
from part2_columns_check import load
from part2_columns_hand import KNOWN_DISAGREEMENTS

SEED = 20261030
# The owner's 30 pages (plan 13 appendix), by their first serial; each sibling serial shares its page and is checked there.
OWNER_PAGES = frozenset({
    '5.1', '5.2', '5.4', '5.6', '5.11', '3.82', '3.86', '3.87', '3.1', '3.12',
    '13.1', '13.2', '13.5', '13.3', '13.7', '3.53', '3.55',
    '5.26', '5.27', '5.28', '14.1', '14.2', '8.2', '8.6',
    '4.1', '4.2', '4.3', '12.1', '11.1', '11.9'})
SIBLINGS = frozenset({f'3.{n}' for n in range(2, 12)} | {'3.56', '3.57', '8.3', '8.4', '8.5', '8.7', '8.8', '8.9'})
OWNER_DRAWN, REST_DRAWN = 10, 15


def _cols(row):
    return [row['from'], row['to']] + row['data']


def oddest(columns):
    """Serials always checked: rows read by hand, rows where the old templates disagree with the printed page, the row with the longest
    caption, and any row whose To column is optional."""
    rows = columns['rows']
    odd = {r['serial'] for r in rows if r.get('hand') and r['hand'] != FIGURE_REASON}
    codes = {code: r['serial'] for r in rows for code in r['codes']}
    odd |= {codes[code] for code, _ in KNOWN_DISAGREEMENTS}
    odd.add(max(rows, key=lambda r: max(len(c['caption']) for c in _cols(r)))['serial'])
    odd |= {r['serial'] for r in rows if r['to']['need'] == 'optional'}
    return sorted(odd, key=lambda s: [int(p) for p in s.split('.')])


def draw(columns, seed):
    rng = random.Random(seed)
    odd = set(oddest(columns))
    serials = [r['serial'] for r in columns['rows']]
    pools = {'owner_pages': (sorted(OWNER_PAGES), OWNER_DRAWN),
             'rest': ([s for s in serials if s not in OWNER_PAGES | SIBLINGS], REST_DRAWN)}
    out = {}
    for name, (pool, size) in pools.items():
        forced = [s for s in pool if s in odd]
        free = [s for s in pool if s not in odd]
        out[name] = sorted(forced + rng.sample(free, size - len(forced)), key=lambda s: [int(p) for p in s.split('.')])
    return out


def sheet(columns, picks):
    lines = ['LOCAL SPOT-CHECK SHEET - not for upload or publication. Read each row against the printed ODP page, then',
             'mark any cell that is wrong. need: R required, O optional, N none; after the dash, the caption as it will ship.', '']
    for pool in ('owner_pages', 'rest'):
        lines.append(f'== {pool}')
        for serial in picks[pool]:
            row = next(r for r in columns['rows'] if r['serial'] == serial)
            lines.append(f'[{serial}] ODP page {row["page"]}  {" / ".join(row["codes"])}')
            for name, col in zip(('From', 'To', 'Data 1', 'Data 2', 'Data 3', 'Data 4'), _cols(row)):
                lines.append(f'    {name:7} {col["need"][0].upper()} - {col["caption"]!r}')
            if 'hand' in row:
                lines.append(f'    read by hand: {row["hand"]}')
        lines.append('')
    return '\n'.join(lines)


def main(argv):
    columns = load()
    picks = draw(columns, SEED)
    with open(argv[0], 'w', encoding='utf-8') as fh:
        fh.write(sheet(columns, picks))
    print(json.dumps(picks))


if __name__ == '__main__':
    main(sys.argv[1:])

"""Shape checks for part2/columns.json, the Annexure III table behind the Part II entry form (Part II Orders Bank, phase 1).

Needs no manual text, so CI runs it (through the tool tests); the extractor (part2_columns_extract.py) needs the local PDF and does not.
problems() as part2_codes_check.py: a list of readable problems, empty means the file is fit to ship.
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR
from part2_columns import NEEDS, VERSION

COLUMNS_PATH = os.path.join(CARDS_DIR, 'part2', 'columns.json')
CAPTION_MAX = 160
HAND_MAX = 240
DATA_COLUMNS = 4
CONTROL = re.compile(r'[\x00-\x08\x0b-\x1f\x7f]')


def load():
    with open(COLUMNS_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def problems(data, codes):
    errs = []
    if data.get('version') != VERSION:
        errs.append(f'columns: version must be {VERSION}')
    want = {s['serial']: s for s in codes['serials']}
    seen = set()
    for row in data.get('rows') or []:
        serial = row.get('serial')
        if serial in seen:
            errs.append(f'columns {serial}: duplicate serial')
        seen.add(serial)
        if serial not in want:
            errs.append(f'columns {serial}: not a serial in codes.json')
            continue
        errs += _row(row, want[serial])
    errs += [f'columns {s}: missing' for s in want if s not in seen]
    return errs


def _row(row, codes_row):
    where = f'columns {row["serial"]}'
    errs = []
    if row.get('codes') != codes_row['codes']:
        errs.append(f'{where}: codes {row.get("codes")} differ from codes.json {codes_row["codes"]}')
    if row.get('page') != codes_row['page']:
        errs.append(f'{where}: page differs from codes.json')
    cols = {'from': row.get('from'), 'to': row.get('to')}
    data = row.get('data')
    if not isinstance(data, list) or len(data) != DATA_COLUMNS:
        errs.append(f'{where}: data must hold exactly {DATA_COLUMNS} columns')
    else:
        cols.update({f'data {i + 1}': c for i, c in enumerate(data)})
    for name, col in cols.items():
        errs += _column(f'{where} {name}', col)
    if 'hand' in row and (not isinstance(row['hand'], str) or not row['hand'].strip() or len(row['hand']) > HAND_MAX):
        errs.append(f'{where}: hand reason must be text of at most {HAND_MAX} characters')
    return errs


def _column(where, col):
    if not isinstance(col, dict) or col.get('need') not in NEEDS:
        return [f'{where}: need must be one of {list(NEEDS)}']
    caption = col.get('caption')
    if not isinstance(caption, str):
        return [f'{where}: caption must be text']
    errs = []
    if col['need'] == 'none' and caption:
        errs.append(f'{where}: a none column carries no caption')
    if len(caption) > CAPTION_MAX:
        errs.append(f'{where}: caption is over {CAPTION_MAX} characters')
    if '<' in caption or '>' in caption:
        errs.append(f'{where}: caption has an angle bracket')
    if CONTROL.search(caption):
        errs.append(f'{where}: caption has a control character')
    return errs

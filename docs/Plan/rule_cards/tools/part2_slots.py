"""Slots, placeholders and columns of a Part II template (Part II plan, phase 2a); part2_templates.py calls these.

A slot is a question the officer answers once per event: a `date`, a `number`, free `text`, an order reference `ref` (used as
`{id.no}` or `{id.date}`), or `days`, which the app counts between two date slots. Placeholders are `{id}` in the narrative, the
authority line and the certificate; a column (From, To, Data 1-4) names a slot or is `none` where the manual says it is not required.
Plain scans, no regex over template text. Problems name ids only.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from part2_rules import ID, text_problems

KINDS = ('date', 'number', 'text', 'days', 'ref')
NEEDS = ('required', 'optional', 'none')
SLOT_KEYS = ('id', 'kind', 'label', 'help', 'between')
REF_PARTS = ('no', 'date')


def _refs(text):
    """(placeholder contents, well_formed) for a text: each `{...}` once, closed, not nested and not empty."""
    found, ok, i = [], True, 0
    while i < len(text):
        if text[i] == '}':
            ok, i = False, i + 1
        elif text[i] == '{':
            end = text.find('}', i + 1)
            inner = text[i + 1:end] if end > 0 else ''
            if end < 0 or not inner or '{' in inner:
                return found, False
            found.append(inner)
            i = end + 1
        else:
            i += 1
    return found, ok


def _split(ref):
    """(slot, part) for `slot` (part None) or `slot.no` / `slot.date`; None when it is neither."""
    base, dot, part = ref.partition('.')
    if not ID.match(base) or (dot and part not in REF_PARTS):
        return None
    return base, part or None


def slot_table(t, where, limits, kinds):
    slots, errs = {}, []
    items = t.get('slots') or []
    if len(items) > limits['slots']:
        errs.append(f"{where}: has {len(items)} slots, max {limits['slots']}")
    for s in items:
        sid, kind = s.get('id'), s.get('kind')
        at = f'{where}: slot {sid}'
        errs += [f'{at}: unknown key {k}' for k in sorted(set(s) - set(SLOT_KEYS))]
        if not (isinstance(sid, str) and ID.match(sid)):
            errs.append(f'{at}: id must be lowercase letters, digits and underscores')
        if sid in slots:
            errs.append(f'{where}: duplicate slot id {sid}')
        slots[sid] = s
        if kind not in KINDS:
            errs.append(f"{at}: kind must be {', '.join(KINDS[:-1])} or {KINDS[-1]}")
        kinds.setdefault(sid, set()).add(kind)
        errs += text_problems(at, 'label', s.get('label'), limits['label']) + text_problems(at, 'help', s.get('help'), limits['help'])
    for s in items:
        errs += _between(s, where, slots)
    return slots, errs


def _between(s, where, slots):
    pair = s.get('between')
    if s.get('kind') != 'days':
        return [f"{where}: slot {s.get('id')}: only a days slot has between"] if 'between' in s else []
    ok = isinstance(pair, list) and len(pair) == 2 and pair[0] != pair[1] and all(slots.get(p, {}).get('kind') == 'date' for p in pair)
    return [] if ok else [f"{where}: slot {s.get('id')}: between must name two different date slots"]


def _use(parts, where, slots, errs):
    """Check one reference to a slot, given as _split's (slot, part), and return the slot id it uses."""
    sid, part = parts
    kind = slots.get(sid, {}).get('kind')
    if sid not in slots:
        errs.append(f'{where}: slot {sid} used but not declared')
    elif kind == 'ref' and not part:
        errs.append(f'{where}: slot {sid} is a reference, so use {sid}.no or {sid}.date')
    elif kind != 'ref' and part:
        errs.append(f'{where}: slot {sid} is not a reference, so it has no .{part}')
    return sid


def text_uses(t, where, limits, slots, errs):
    used = set()
    for field in ('narrative', 'authority', 'certificate'):
        if field not in t or (field != 'narrative' and t[field] is None):
            continue
        errs += text_problems(where, field, t[field], limits['narrative'])
        if not isinstance(t[field], str):
            continue
        refs, ok = _refs(t[field])
        parts = [_split(r) for r in refs]
        if not ok or None in parts:
            errs.append(f'{where}: {field} has a malformed placeholder')
        used |= {_use(p, where, slots, errs) for p in parts if p}
    return used


def column_uses(t, where, slots, errs):
    used = set()
    data = t.get('data')
    if not isinstance(data, list) or len(data) != 4:
        errs.append(f'{where}: data must have exactly 4 columns')
        data = []
    named = [('from', t.get('from')), ('to', t.get('to'))] + [(f'data{i}', c) for i, c in enumerate(data, start=1)]
    for name, col in named:
        at = f'{where}: column {name}'
        need = col.get('need') if isinstance(col, dict) else None
        if need not in NEEDS:
            errs.append(f'{at} need must be required, optional or none')
        elif need == 'none':
            if col.get('slot') is not None:
                errs.append(f'{at} is not required but names a slot')
        elif not col.get('slot'):
            errs.append(f'{at} needs a slot')
        elif _split(col['slot']) is None:
            errs.append(f'{at} has a malformed slot reference')
        else:
            sid = _use(_split(col['slot']), where, slots, errs)
            used.add(sid)
            if name in ('from', 'to') and sid in slots and (slots[sid].get('kind') != 'date' or '.' in col['slot']):
                errs.append(f'{at} must name a date slot')
    return used



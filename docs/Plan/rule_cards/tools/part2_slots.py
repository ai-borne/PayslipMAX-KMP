"""Slots, placeholders and columns of a Part II template (Part II plan, phase 2a); part2_templates.py calls these.

A slot is a question the officer answers once per event: a `date`, a `number`, free `text`, an order reference `ref` (used as
`{id.no}` or `{id.date}`), or one the app works out from other dates: `days` (counted between two date slots), `earliest` / `latest`
(of 2 to 4 date slots, using those the officer gave) and `next_day` (the day after one date slot). A computed slot may only read slots
declared before it, so no loop can exist. `if_missing` makes a date, number or text slot optional and names what prints when it is
not answered; `for` marks a slot only a check reads (it prints nowhere). Placeholders are `{id}` in the narrative, the authority line
and the certificate; a column (From, To, Data 1-4) names a slot, holds `text` that joins words and placeholders, or is `none` where the
manual says it is not required. Plain scans, no regex over template text. Problems name ids only.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from part2_rules import ID, text_problems

KINDS = ('date', 'number', 'text', 'days', 'ref', 'earliest', 'latest', 'next_day')
COMPUTED = ('earliest', 'latest', 'next_day')
PLAIN = ('date', 'number', 'text')
NEEDS = ('required', 'optional', 'none')
SLOT_KEYS = ('id', 'kind', 'label', 'help', 'between', 'if_missing', 'for')
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
    for i, s in enumerate(items):
        errs += _between(s, where, slots, items[:i]) + _optional(s, where, limits, t.get('rules') or [])
    return slots, errs


def _between(s, where, slots, before):
    """Inputs of a days or computed slot. days reads two date slots declared anywhere; a computed slot reads only slots declared before
    it (so no loop is possible), which must hold a date: a date slot or an earlier computed date."""
    pair, kind, at = s.get('between'), s.get('kind'), f"{where}: slot {s.get('id')}"
    if kind != 'days' and kind not in COMPUTED:
        return [f'{at}: only a days or computed slot has between'] if 'between' in s else []
    if kind == 'days':
        ok = isinstance(pair, list) and len(pair) == 2 and pair[0] != pair[1] and all(slots.get(p, {}).get('kind') == 'date' for p in pair)
        return [] if ok else [f'{at}: between must name two different date slots']
    earlier = {x.get('id'): x.get('kind') for x in before}
    dated = isinstance(pair, list) and all(earlier.get(p) == 'date' or earlier.get(p) in COMPUTED for p in pair)
    if kind == 'next_day':
        return [] if dated and len(pair) == 1 else [f'{at}: between must name one date slot']
    return [] if dated and 2 <= len(pair) <= 4 and len(set(pair)) == len(pair) else [f'{at}: between must name 2 to 4 different date slots']


def _optional(s, where, limits, rules):
    """`if_missing` (an optional answer and what prints without it) and `for` (an answer only a check reads) belong to plain slots."""
    at, errs = f"{where}: slot {s.get('id')}", []
    if 'if_missing' in s:
        if s.get('kind') not in PLAIN:
            errs.append(f'{at}: if_missing is only for a date, number or text slot')
        errs += text_problems(at, 'if_missing', s['if_missing'], limits['note'])
    if 'for' in s:
        if s.get('kind') not in PLAIN:
            errs.append(f'{at}: for is only for a date, number or text slot')
        elif s['for'] not in rules:
            errs.append(f'{at}: for names a rule the template does not run')
    return errs


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


def _text_use(text, label, where, limits, slots, errs):
    """Limits, markup and placeholders of one shipped text; the slots it uses. `label` is the field or column named in a problem."""
    errs += text_problems(where, label, text, limits['narrative'])
    if not isinstance(text, str):
        return set()
    refs, ok = _refs(text)
    parts = [_split(r) for r in refs]
    if not ok or None in parts:
        errs.append(f'{where}: {label} has a malformed placeholder')
    return {_use(p, where, slots, errs) for p in parts if p}


def text_uses(t, where, limits, slots, errs):
    used = set()
    for field in ('narrative', 'authority', 'certificate'):
        if field in t and (field == 'narrative' or t[field] is not None):
            used |= _text_use(t[field], field, where, limits, slots, errs)
    return used


def column_uses(t, where, limits, slots, errs):
    used = set()
    data = t.get('data')
    if not isinstance(data, list) or len(data) != 4:
        errs.append(f'{where}: data must have exactly 4 columns')
        data = []
    named = [('from', t.get('from')), ('to', t.get('to'))] + [(f'data{i}', c) for i, c in enumerate(data, start=1)]
    for name, col in named:
        col = col if isinstance(col, dict) else {}
        at = f'{where}: column {name}'
        need, slot, text = col.get('need'), col.get('slot'), col.get('text')
        if need not in NEEDS:
            errs.append(f'{at} need must be required, optional or none')
        elif need == 'none':
            errs += [f'{at} is not required but {what}' for what, v in (('names a slot', slot), ('has text', text)) if v is not None]
        elif slot and text is not None:
            errs.append(f'{at} names a slot and text, use one')
        elif text is not None:
            if name in ('from', 'to'):
                errs.append(f'{at} must name a date slot, not text')
            else:
                used |= _text_use(text, f'column {name}', where, limits, slots, errs)
        elif not slot:
            errs.append(f'{at} needs a slot')
        elif _split(slot) is None:
            errs.append(f'{at} has a malformed slot reference')
        else:
            sid = _use(_split(slot), where, slots, errs)
            used.add(sid)
            if name in ('from', 'to') and sid in slots and (slots[sid].get('kind') not in ('date',) + COMPUTED or '.' in slot):
                errs.append(f'{at} must name a date slot')
    return used

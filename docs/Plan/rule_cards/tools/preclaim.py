"""Validate and ship preclaim.json, the TA/DA pre-claim worksheet content (plan 22, P3).

The worksheet's field order, document lists, dropdown options and LTC rule picker are data, so a portal change is a data edit and
no code. A move is a list of fields in the portal's order; each field says who fills it (the portal, the officer, or the app as an
estimate). Anything the portal was not seen to confirm is marked `unverified` and carries a hint saying what. compile.py calls
validate(); bundle.py calls for_bundle() and ships the result as the additive `preclaim` section (BUNDLE_VERSION stays 1).
GuidePreClaimValidator.kt rechecks the shape on every load, reading the length limits from the shipped section.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

PRECLAIM_PATH = os.path.join(CARDS_DIR, 'preclaim.json')
SUPPORTED_VERSION = 1
MOVES = ('PD', 'TD', 'LTC')
KINDS = {'text', 'date', 'number', 'amount', 'tick', 'choice'}
SOURCES = {'portal_filled', 'officer_types', 'app_suggests'}
# The one home of the length limits: they ship in the section (`limits`), and the app's validator reads them from there.
LIMITS = {'label': 60, 'hint': 160, 'text_chars': 200}
ID_MAX = 32
# Fields the app needs. `provenance` and any other key stay in the repo.
OPTION_KEYS = ('id', 'label', 'use_when', 'short', 'card')
MAX_SHORT_WORDS = 3
FIELD_KEYS = ('id', 'label', 'kind', 'source', 'max_chars', 'options', 'hint', 'unverified')
DOCUMENT_KEYS = ('id', 'label', 'when')


def load():
    with open(PRECLAIM_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def _is_id(value):
    return isinstance(value, str) and 0 < len(value) <= ID_MAX and all(c.islower() or c.isdigit() or c == '_' for c in value) and value[0].isalpha()


def _int(value):
    return isinstance(value, int) and not isinstance(value, bool)


def _text_problems(where, name, value, limit, required=True):
    """Plain text only: the worksheet is drawn as text, so angle brackets are refused rather than escaped."""
    if value is None and not required:
        return []
    if not isinstance(value, str) or not value.strip():
        return [f'{where}: {name} is empty']
    errs = []
    if len(value) > limit:
        errs.append(f'{where}: {name} is {len(value)} characters, over {limit}')
    if '<' in value or '>' in value:
        errs.append(f'{where}: {name} has angle brackets (plain text only)')
    return errs


def _duplicate_ids(where, items):
    ids = [i.get('id') for i in items]
    return [f'{where}: duplicate id {i!r}' for i in sorted({x for x in ids if ids.count(x) > 1}, key=str)]


def validate(data, cards):
    """Every problem found, as readable lines. Empty means the file is fit to bundle. `cards` are the compiled rulebook cards."""
    errs = []
    if data.get('version') != SUPPORTED_VERSION:
        errs.append(f'preclaim: version must be {SUPPORTED_VERSION}')
    errs += _upload_problems(data.get('upload') or {})
    groups = data.get('options') or {}
    live_cards = {c['id'] for c in cards if not c.get('replaced_by')}
    for gid, group in groups.items():
        errs += _group_problems(gid, group, live_cards, {c['id'] for c in cards})
    moves = data.get('moves') or {}
    errs += [f'preclaim: unknown move {m!r}' for m in moves if m not in MOVES]
    errs += [f'preclaim: move {m} is missing' for m in MOVES if m not in moves]
    for mid, move in moves.items():
        if mid in MOVES:
            errs += _move_problems(mid, move, groups)
    return errs


def _upload_problems(upload):
    errs = []
    if upload.get('files') != 1:
        errs.append('preclaim: upload files must be 1 (the portal takes one scanned PDF)')
    for key in ('more_than_kb', 'less_than_mb'):
        if not (_int(upload.get(key)) and upload[key] > 0):
            errs.append(f'preclaim: upload {key} must be a positive whole number')
    if upload.get('format') != 'PDF':
        errs.append('preclaim: upload format must be PDF')
    return errs


def _group_problems(gid, group, live_cards, known_cards):
    where = f'preclaim: options {gid}'
    errs = [] if _is_id(gid) else [f'{where}: group id must be lowercase letters, digits and underscores']
    options = group.get('options') or []
    if not options:
        errs.append(f'{where}: no options')
    linked = group.get('card_linked') is True
    errs += _duplicate_ids(where, options)
    for opt in options:
        at = f'{where} option {opt.get("id")!r}'
        errs += [] if _is_id(opt.get('id')) else [f'{at}: id must be lowercase letters, digits and underscores']
        errs += _text_problems(at, 'label', opt.get('label'), LIMITS['label'])
        errs += _text_problems(at, 'use_when', opt.get('use_when'), LIMITS['hint'], required=linked)
        errs += _text_problems(at, 'short', opt.get('short'), LIMITS['label'], required=False)
        if isinstance(opt.get('short'), str) and len(opt['short'].split()) > MAX_SHORT_WORDS:
            errs.append(f'{at}: short is over {MAX_SHORT_WORDS} words')
        card = opt.get('card')
        if not linked:
            errs += [f'{at}: names a card but the group is not card_linked'] if card else []
        elif not card:
            errs.append(f'{at}: needs a card (the group is card_linked)')
        elif card not in known_cards:
            errs.append(f'{at}: card {card} is not in the rulebook')
        elif card not in live_cards:
            errs.append(f'{at}: card {card} is replaced, so it is homed nowhere')
    return errs


def _move_problems(mid, move, groups):
    where = f'preclaim: move {mid}'
    errs = _text_problems(where, 'title', move.get('title'), LIMITS['label'])
    fields, docs = move.get('fields') or [], move.get('documents') or []
    if not fields:
        errs.append(f'{where}: no fields')
    if not docs:
        errs.append(f'{where}: no documents')
    errs += _duplicate_ids(f'{where} fields', fields) + _duplicate_ids(f'{where} documents', docs)
    for f in fields:
        errs += _field_problems(f'{where} field {f.get("id")!r}', f, groups)
    for d in docs:
        at = f'{where} document {d.get("id")!r}'
        errs += [] if _is_id(d.get('id')) else [f'{at}: id must be lowercase letters, digits and underscores']
        errs += _text_problems(at, 'label', d.get('label'), LIMITS['hint'])
        errs += _text_problems(at, 'when', d.get('when'), LIMITS['hint'], required=False)
    for note in move.get('notes') or []:
        errs += _text_problems(where, 'note', note, LIMITS['hint'])
    return errs


def _field_problems(at, f, groups):
    errs = [] if _is_id(f.get('id')) else [f'{at}: id must be lowercase letters, digits and underscores']
    errs += _text_problems(at, 'label', f.get('label'), LIMITS['label'])
    errs += _text_problems(at, 'hint', f.get('hint'), LIMITS['hint'], required=False)
    if f.get('kind') not in KINDS:
        errs.append(f'{at}: kind must be one of {sorted(KINDS)}')
    if f.get('source') not in SOURCES:
        errs.append(f'{at}: source must be one of {sorted(SOURCES)}')
    if 'unverified' in f and f['unverified'] is not True:
        errs.append(f'{at}: unverified, when present, must be true')
    if f.get('unverified') and not f.get('hint'):
        errs.append(f'{at}: an unverified field needs a hint saying what is unconfirmed')
    if 'max_chars' in f:
        limit = f['max_chars']
        if f.get('kind') != 'text':
            errs.append(f'{at}: max_chars only applies to a text field')
        if not (_int(limit) and 0 < limit <= LIMITS['text_chars']):
            errs.append(f'{at}: max_chars must be a whole number from 1 to {LIMITS["text_chars"]}')
    if f.get('kind') == 'choice':
        if f.get('options') not in groups:
            errs.append(f'{at}: options names {f.get("options")!r}, which is not an option group')
    elif 'options' in f:
        errs.append(f'{at}: options only applies to a choice field')
    return errs


def for_bundle(data):
    """The slice the app ships: whitelisted keys only, authored order kept. Call validate() first."""
    def pick(item, keys):
        return {k: item[k] for k in keys if k in item}
    return {
        'limits': dict(LIMITS),
        'upload': dict(data['upload']),
        'options': {g: {'card_linked': grp['card_linked'], 'options': [pick(o, OPTION_KEYS) for o in grp['options']]}
                    for g, grp in data['options'].items()},
        'moves': {m: {**pick(move, ('title', 'notes')), 'fields': [pick(f, FIELD_KEYS) for f in move['fields']],
                      'documents': [pick(d, DOCUMENT_KEYS) for d in move['documents']]}
                  for m, move in data['moves'].items()},
    }


def texts(data):
    """Every sentence a user reads, for the own-words guard (copycheck.py)."""
    out = []
    for grp in data['options'].values():
        out += [o[k] for o in grp['options'] for k in ('use_when', 'short') if o.get(k)]
    for move in data['moves'].values():
        out += list(move.get('notes', []))
        out += [f['hint'] for f in move['fields'] if f.get('hint')]
        out += [d[k] for d in move['documents'] for k in ('label', 'when') if d.get(k)]
    return out

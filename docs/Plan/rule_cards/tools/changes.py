#!/usr/bin/env python3
"""Rule-change metadata for the Claim Guide: card replacement and the dated change log.

Replacement: a card line may carry `effective=YYYY-MM-DD` (the date the rule applies from) and `replaces=RB-x`. The old card
stays in the data (arrears are worked out under the old rule) and gains `replaced_by` and `until` (the new card's effective
date); nav.py then keeps it off the tiles. Checked here: the target exists, dates are in order, no card is replaced twice, no loop.

Change log: authoring/changes.txt is written by hand, one dated block per release note (not read as card text):
  == 2026-11-15
  - Food rate raised from the 8th CPC order [RB-SS-T181, RB-SS-T184]
bundle.py ships the newest 12 entries. Card ids in brackets are validated against the compiled cards.
"""
import datetime
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))

CARD_ID = re.compile(r'^RB-[A-Za-z0-9_-]+$')
ITEM = re.compile(r'^- (.*?)\s*(?:\[([^\]]*)\])?\s*$')
SHIPPED_ENTRIES = 12
# Change text is shown to every user, locked or not, so it may not state a rupee amount (that is a card's paid half).
RUPEE_FIGURE = re.compile(r'(?:\u20b9|\bRs\.?)\s*\d', re.IGNORECASE)


def is_date(value):
    """A strict YYYY-MM-DD calendar date (no 2026-11-5, no 2026-13-01)."""
    try:
        return isinstance(value, str) and len(value) == 10 and datetime.date.fromisoformat(value).isoformat() == value
    except ValueError:
        return False


def apply_replacements(cards, errors):
    """Set `replaced_by` and `until` on every card that another card replaces. Cards are dicts with `id` and optional `effective`/`replaces`."""
    by_id = {c['id']: c for c in cards}
    replaced_by = {}
    for new in cards:
        old_id = new.get('replaces')
        if not old_id:
            continue
        old = by_id.get(old_id)
        if not CARD_ID.match(old_id) or old is None:
            errors.append(f'{new["id"]} replaces {old_id}: no such card')
            continue
        if old_id in replaced_by:
            errors.append(f'{old_id} is already replaced by {replaced_by[old_id]}; a card has one successor')
            continue
        if not new.get('effective'):
            errors.append(f'{new["id"]} replaces {old_id} but has no effective= date (it becomes the old card\'s until date)')
            continue
        if old.get('effective') and old['effective'] >= new['effective']:
            errors.append(f'dates out of order: {old_id} applies from {old["effective"]}, {new["id"]} from {new["effective"]}; the new rule must start later')
            continue
        replaced_by[old_id] = new['id']
        old['replaced_by'], old['until'] = new['id'], new['effective']
    for card in cards:
        seen, cur = set(), card
        while cur.get('replaces') in by_id:
            if cur['id'] in seen:
                errors.append(f'replacement cycle through {card["id"]}')
                break
            seen.add(cur['id'])
            cur = by_id[cur['replaces']]


def parse_log(lines, card_ids, errors, name='changes.txt'):
    """Entries newest first: [{'date': ..., 'items': [{'text': ..., 'cards': [...]}]}]. Problems go to `errors` with file:line."""
    entries, cur, dates = [], None, set()
    for n, raw in enumerate(lines, 1):
        line, where = raw.rstrip('\n'), f'{name}:{n}'
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        if line.startswith('== '):
            date = line[3:].strip()
            cur = {'date': date, 'items': []}
            if not is_date(date):
                errors.append(f'{where}: bad date {date!r} (use YYYY-MM-DD)')
            elif date in dates:
                errors.append(f'{where}: date {date} appears twice; merge the entries')
            dates.add(date)
            entries.append(cur)
        elif line.startswith('- ') and cur is not None:
            m = ITEM.match(line)
            text = m.group(1).strip()
            ids = [x.strip() for x in (m.group(2) or '').split(',') if x.strip()]
            if not text:
                errors.append(f'{where}: change item has no text')
            if RUPEE_FIGURE.search(text):
                errors.append(f'{where}: change text states a rupee figure; free users read it, so describe the change without the amount')
            for i in ids:
                if i not in card_ids:
                    errors.append(f'{where}: unknown card {i}')
            cur['items'].append({'text': text, 'cards': ids})
        else:
            errors.append(f'{where}: unrecognised line (expected "== YYYY-MM-DD" or "- text [RB-x]"): {line[:50]}')
    for e in entries:
        if not e['items']:
            errors.append(f'{name}: entry {e["date"]} has no items')
    return sorted(entries, key=lambda e: e['date'], reverse=True)


def load_log(path, card_ids, errors):
    if not os.path.exists(path):
        return []
    with open(path, encoding='utf-8') as fh:
        return parse_log(fh.read().splitlines(), card_ids, errors, os.path.basename(path))


def latest(entries, n=SHIPPED_ENTRIES):
    return entries[:n]

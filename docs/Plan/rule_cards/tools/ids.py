#!/usr/bin/env python3
"""Stable card IDs: ids_lock.json lists every card ID that has ever shipped.

A card ID is derived from its first `from=` source (compile.py) unless `id=` is given, so reordering sources would
silently rename a card and orphan pins, notes and the expert's references. compile.py therefore refuses to drop an ID
in the lock unless an authoring file says `--- retire RB-x reason`. New IDs are added by refresh.py, never by hand.
A retired ID stays in the lock, so it can never be reused for a different card.

Usage: ids.py --check | --update   (reads rulebook.json, so run it after compile.py)
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR, LOCK_PATH


def load_lock(path=LOCK_PATH):
    if not os.path.exists(path):
        return set()
    with open(path, encoding='utf-8') as fh:
        return set(json.load(fh))


def check(current, lock, retired):
    """Errors for IDs that vanished without a retire line, and for retire lines that make no sense."""
    errors = [f'card id {i} is in ids_lock.json but no card has it: restore it, or add "--- retire {i} <reason>"'
              for i in sorted(lock - current - set(retired))]
    for i in sorted(retired):
        if i in current:
            errors.append(f'{i} is retired but a card still uses that id (retired ids are never reused)')
        elif i not in lock:
            errors.append(f'retire of {i}: no such shipped id')
    return errors


def stale(current, lock):
    return sorted(current - lock)


def updated(current, lock):
    return sorted(current | lock)


def render(id_list):
    return json.dumps(sorted(id_list), indent=0) + '\n'


def main(argv):
    with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
        current = {c['id'] for c in json.load(fh)['cards']}
    lock = load_lock()
    if '--update' in argv:
        text = render(updated(current, lock))
        with open(LOCK_PATH, 'w', encoding='utf-8') as fh:
            fh.write(text)
        print(f'ids_lock.json: {len(json.loads(text))} ids ({len(stale(current, lock))} added)')
        return 0
    new = stale(current, lock)
    if new:
        print(f'ids_lock.json is stale, run tools/refresh.py: {", ".join(new[:5])}{" ..." if len(new) > 5 else ""}')
        return 1
    print('ids_lock.json up to date')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))

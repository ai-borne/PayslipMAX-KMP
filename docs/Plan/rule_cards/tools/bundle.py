#!/usr/bin/env python3
"""Turn rulebook.json into the Claim Guide bundle the app ships (config.BUNDLE_PATH).

Only whitelisted fields ship: internal provenance (`from`), reviewer notes (`open`) and the coverage bookkeeping
stay in the repo. Each card gains `unverified` (it carries an open point) and the bundle gains `rates_as_of` and the owner-approved
`figures` (figures.json; authority and evidence stay in the repo).
Rule-change metadata is additive (BUNDLE_VERSION stays 1): every card gains `rev`, the first 8 hex characters of the SHA-256 of
its shipped text fields (see card_rev), so a note can tell the card changed since it was written; a dated or replaced card also
gains `effective`, `replaced_by` and `until` (omitted when empty); the bundle gains `changes`, the newest 12 change-log entries.
The output is compact and deterministic, so test_bundle.py can fail the build when it is stale.

Usage: bundle.py [--check]   (--check exits non-zero if the committed bundle is out of date)
Run after compile.py whenever rulebook.json changes.
"""
import hashlib
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import changes
import figures
from config import BUNDLE_PATH, BUNDLE_VERSION, CARDS_DIR, RATES_AS_OF

CARD_KEYS = ('id', 'domain', 'topic', 'title', 'answer', 'key', 'attach', 'watch', 'cite', 'details', 'chips',
             'personal', 'status', 'facet', 'nav')
# Dated-rule fields ship only on the cards that have them, which keeps the bundle small for the 400 that do not.
OPTIONAL_CARD_KEYS = ('effective', 'replaced_by', 'until')
# The words a user reads. A change to any of them changes `rev`; reviewer notes, source ids and dates do not.
REV_TEXT_FIELDS = ('title', 'answer', 'key', 'attach', 'watch', 'cite', 'details')


def card_rev(card):
    text = json.dumps([card[f] for f in REV_TEXT_FIELDS], ensure_ascii=False, separators=(',', ':'))
    return hashlib.sha256(text.encode('utf-8')).hexdigest()[:8]


def ship_card(card):
    out = {k: card[k] for k in CARD_KEYS}
    out.update(rev=card_rev(card), unverified=bool(card['open']))
    out.update({k: card[k] for k in OPTIONAL_CARD_KEYS if card.get(k)})
    return out


def build(rulebook, figures_data=None):
    figures_data = figures.load() if figures_data is None else figures_data
    cards = [ship_card(c) for c in rulebook['cards']]
    return {'version': BUNDLE_VERSION, 'generated': rulebook['generated'], 'rates_as_of': RATES_AS_OF,
            'limits': rulebook['limits'], 'nav': rulebook['nav'], 'facets': rulebook['facets'], 'cards': cards,
            'figures': figures.for_bundle(figures_data), 'changes': changes.latest(rulebook.get('changes', []))}


def render(data):
    return json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n'


def main():
    with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
        text = render(build(json.load(fh)))
    if '--check' in sys.argv:
        current = open(BUNDLE_PATH, encoding='utf-8').read() if os.path.exists(BUNDLE_PATH) else ''
        if current != text:
            sys.exit(f'stale bundle: run python3 docs/Plan/rule_cards/tools/bundle.py ({BUNDLE_PATH})')
        print('bundle up to date')
        return
    os.makedirs(os.path.dirname(BUNDLE_PATH), exist_ok=True)
    with open(BUNDLE_PATH, 'w', encoding='utf-8') as fh:
        fh.write(text)
    print(f'wrote {os.path.relpath(BUNDLE_PATH)} ({len(text.encode("utf-8"))} bytes)')


if __name__ == '__main__':
    main()

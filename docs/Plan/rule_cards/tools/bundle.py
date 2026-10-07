#!/usr/bin/env python3
"""Turn rulebook.json into the Claim Guide bundle the app ships (config.BUNDLE_PATH).

Only whitelisted fields ship: internal provenance (`from`), reviewer notes (`open`) and the coverage bookkeeping
stay in the repo. Each card gains `unverified` (it carries an open point) and the bundle gains `rates_as_of`.
The output is compact and deterministic, so test_bundle.py can fail the build when it is stale.

Usage: bundle.py [--check]   (--check exits non-zero if the committed bundle is out of date)
Run after compile.py whenever rulebook.json changes.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import BUNDLE_PATH, BUNDLE_VERSION, CARDS_DIR, RATES_AS_OF

CARD_KEYS = ('id', 'domain', 'topic', 'title', 'answer', 'key', 'attach', 'watch', 'cite', 'details', 'chips',
             'personal', 'status', 'facet', 'nav')


def build(rulebook):
    cards = [dict({k: c[k] for k in CARD_KEYS}, unverified=bool(c['open'])) for c in rulebook['cards']]
    return {'version': BUNDLE_VERSION, 'generated': rulebook['generated'], 'rates_as_of': RATES_AS_OF,
            'limits': rulebook['limits'], 'nav': rulebook['nav'], 'facets': rulebook['facets'], 'cards': cards}


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

#!/usr/bin/env python3
"""The 8th CPC sweep list: every current card that could quote a rate, and every shipped figure.

A card is listed when it carries the RATES chip, has a digit in its title, answer, key points, attach, watch-out or details,
has a % or "Rs" / rupee sign anywhere (the cite included), or is the card of a figure in figures.json. A digit in the cite alone
is not enough: nearly every cite is a rule number ("Rule 114 TR") and counting those would list the whole book. The list errs
towards too many cards, never too few. Replaced cards are not current, so they are left out. review_pack.py prints it in the
expert hand-out's appendix. No templating in card prose (EP 22): this list is how the next commission's changes are found.

Usage: rates_report.py   (reads rulebook.json and figures.json; prints a plain-text table)
"""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

BODY_FIELDS = ('title', 'answer', 'key', 'attach', 'watch', 'details')
RUPEE = re.compile(r'\bRs\b|₹')


def _text(card, fields):
    return ' '.join(' '.join(v) if isinstance(v, list) else v for v in (card[f] for f in fields))


def flags(card):
    """Why a card is on the list, in a fixed order; empty when it is not."""
    body, whole = _text(card, BODY_FIELDS), _text(card, BODY_FIELDS + ('cite',))
    found = [('RATES chip', 'RATES' in card['chips']), ('digit', any(ch.isdigit() for ch in body)), ('%', '%' in whole),
             ('Rs', bool(RUPEE.search(whole)))]
    return [name for name, hit in found if hit]


def report(rulebook, figures_data):
    figure_cards = {f['card'] for f in figures_data['figures'].values()}
    rows = []
    for card in rulebook['cards']:
        if card.get('replaced_by'):
            continue
        why = flags(card) + (['figure'] if card['id'] in figure_cards else [])
        if why:
            rows.append({'id': card['id'], 'title': card['title'], 'flags': why})
    figs = [{'key': k, 'card': f['card'], 'effective_from': f.get('effective_from', 'see rate table'), 'evidence': f['evidence']}
            for k, f in sorted(figures_data['figures'].items())]
    return {'cards': rows, 'figures': figs}


def render_text(rep):
    lines = [f'{len(rep["cards"])} cards to re-check for rates', '']
    lines += [f'{r["id"]:<24} {", ".join(r["flags"]):<28} {r["title"]}' for r in rep['cards']]
    lines += ['', f'{len(rep["figures"])} figures (figures.json)', '']
    lines += [f'{r["key"]:<24} {r["card"]:<24} from {r["effective_from"]}  {r["evidence"]}' for r in rep['figures']]
    return '\n'.join(lines) + '\n'


def main():
    with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
        rulebook = json.load(fh)
    with open(os.path.join(CARDS_DIR, 'figures.json'), encoding='utf-8') as fh:
        figures_data = json.load(fh)
    sys.stdout.write(render_text(report(rulebook, figures_data)))


if __name__ == '__main__':
    main()

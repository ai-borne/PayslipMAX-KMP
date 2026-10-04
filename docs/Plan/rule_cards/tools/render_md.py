#!/usr/bin/env python3
"""Render docs/Plan/rule_cards/rulebook.json into the human reference RULEBOOK.md.

The markdown is generated, never hand-edited: change the authoring files, run compile.py, then this.
"""
import json
import os
from collections import defaultdict

CARDS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
DOMAIN_LABEL = {'GEN': 'General rules and claims', 'MOD': 'Modes of travel', 'PDM': 'Permanent duty move', 'TD': 'Temporary duty',
                'LTC': 'Leave Travel Concession', 'RET': 'Retirement, release, death', 'TPT': 'Transport allowance',
                'ORD': 'Orders and letters (post-TR)'}


def card_md(c):
    chips = f"  `{'` `'.join(c['chips'])}`" if c['chips'] else ''
    lines = [f"#### {c['id']} {c['title']}{chips}", '', f"**{c['answer']}**", '']
    lines += [f'- {k}' for k in c['key']]
    if c['attach']:
        lines += [f'- *Attach:* {a}' for a in c['attach']]
    if c['watch']:
        lines += [f'- *Watch out:* {w}' for w in c['watch']]
    lines.append('')
    meta = [f"Cite: {c['cite']}" if c['cite'] else 'Cite: none (guidance)']
    if c['personal']:
        meta.append(f"personal: {c['personal']}")
    lines.append(' · '.join(meta))
    if c['details']:
        lines += ['', f"<sub>Details: {c['details']}</sub>"]
    if c['open']:
        lines += ['', *[f'> Open point: {o}' for o in c['open']]]
    lines.append('')
    return lines


def main():
    data = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))
    topics = {t['id']: t for t in data['topics']}
    by = defaultdict(list)
    for c in data['cards']:
        by[c['topic']].append(c)
    cov = data['coverage']
    out = ['# PayslipMax rulebook: reference (generated)', '',
           f"Generated from [rulebook.json](rulebook.json) on {data['generated']}. **Do not edit by hand.** "
           f"{cov['cards']} cards ({cov['cards_by_domain'].get('travel', 0)} travel, {cov['cards_by_domain'].get('pay', 0)} pay). "
           f"Source entries covered: {cov['covered']} of {cov['entries_total']} (skipped with a reason: {cov['skipped']}; uncovered: {cov['uncovered']}).", '',
           'Each card: one-line answer in bold, short bullets, the authority to cite, optional collapsed details. '
           '"Open point" lines are for reviewers and are never shown to users. Card text is written in our own words.', '']
    out.append('## Contents')
    out.append('')
    for dom, label in (('travel', 'Travel'), ('pay', 'Pay and allowances')):
        n = sum(1 for c in data['cards'] if c['domain'] == dom)
        out.append(f'- **{label}**: {n} cards')
    out.append('')
    for dom, title in (('travel', 'Travel'), ('pay', 'Pay and allowances')):
        out += [f'## {title}', '']
        order = [t for t in data['topics'] if (t['id'].startswith('RR-') if dom == 'travel' else t['id'].startswith('RP-'))]
        group = None
        for t in order:
            cards = by.get(t['id'], [])
            if not cards:
                continue
            if dom == 'travel':
                g = DOMAIN_LABEL.get(t.get('domain'), '')
                if g != group:
                    out += [f'### {g}', '']
                    group = g
            else:
                g = f"Chapter {t.get('chapter')}"
                if g != group:
                    out += [f'### {g}', '']
                    group = g
            out += [f"##### {t['title']} ({len(cards)})", '']
            for c in cards:
                out += card_md(c)
    with open(os.path.join(CARDS_DIR, 'RULEBOOK.md'), 'w', encoding='utf-8') as fh:
        fh.write('\n'.join(out))
    print('wrote RULEBOOK.md', len(out), 'lines')


if __name__ == '__main__':
    main()

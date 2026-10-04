"""Tile navigation for the Claim Guide: validate nav.json and attach each card's place in the tree.

nav.json (authored) defines areas > cases. A case collects cards by `topics` (every card of those
topics) and/or explicit `cards` ids; `also` lists extra card ids shown in that case but homed elsewhere.
A card needs exactly one home. Limits live in nav.json so the app and the checker agree.
"""
import json
import os
import re

WORD = re.compile(r"\S+")


def load_nav(cards_dir):
    with open(os.path.join(cards_dir, 'nav.json'), encoding='utf-8') as fh:
        return json.load(fh)


def build_nav(cards, nav, errors):
    """Return (tree, homes). tree mirrors nav.json plus resolved card ids; homes maps card id -> case id."""
    lim = nav['limits']
    by_id = {c['id']: c for c in cards}
    homes, case_ids, tree = {}, set(), []
    areas = nav['areas']
    if not lim['areas'][0] <= len(areas) <= lim['areas'][1]:
        errors.append(f"nav: {len(areas)} areas, allowed {lim['areas'][0]} to {lim['areas'][1]}")
    explicit = {i for a in areas for c in a['cases'] for i in c.get('cards', [])}
    for a in areas:
        if len(a['cases']) > lim['cases_per_area']:
            errors.append(f"nav: area '{a['title']}' has {len(a['cases'])} cases, max {lim['cases_per_area']}")
        out_cases = []
        for c in a['cases']:
            if c['id'] in case_ids:
                errors.append(f"nav: duplicate case id {c['id']}")
            case_ids.add(c['id'])
            for label, text, cap in (('title', c['title'], lim['title_words']), ('sub', c.get('sub', ''), lim['sub_words'])):
                if len(WORD.findall(text)) > cap:
                    errors.append(f"nav: case {c['id']} {label} over {cap} words")
            members = list(c.get('cards', []))
            members += [k['id'] for k in cards if k['topic'] in c.get('topics', []) and k['id'] not in explicit]
            for i in members:
                if i not in by_id:
                    errors.append(f"nav: unknown card {i} in {c['id']}")
                elif i in homes:
                    errors.append(f"nav: card {i} homed twice ({homes[i]}, {c['id']})")
                else:
                    homes[i] = c['id']
            for i in c.get('also', []):
                if i not in by_id:
                    errors.append(f"nav: unknown also-card {i} in {c['id']}")
            n = len(members)
            if not lim['cards_per_case'][0] <= n <= lim['cards_per_case'][1]:
                errors.append(f"nav: case '{c['title']}' has {n} cards, allowed {lim['cards_per_case'][0]} to {lim['cards_per_case'][1]}")
            out_cases.append({'id': c['id'], 'title': c['title'], 'sub': c.get('sub', ''), 'cards': members, 'also': c.get('also', [])})
        tree.append({'id': a['id'], 'title': a['title'], 'cases': out_cases})
    for k in cards:
        if k['id'] not in homes:
            errors.append(f"nav: card {k['id']} ({k['topic']}) has no home")
    return tree, homes


def apply_facets(cards, cards_dir, errors):
    """facets.json maps every card id to one facet key (the filter chips inside a case feed)."""
    with open(os.path.join(cards_dir, 'facets.json'), encoding='utf-8') as fh:
        fac = json.load(fh)
    ids = {c['id'] for c in cards}
    for c in cards:
        k = fac['cards'].get(c['id'])
        if k not in fac['labels']:
            errors.append(f"facet: card {c['id']} has no valid facet ({k!r})")
        c['facet'] = k or ''
    for i in fac['cards']:
        if i not in ids:
            errors.append(f"facet: unknown card {i}")
    return fac['labels']

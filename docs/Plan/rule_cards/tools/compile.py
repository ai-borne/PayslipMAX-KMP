#!/usr/bin/env python3
"""Compile the rule-card authoring files into one dataset and validate it.

Inputs : docs/Plan/rule_cards/authoring/*.txt  (our own-words cards, compact line format)
         docs/Plan/rule_cards/ssot.json        (topics and the 474 source entries; the coverage target)
         docs/Plan/rule_cards/authoring/changes.txt  (hand-written dated change log; read apart from the card files)
Outputs: docs/Plan/rule_cards/rulebook.json    (canonical dataset; the app will load this)

Authoring format (one block per card; '#' lines are comments):
  === topic=RR-TD-02 from=SS-T020,SS-T021 [id=RB-x] [chips=RATES,AMENDED] [personal=level:food_rate] [status=draft]
      [effective=YYYY-MM-DD] [replaces=RB-x]   (a dated rule; the replaced card leaves the tiles, see changes.py)
  T: question-style title
  A: one-line answer
  K: key point            (up to 3)
  H: attach item          (optional, up to 3)
  W: watch-out item       (optional, up to 3)
  C: authority to cite    (empty only for guidance cards)
  D: collapsed details    (optional)
  O: open point for the reviewer (optional, never shown to users)
  --- skip SS-T123 reason text
  --- retire RB-x reason text   (a shipped card is deliberately removed; see ids.py)

Usage: compile.py [--check [--fresh]]   (--check validates without writing; --fresh also fails if rulebook.json is stale)
Exit status is non-zero on any error, so it can gate a commit.
"""
import glob
import json
import os
import re
import sys
from collections import Counter, defaultdict

sys.path.insert(0, os.path.dirname(__file__))
import changes as chgmod
import figures as figmod
import ids as idsmod
import nav as navmod

CARDS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
AUTH_DIR = os.path.join(CARDS_DIR, 'authoring')
LIMITS = {'answer': 25, 'bullets': 3, 'bullet_words': 12, 'visible': 90, 'details': 120, 'title': 14}
CHANGE_LOG = 'changes.txt'
CHIPS = {'RATES', 'AMENDED', 'GUIDANCE'}
CITE_OK = re.compile(r'(MHA|Rule|Para|TR|AO|SAO|SAI|MoD|MoF|DoE|DoPT|IHQ|CGDA|OM|SRO|Pay Rules|Army|Handbook|Regulation|Section|Sec|FR|CCS|GFR|DFPDS|Note|Appx|Order|Advisory)', re.I)
WORD = re.compile(r"[A-Za-z0-9₹%.,/'()&+-]+")


def card_files():
    """Card authoring files; the change log shares the folder but has its own format."""
    return [p for p in sorted(glob.glob(os.path.join(AUTH_DIR, '*.txt'))) if os.path.basename(p) != CHANGE_LOG]


def wc(text):
    return len(WORD.findall(text))


def parse(path, errors):
    cards, skips, retires, cur = [], [], {}, None
    with open(path, encoding='utf-8') as fh:
        for n, raw in enumerate(fh, 1):
            line = raw.rstrip('\n')
            if not line.strip() or line.lstrip().startswith('#'):
                continue
            where = f'{os.path.basename(path)}:{n}'
            if line.startswith('=== '):
                cur = {'attrs': dict(kv.split('=', 1) for kv in line[4:].split() if '=' in kv), 'key': [], 'attach': [], 'watch': [],
                       'T': '', 'A': '', 'C': '', 'D': '', 'open': [], 'where': where}
                cards.append(cur)
            elif line.startswith('--- skip '):
                m = re.match(r'--- skip (SS-[TP]\d+)\s+(.+)', line)
                if not m:
                    errors.append(f'{where}: bad skip line')
                else:
                    skips.append({'from': m.group(1), 'reason': m.group(2).strip()})
            elif line.startswith('--- retire '):
                m = re.match(r'--- retire (RB-\S+)\s+(.+)', line)
                if not m:
                    errors.append(f'{where}: bad retire line (needs an id and a reason)')
                else:
                    retires[m.group(1)] = m.group(2).strip()
            elif cur is not None and re.match(r'^[TAKHWCDO]: ?', line):
                tag, val = line[0], line[3:].strip() if line[2:3] == ' ' else line[2:].strip()
                if tag == 'K': cur['key'].append(val)
                elif tag == 'H': cur['attach'].append(val)
                elif tag == 'W': cur['watch'].append(val)
                elif tag == 'O': cur['open'].append(val)
                else:
                    if cur[tag]:
                        errors.append(f'{where}: duplicate {tag}:')
                    cur[tag] = val
            else:
                errors.append(f'{where}: unrecognised line: {line[:50]}')
    return cards, skips, retires


def validate(card, topics, ssot_ids, errors, warns):
    a, w = card['attrs'], card['where']
    topic = a.get('topic', '')
    if topic not in topics:
        errors.append(f'{w}: unknown topic {topic!r}')
    frm = [x for x in a.get('from', '').split(',') if x]
    if not frm:
        errors.append(f'{w}: card has no from= source entries')
    for x in frm:
        if x not in ssot_ids:
            errors.append(f'{w}: unknown source entry {x}')
    chips = [c for c in a.get('chips', '').split(',') if c]
    for c in chips:
        if c not in CHIPS:
            errors.append(f'{w}: unknown chip {c}')
    if 'effective' in a and not chgmod.is_date(a['effective']):
        errors.append(f'{w}: effective={a["effective"]!r} is not a YYYY-MM-DD date')
    if 'replaces' in a and not chgmod.CARD_ID.match(a['replaces']):
        errors.append(f'{w}: replaces={a["replaces"]!r} is not a card id (RB-...)')
    if not card['T']: errors.append(f'{w}: missing T:')
    if not card['A']: errors.append(f'{w}: missing A:')
    if not card['key']: errors.append(f'{w}: needs at least one K:')
    if not card['C'] and 'GUIDANCE' not in chips:
        errors.append(f'{w}: no C: cite (add chips=GUIDANCE if there is genuinely no authority)')
    if card['C'] and 'GUIDANCE' in chips:
        # owner decision 2026-10-07: guidance means "no cite", so the app's "No official source" chip has one source
        errors.append(f'{w}: GUIDANCE card has a cite; drop the chip or the cite')
    if card['C'] and not CITE_OK.search(card['C']):
        warns.append(f'{w}: cite does not look like an authority: {card["C"][:50]}')
    if wc(card['T']) > LIMITS['title']: errors.append(f'{w}: title {wc(card["T"])} words > {LIMITS["title"]}')
    if wc(card['A']) > LIMITS['answer']: errors.append(f'{w}: answer {wc(card["A"])} words > {LIMITS["answer"]}')
    vis = wc(card['A']) + wc(card['C'])
    for sec, label in (('key', 'K'), ('attach', 'H'), ('watch', 'W')):
        if len(card[sec]) > LIMITS['bullets']:
            errors.append(f'{w}: {len(card[sec])} {label}: bullets > {LIMITS["bullets"]}')
        for b in card[sec]:
            vis += wc(b)
            if wc(b) > LIMITS['bullet_words']:
                errors.append(f'{w}: {label}: bullet {wc(b)} words > {LIMITS["bullet_words"]}: {b[:40]}')
    if vis > LIMITS['visible']: errors.append(f'{w}: visible text {vis} words > {LIMITS["visible"]}')
    if wc(card['D']) > LIMITS['details']: errors.append(f'{w}: details {wc(card["D"])} words > {LIMITS["details"]}')
    return frm, chips, vis


def same_content(a, b):
    """Compare two rulebooks, ignoring the generation date (it changes daily)."""
    return {k: v for k, v in a.items() if k != 'generated'} == {k: v for k, v in b.items() if k != 'generated'}


def main():
    check_only = '--check' in sys.argv
    ssot = json.load(open(os.path.join(CARDS_DIR, 'ssot.json'), encoding='utf-8'))
    topics = {t['id']: t for t in ssot['travel_topics'] + ssot['pay_topics']}
    ssot_ids = {e['id']: e for e in ssot['entries']}
    # handbook-only pay topics have no FAQ entries; a card may cite the topic id (RP-nnn) as its source instead
    pay_topic_ids = {t['id'] for t in ssot['pay_topics']}
    valid_sources = set(ssot_ids) | pay_topic_ids
    errors, warns, raw_cards, skips, retires = [], [], [], [], {}
    for path in card_files():
        c, s, r = parse(path, errors)
        raw_cards += c
        skips += s
        retires.update(r)
    out_cards, seen, covered, vis_total = [], set(), defaultdict(list), []
    for card in raw_cards:
        frm, chips, vis = validate(card, topics, valid_sources, errors, warns)
        cid = card['attrs'].get('id') or ('RB-' + frm[0] if frm else None)
        if cid in seen: errors.append(f'{card["where"]}: duplicate card id {cid}')
        seen.add(cid)
        for x in frm: covered[x].append(cid)
        if not card['C'] and 'GUIDANCE' not in chips: chips.append('GUIDANCE')
        vis_total.append(vis)
        dated = {k: card['attrs'][k] for k in ('effective', 'replaces') if card['attrs'].get(k)}
        out_cards.append({**dated, 'id': cid, 'domain': 'travel' if card['attrs'].get('topic', '').startswith('RR-') else 'pay',
                          'topic': card['attrs'].get('topic'), 'title': card['T'], 'answer': card['A'], 'key': card['key'],
                          'attach': card['attach'], 'watch': card['watch'], 'cite': card['C'], 'details': card['D'], 'chips': chips,
                          'personal': card['attrs'].get('personal', ''), 'from': frm, 'open': card['open'],
                          'status': card['attrs'].get('status', 'draft')})
    chgmod.apply_replacements(out_cards, errors)
    log = chgmod.load_log(os.path.join(AUTH_DIR, CHANGE_LOG), seen, errors)
    nav_tree, nav_homes = navmod.build_nav(out_cards, navmod.load_nav(CARDS_DIR), errors)
    facet_labels = navmod.apply_facets(out_cards, CARDS_DIR, errors)
    for c in out_cards:
        c['nav'] = nav_homes.get(c['id'], '')
    errors += idsmod.check(seen, idsmod.load_lock(), retires)
    errors += figmod.validate(figmod.load(), out_cards)
    skipped = {s['from'] for s in skips}
    for s in skips:
        if s['from'] not in ssot_ids: errors.append(f'skip of unknown entry {s["from"]}')
        if s['from'] in covered: warns.append(f'{s["from"]} is both skipped and used in a card')
    uncovered = [e for e in ssot_ids if e not in covered and e not in skipped]
    pay_topics_done = {c['topic'] for c in out_cards if c['topic'] in pay_topic_ids} | {x for x in covered if x in pay_topic_ids}
    pay_topics_open = sorted(pay_topic_ids - pay_topics_done)
    titles = Counter(c['title'].lower() for c in out_cards)
    for t, n in titles.items():
        if n > 1: warns.append(f'duplicate card title: {t}')
    dom = Counter(c['domain'] for c in out_cards)
    cov = {'entries_total': len(ssot_ids), 'covered': len(covered), 'skipped': len(skipped), 'uncovered': len(uncovered),
           'cards': len(out_cards), 'cards_by_domain': dict(dom),
           'pay_topics_total': len(pay_topic_ids), 'pay_topics_with_cards': len(pay_topic_ids) - len(pay_topics_open)}
    print(f"cards {len(out_cards)} ({dict(dom)}) | entries covered {len(covered)}/{len(ssot_ids)} | skipped {len(skipped)} | uncovered {len(uncovered)}")
    if vis_total: print(f"visible words: max {max(vis_total)}, mean {sum(vis_total)/len(vis_total):.0f}")
    for m in warns[:15]: print('WARN ', m)
    for m in errors[:40]: print('ERROR', m)
    if len(errors) > 40: print(f'... {len(errors)-40} more errors')
    print(f"nav: {len(nav_tree)} areas, {sum(len(a['cases']) for a in nav_tree)} cases, {len(nav_homes)} cards homed")
    print(f"pay topics with at least one card: {len(pay_topic_ids) - len(pay_topics_open)}/{len(pay_topic_ids)}")
    if '--uncovered' in sys.argv:
        print('UNCOVERED', ' '.join(uncovered))
        print('PAYTOPICS_OPEN', ' '.join(pay_topics_open))
    if errors: sys.exit(1)
    if check_only and '--fresh' not in sys.argv:
        return
    data = {'version': 1, 'generated': __import__('datetime').date.today().isoformat(), 'limits': LIMITS,
            'topics': [{k: t[k] for k in t if k in ('id', 'title', 'domain', 'chapter', 'handbook_page', 'tr_rules')} for t in topics.values()],
            'nav': nav_tree, 'facets': facet_labels, 'cards': out_cards, 'changes': log, 'skipped': skips, 'coverage': cov, 'uncovered': uncovered, 'pay_topics_open': pay_topics_open}
    if check_only:
        with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
            if not same_content(json.load(fh), data):
                sys.exit('stale rulebook.json: authoring files changed, run tools/refresh.py')
        print('rulebook.json up to date')
        return
    with open(os.path.join(CARDS_DIR, 'rulebook.json'), 'w', encoding='utf-8') as fh:
        json.dump(data, fh, indent=1, ensure_ascii=False)
    print('wrote rulebook.json')


if __name__ == '__main__':
    main()

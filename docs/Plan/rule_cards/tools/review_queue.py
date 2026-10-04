#!/usr/bin/env python3
"""Regenerate 13_review_queue.md from rulebook.json (cards with an open point, plus guidance-only cards)."""
import datetime
import json
import os

from config import CARDS_DIR

cards = json.load(open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8'))['cards']
out = [f'# Domain-owner review queue (generated {datetime.date.today().isoformat()})', '',
       'Cards with an open point. CONFLICT marks a disagreement between sources. Not shown to app users.', '']
for dom, label in (('pay', 'Pay'), ('travel', 'Travel')):
    sel = [c for c in cards if c['domain'] == dom and c['open']]
    out.append(f'## {label} ({len(sel)} cards)')
    for c in sel:
        out += [f'### {c["id"]}: {c["title"]}', f'- Cite: {c["cite"] or "(guidance, no cite)"}']
        for o in c['open']:
            out.append(f'- **CONFLICT**: {o}' if o.startswith('CONFLICT') else f'- **Open**: {o}')
    out.append('')
g = [c for c in cards if 'GUIDANCE' in c['chips']]
out.append('## Guidance-only cards (no authority found)')
out += [f'- {c["id"]}: {c["title"]}' for c in g]
open(os.path.join(CARDS_DIR, '13_review_queue.md'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print('queue:', {'pay': sum(1 for c in cards if c['domain'] == 'pay' and c['open']), 'travel': sum(1 for c in cards if c['domain'] == 'travel' and c['open']), 'guidance': len(g)})

"""Print source material for a set of SSOT entries so cards can be drafted in our own words."""
import json, re, sys
from sources import faq, ta, txt
from config import CARDS_DIR
import os
J = json.load(open(os.path.join(CARDS_DIR, "ssot.json")))
E = {e['id']: e for e in J['entries']}
def show(ids, ta_words=140):
    for i in ids:
        e = E[i]; print(f"\n### {i} [{e['topic']}] {e['kind']} support={e['support']} flags={','.join(e['flags'])} tr={','.join(e['tr'][:3])}")
        for f in e['faq']:
            x = faq[f]; print('Q:', txt(x['question'])); print('A:', txt(x['answer'])[:900])
        for t in e['ta']:
            u = ta[t]; body = re.sub(r'\s+', ' ', u['title'] + ' ' + u['text']); print('TA p%s:' % u['page'], ' '.join(body.split()[:ta_words]))
if __name__ == '__main__':
    arg = sys.argv[1]
    ids = [e['id'] for e in J['entries'] if e['topic'] == arg] if not arg.startswith('SS-') else arg.split(',')
    if len(sys.argv) > 2: ids = ids[int(sys.argv[2]):int(sys.argv[3])]
    show(ids)

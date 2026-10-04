import json, os, sys, re, subprocess
from config import CARDS_DIR
out = subprocess.run(['python3', os.path.join(os.path.dirname(__file__), 'compile.py'), '--check', '--uncovered'], capture_output=True, text=True).stdout
unc = set(re.search(r'UNCOVERED (.*)', out).group(1).split())
J = json.load(open(os.path.join(CARDS_DIR, 'ssot.json')))
pt = {t['id']: t['title'] for t in J['pay_topics']}
if len(sys.argv) == 1:
    import collections
    c = collections.Counter(pt[e['topic']][:42] for e in J['entries'] if e['domain'] == 'pay' and e['id'] in unc); print(len(unc), 'uncovered'); print(c.most_common())
else:
    keys = sys.argv[1].split('|')
    ids = [e['id'] for e in J['entries'] if e['domain'] == 'pay' and e['id'] in unc and any(pt[e['topic']].startswith(k) for k in keys)]
    from packet import show; show(ids)

import re, sys
from config import src
T = open(src('Handbook_Pay_and_Allowances_2023.txt'), errors='ignore').read()
T = re.sub(r'\f', '\n', T)
def section(head, n=1500, occ=None, skip=0):
    ms = [m.start() for m in re.finditer(re.escape(head), T)]
    if not ms: return f'[no match for {head!r}]'
    i = ms[occ] if occ is not None else (ms[1] if len(ms) > 1 else ms[0])
    t = T[i:]
    t = re.sub(r'Handbook on Pay & Allowances - 2023', '', t)
    t = re.sub(r'[ \t]+', ' ', t); t = re.sub(r'\n\s*\n+', '\n', t)
    w = t.split()
    return ' '.join(w[skip:skip + n])
if __name__ == '__main__':
    head = sys.argv[1]; n = int(sys.argv[2]) if len(sys.argv) > 2 else 1500
    occ = int(sys.argv[3]) if len(sys.argv) > 3 else None
    skip = int(sys.argv[4]) if len(sys.argv) > 4 else 0
    print(section(head, n, occ, skip))

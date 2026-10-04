"""Flag any run of N identical words between card text and the source corpora (guards the 'own words' decision)."""
import re, sys, json, glob
from config import AUTH_DIR, src
N = 8
def words(s): return re.findall(r"[a-z0-9]+", s.lower())
def grams(ws, n=N): return {tuple(ws[i:i+n]) for i in range(len(ws) - n + 1)}
def corpus():
    texts = []
    for f in ['Handbook_Pay_and_Allowances_2023.txt', 'Handbook_Travelling_Allowances_2023.txt', 'tr2014_ocr2.txt', 'prim/p_06bd09.txt', 'prim/cwc_air.txt', 'prim/bag2018.txt', 'prim/sports2018.txt', 'prim/fee2014.txt']:
        try: texts.append(open(src(f), errors='ignore').read())
        except FileNotFoundError: pass
    from sources import faq, txt
    for x in faq.values(): texts.append(txt(x['question']) + ' ' + txt(x['answer']))
    return texts
if __name__ == '__main__':
    import os
    G = set()
    for t in corpus(): G |= grams(words(t))
    A = AUTH_DIR + "/"
    bad = 0
    for p in sorted(glob.glob(A + '*.txt')):
        cur = None
        for n, line in enumerate(open(p), 1):
            if line.startswith('=== '): cur = line[:60].strip()
            m = re.match(r'^[TAKHWD]: (.*)', line)
            if m:
                ws = words(m.group(1))
                hit = [g for g in grams(ws) if g in G]
                if hit: bad += 1; print(f"{os.path.basename(p)}:{n} copies {len(hit)} 8-grams: \"{' '.join(hit[0])}\"")
    print('copy check:', 'CLEAN' if not bad else f'{bad} lines overlap sources')

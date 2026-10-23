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
def part2_texts(templates=None):
    """The Part II template words that are ours (slot labels and help). Narratives, authority lines and certificates keep the manual's wording on purpose, so they are not checked."""
    import part2_templates
    return part2_templates.texts(part2_templates.load_templates() if templates is None else templates)
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
            # card text lines (T: A: K: ...), and the items of the hand-written change log
            m = re.match(r'^- (.*)' if os.path.basename(p) == 'changes.txt' else r'^[TAKHWD]: (.*)', line)
            if m:
                ws = words(m.group(1))
                hit = [g for g in grams(ws) if g in G]
                if hit: bad += 1; print(f"{os.path.basename(p)}:{n} copies {len(hit)} 8-grams: \"{' '.join(hit[0])}\"")
    # the TA/DA pre-claim worksheet text (preclaim.json) is user-facing copy too
    import preclaim
    for text in preclaim.texts(preclaim.load()):
        hit = [g for g in grams(words(text)) if g in G]
        if hit: bad += 1; print(f"preclaim.json copies {len(hit)} 8-grams: \"{' '.join(hit[0])}\"")
    # the Part II template help text (part2/templates/*.json) is ours too; its narratives are the manual's wording
    for text in part2_texts():
        hit = [g for g in grams(words(text)) if g in G]
        if hit: bad += 1; print(f"part2 templates copy {len(hit)} 8-grams: \"{' '.join(hit[0])}\"")
    print('copy check:', 'CLEAN' if not bad else f'{bad} lines overlap sources')

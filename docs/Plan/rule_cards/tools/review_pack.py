#!/usr/bin/env python3
"""Build the expert review pack: one printable HTML (and optionally PDF) of every shipped Claim Guide card.

Cover page (how to respond), one block per card grouped Guide area > case, then an appendix with the figures table
and the unverified cards. Retired cards are not in rulebook.json, so they never appear. All text is HTML-escaped.
Output goes to review/guide_review_<date>.html, which is git-ignored: the pack is a hand-out, not a source.

Usage: review_pack.py [--pdf]    (--pdf converts with headless Chrome and fails loudly if Chrome is missing)
"""
import datetime
import html
import json
import os
import subprocess
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

CHROME_CANDIDATES = ['/Applications/Google Chrome.app/Contents/MacOS/Google Chrome', '/usr/bin/google-chrome', '/usr/bin/chromium']
STYLE = ('body{font:11pt/1.4 Helvetica,Arial,sans-serif;margin:24px;color:#111}h1{font-size:20pt}h2{font-size:15pt;margin-top:28px;'
         'border-bottom:2px solid #333}h3{font-size:12pt;margin-top:18px;color:#444}.card{border:1px solid #999;padding:8px 12px;'
         'margin:10px 0;page-break-inside:avoid}.id{font-family:monospace;font-weight:bold}.chip{border:1px solid #666;border-radius:3px;'
         'padding:0 4px;font-size:9pt}.meta{font-size:9pt;color:#444}.open{background:#fff3cd;padding:2px 6px}.blank{border-bottom:1px solid #000;'
         'height:22px;margin-top:4px}table{border-collapse:collapse;width:100%;font-size:9pt}td,th{border:1px solid #999;padding:3px;'
         'text-align:left;vertical-align:top}')


def esc(value):
    return html.escape(str(value), quote=True)


def _items(label, values):
    prefix = f'<i>{label}</i>' if label else ''
    return ''.join(f'<li>{prefix}{esc(v)}</li>' for v in values)


def _card(c, fig_evidence):
    chips = ' '.join(f'<span class="chip">{esc(x)}</span>' for x in c['chips'])
    ev = ''.join(f'<p class="meta">Figure evidence: {esc(e)}</p>' for e in fig_evidence)
    opens = ''.join(f'<p class="open"><b>Open point:</b> {esc(o)}</p>' for o in c['open'])
    return (f'<article class="card" id="{esc(c["id"])}"><p><span class="id">{esc(c["id"])}</span> <b>{esc(c["title"])}</b> {chips}</p>'
            f'<p><b>{esc(c["answer"])}</b></p><ul>{_items("", c["key"])}{_items("Attach: ", c["attach"])}{_items("Watch out: ", c["watch"])}</ul>'
            f'<p class="meta">Cite: {esc(c["cite"] or "none (guidance)")}</p>'
            + (f'<p class="meta">Details: {esc(c["details"])}</p>' if c['details'] else '')
            + f'<p class="meta">Source entries: {esc(", ".join(c["from"]))}</p>{ev}{opens}'
            '<p class="meta">Verdict (OK / Wrong / Outdated / Missing), correction, authority:</p><div class="blank"></div><div class="blank"></div></article>')


def _cover(date, count):
    return (f'<h1>PayslipMax Claim Guide: expert review pack</h1><p>Prepared {esc(date)}. {count} cards. Please read each card as a '
            'serving PCDA(O) claimant would, and tell us where it is wrong, out of date or missing something.</p>'
            '<p><b>How to respond.</b> One line per card you want changed, in this format (cards you find correct can be marked OK or left out):</p>'
            '<p><code>Card ID | OK / Wrong / Outdated / Missing | Correction | Authority</code></p>'
            '<p>Authority is the letter, rule or order that supports your correction (number and date). Cards marked "Open point" '
            'have a question we could not settle from the documents we hold; your answer there is the most useful.</p>')


def _appendix(rulebook, figures_data):
    rows = ''.join(f'<tr><td>{esc(k)}</td><td>{esc(f["card"])}</td><td>{esc(f.get("unit", ""))}</td><td>{esc(f.get("effective_from", "see rate table"))}</td>'
                   f'<td>{esc(f["authority"])}</td><td>{esc(f["evidence"])}</td></tr>' for k, f in sorted(figures_data['figures'].items()))
    unverified = ''.join(f'<li><span class="id">{esc(c["id"])}</span> {esc(c["title"])}</li>' for c in rulebook['cards'] if c['open'])
    return ('<section id="appendix"><h2>Appendix</h2><h3>Figures shown in the app</h3><table><tr><th>Figure</th><th>Card</th><th>Unit</th>'
            f'<th>Effective</th><th>Authority</th><th>Evidence</th></tr>{rows}</table><h3>Unverified cards (carry an open point)</h3>'
            f'<ul>{unverified}</ul></section>')


def build_html(rulebook, figures_data, date, retired=()):
    retired = set(retired)
    cards = {c['id']: c for c in rulebook['cards'] if c['id'] not in retired}
    evidence = {}
    for fig in figures_data['figures'].values():
        evidence.setdefault(fig['card'], []).append(fig['evidence'])
    body, printed = [], set()
    for area in rulebook['nav']:
        body.append(f'<h2>{esc(area["title"])}</h2>')
        for case in area['cases']:
            body.append(f'<h3>{esc(case["title"])}</h3>')
            for cid in case['cards']:
                if cid in cards and cid not in printed:
                    printed.add(cid)
                    body.append(_card(cards[cid], evidence.get(cid, [])))
    live = {'cards': [c for c in rulebook['cards'] if c['id'] not in retired]}
    return (f'<!doctype html><html><head><meta charset="utf-8"><title>Claim Guide review {esc(date)}</title><style>{STYLE}</style></head><body>'
            f'{_cover(date, len(printed))}<main>{"".join(body)}</main>{_appendix(live, figures_data)}</body></html>\n')


def out_path(date):
    return os.path.join(CARDS_DIR, 'review', f'guide_review_{date}.html')


def to_pdf(html_path, pdf_path, chrome_candidates=None):
    chrome = next((p for p in (chrome_candidates or CHROME_CANDIDATES) if os.path.exists(p)), None)
    if not chrome:
        sys.exit('Chrome not found: install Google Chrome to use --pdf (the HTML pack was still written).')
    code = subprocess.call([chrome, '--headless', '--disable-gpu', f'--print-to-pdf={pdf_path}', '--no-pdf-header-footer',
                            'file://' + os.path.abspath(html_path)], stderr=subprocess.DEVNULL)
    if code or not os.path.exists(pdf_path):
        sys.exit(f'Chrome failed to write {pdf_path} (exit {code})')


def main(argv):
    date = datetime.date.today().isoformat()
    with open(os.path.join(CARDS_DIR, 'rulebook.json'), encoding='utf-8') as fh:
        rulebook = json.load(fh)
    with open(os.path.join(CARDS_DIR, 'figures.json'), encoding='utf-8') as fh:
        figures_data = json.load(fh)
    path = out_path(date)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as fh:
        fh.write(build_html(rulebook, figures_data, date))
    print('wrote', os.path.relpath(path))
    if '--pdf' in argv:
        pdf = path[:-5] + '.pdf'
        to_pdf(path, pdf)
        print('wrote', os.path.relpath(pdf))


if __name__ == '__main__':
    main(sys.argv[1:])

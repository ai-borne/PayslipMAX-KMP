"""Loaders for the extracted FAQ and TA-handbook units (used by packet.py and copycheck.py)."""
import html
import json
import re

from config import src


def txt(s):
    return re.sub(r'\s+', ' ', html.unescape(re.sub(r'<[^>]+>', ' ', s or ''))).strip()


faq = {('FAQ-' + x['id'][:6]): x for x in json.load(open(src('faq.json')))}
ta = {}
for _u in json.load(open(src('TA_units.json'))):
    _k = f"TA-{_u['para']}@p{_u['page']}"
    _n = 2
    while _k in ta:
        _k = f"TA-{_u['para']}@p{_u['page']}.{_n}"
        _n += 1
    ta[_k] = _u

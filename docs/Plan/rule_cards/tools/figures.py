"""Validate and ship figures.json, the rupee figures behind the Claim Guide's "your figure" line (phase E6).

A figure is a rule-card rate the app turns into a personal amount from the officer's payslip. It ships only with a value,
an effective date, a primary letter, an evidence level and the owner's approval date. No rupee figure is written in
Kotlin: the app reads these from the bundle. compile.py calls validate(); bundle.py calls for_bundle().
"""
import datetime
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

FIGURES_PATH = os.path.join(CARDS_DIR, 'figures.json')
SUPPORTED_VERSION = 1
EVIDENCE = {'LETTER_TEXT', 'HANDBOOK_ONLY', 'WEB_SECONDARY'}
MODES = {'da_step', 'percent_of_basic', 'plus_da', 'rate_table'}
CITY_CLASSES = {'HIGHER', 'OTHER', 'ANY'}
# Fields the app needs. Authority, evidence, notes and approval stay in the repo.
SHIPPED_FIGURE_KEYS = ('card', 'mode', 'assumption', 'effective_from', 'percent', 'bands', 'classes')
SHIPPED_BAND_KEYS = ('levels', 'city_class', 'base')
SHIPPED_STEP_KEYS = ('from_da_percent', 'percent', 'effective_from')


def load():
    with open(FIGURES_PATH, encoding='utf-8') as fh:
        return json.load(fh)


def _is_date(value):
    try:
        datetime.date.fromisoformat(value)
        return isinstance(value, str) and len(value) == 10
    except (TypeError, ValueError):
        return False


def _positive_int(value):
    return isinstance(value, int) and not isinstance(value, bool) and value > 0


def validate(data, cards):
    """Every problem found, as readable lines. Empty means the file is fit to bundle (approval is checked apart)."""
    errs = []
    if data.get('version') != SUPPORTED_VERSION:
        errs.append(f'figures: version must be {SUPPORTED_VERSION}')
    step = data.get('da_step') or {}
    if not (_positive_int(step.get('per_da_percent')) and _positive_int(step.get('increase_percent'))):
        errs.append('figures: da_step needs positive per_da_percent and increase_percent')
    if not step.get('authority'):
        errs.append('figures: da_step has no authority')
    by_id = {c['id']: c for c in cards}
    figs = data.get('figures') or {}
    for key, fig in figs.items():
        errs += _figure_problems(key, fig, by_id)
    claimed = {fig.get('card') for fig in figs.values()}
    for card in cards:
        if card.get('personal') and card['id'] not in claimed:
            errs.append(f'card {card["id"]} has a personal spec but no figure')
    return errs


def _figure_problems(key, fig, by_id):
    errs = []
    card = by_id.get(fig.get('card'))
    if card is None:
        errs.append(f'{key}: unknown card {fig.get("card")}')
    elif not card.get('personal'):
        errs.append(f'{key}: card {card["id"]} has no personal spec')
    if not fig.get('authority'):
        errs.append(f'{key}: no authority (the primary letter)')
    if fig.get('evidence') not in EVIDENCE:
        errs.append(f'{key}: evidence must be one of {sorted(EVIDENCE)}')
    if not _is_date(fig.get('effective_from')) and fig.get('mode') != 'rate_table':
        errs.append(f'{key}: effective_from must be a YYYY-MM-DD date')
    mode = fig.get('mode')
    if mode not in MODES:
        errs.append(f'{key}: unknown mode {mode!r}')
    elif mode in ('da_step', 'plus_da'):
        errs += _band_problems(key, fig, mode)
    elif mode == 'percent_of_basic':
        pct = fig.get('percent')
        if not (_positive_int(pct) and pct <= 100):
            errs.append(f'{key}: percent must be a whole number from 1 to 100')
    else:
        errs += _rate_table_problems(key, fig)
    return errs


def _band_problems(key, fig, mode):
    errs, seen = [], {}
    bands = fig.get('bands') or []
    if not bands:
        errs.append(f'{key}: no bands')
    for band in bands:
        city = band.get('city_class', 'ANY')
        if city not in CITY_CLASSES or (mode == 'da_step' and city != 'ANY'):
            errs.append(f'{key}: bad city_class {city!r} for mode {mode}')
        if not _positive_int(band.get('base')):
            errs.append(f'{key}: band base must be a positive whole number')
        if not band.get('levels'):
            errs.append(f'{key}: a band has no levels')
        for level in band.get('levels', []):
            for other in (city, 'ANY') if city != 'ANY' else CITY_CLASSES:
                if (level, other) in seen:
                    errs.append(f'{key}: level {level} claimed twice ({other})')
            seen[(level, city)] = True
    return errs


def _rate_table_problems(key, fig):
    errs, ladders = [], []
    classes = fig.get('classes') or {}
    if not classes:
        errs.append(f'{key}: no classes')
    for name, steps in classes.items():
        das = [s.get('from_da_percent') for s in steps]
        pcts = [s.get('percent') for s in steps]
        if not steps or das[0] != 0 or das != sorted(set(das)):
            errs.append(f'{key}: class {name} steps must start at DA 0 and be strictly ascending')
        if pcts != sorted(pcts) or not all(_positive_int(p) for p in pcts):
            errs.append(f'{key}: class {name} percents must be positive and not fall as DA rises')
        for s in steps:
            if not _is_date(s.get('effective_from')):
                errs.append(f'{key}: class {name} step at DA {s.get("from_da_percent")} needs an effective_from date')
        ladders.append(das)
    if any(ladder != ladders[0] for ladder in ladders):
        errs.append(f'{key}: every class must use the same DA steps')
    return errs


def unapproved(data):
    return [key for key, fig in (data.get('figures') or {}).items() if not _is_date(fig.get('approved'))]


def for_bundle(data):
    """The slice the app ships. Stops (SystemExit) if any figure lacks the owner's approval."""
    missing = unapproved(data)
    if missing:
        sys.exit(f'figures not approved by the owner, so not bundled: {", ".join(missing)}')
    step = data['da_step']
    out = {}
    for key, fig in data['figures'].items():
        shipped = {k: fig[k] for k in SHIPPED_FIGURE_KEYS if k in fig}
        if 'bands' in shipped:
            shipped['bands'] = [{k: b[k] for k in SHIPPED_BAND_KEYS if k in b} for b in fig['bands']]
        if 'classes' in shipped:
            shipped['classes'] = {n: [{k: s[k] for k in SHIPPED_STEP_KEYS} for s in steps] for n, steps in fig['classes'].items()}
        out[key] = shipped
    return {'da_step': {'per_da_percent': step['per_da_percent'], 'increase_percent': step['increase_percent']}, 'figures': out}

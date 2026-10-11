"""Validate and ship figures.json, the rupee figures behind the Claim Guide's "your figure" line (phase E6).

A figure is a rule-card rate the app turns into a personal amount from the officer's payslip. It ships only with a value,
an effective date, a primary letter, an evidence level and the owner's approval date. OWNER_CONFIRMED (with a `confirmed` date)
records a fact the owner stated without a document; it is the weakest label and is named as such. No rupee figure is written in
Kotlin: the app reads these from the bundle. compile.py calls validate(); bundle.py calls for_bundle().

A figure either belongs to a rule card (`card`, shown as that card's "your figure" line) or feeds the TA/DA pre-claim worksheet
(`use: "preclaim"`, no card). Never both, so a worksheet rate can never be drawn on the wrong card.
"""
import datetime
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from config import CARDS_DIR

FIGURES_PATH = os.path.join(CARDS_DIR, 'figures.json')
SUPPORTED_VERSION = 1
EVIDENCE = {'LETTER_TEXT', 'HANDBOOK_ONLY', 'WEB_SECONDARY', 'OWNER_CONFIRMED'}
MODES = {'da_step', 'percent_of_basic', 'plus_da', 'rate_table', 'rate_by_state', 'time_share'}
USE_PRECLAIM = 'preclaim'
# The states and territories of PCDA(O) Pune Wing Circular no. 10 (2024-09-03), spelt as the circular spells them. Lakshadweep and
# Dadra and Nagar Haveli and Daman and Diu are absent because the Army has no presence there (owner, 2026-10-11).
RMA_STATES = (
    'Andaman & Nicobar', 'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chandigarh (UT)', 'Chattisgarh', 'Delhi', 'Goa',
    'Gujrat', 'Haryana', 'Himachal Pradesh', 'J&K', 'Jharkhand', 'Karnataka', 'Kerala', 'Leh & Ladakh', 'Madhya Pradesh', 'Maharashtra',
    'Manipur', 'Meghalaya', 'Mizoram', 'Nagaland', 'Odisha', 'Puducherry', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamilnadu', 'Telangana',
    'Tripura', 'Uttar Pradesh', 'Uttarakhand', 'West Bengal',
)
MAX_RATE_PER_KM = 100
MAX_LIMIT_CHARS = 120
MAX_LOCAL_MOVE_DIVISOR = 10
CTG_CASE_KEYS = ('island_percent', 'local_move_divisor')
SHARE_KEYS = {'from_minutes', 'percent'}
CITY_CLASSES = {'HIGHER', 'OTHER', 'ANY'}
# Fields the app needs. Authority, evidence, notes and approval stay in the repo.
SHIPPED_FIGURE_KEYS = ('card', 'use', 'mode', 'assumption', 'effective_from', 'percent', 'island_percent', 'local_move_divisor', 'bands',
                       'classes', 'rates', 'time_shares')
SHIPPED_BAND_KEYS = ('levels', 'city_class', 'base', 'limit')
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
    errs += _first_step_problems(step.get('first_step') or {})
    by_id = {c['id']: c for c in cards}
    figs = data.get('figures') or {}
    for key, fig in figs.items():
        errs += _figure_problems(key, fig, by_id)
    claimed = {fig.get('card') for fig in figs.values()}
    for card in cards:
        if card.get('personal') and card['id'] not in claimed:
            errs.append(f'card {card["id"]} has a personal spec but no figure')
    return errs


def _first_step_problems(first):
    """The date the first DA step (DA 50%) reaches the lump sums. It is a decision, so it names its evidence and who confirmed it."""
    errs = []
    if not _is_date(first.get('effective_from')):
        errs.append('figures: da_step first_step needs an effective_from date')
    if first.get('evidence') not in EVIDENCE:
        errs.append(f'figures: da_step first_step evidence must be one of {sorted(EVIDENCE)}')
    if first.get('evidence') == 'OWNER_CONFIRMED' and not _is_date(first.get('confirmed')):
        errs.append('figures: da_step first_step is OWNER_CONFIRMED but has no confirmed date')
    return errs


def _card_problems(key, fig, by_id):
    if 'card' not in fig:
        return [] if fig.get('use') == USE_PRECLAIM else [f'{key}: a figure with no card must have use "{USE_PRECLAIM}"']
    if fig.get('use') == USE_PRECLAIM:
        return [f'{key}: a {USE_PRECLAIM} figure may not name a card']
    card = by_id.get(fig['card'])
    if card is None:
        return [f'{key}: unknown card {fig["card"]}']
    return [] if card.get('personal') else [f'{key}: card {card["id"]} has no personal spec']


def _figure_problems(key, fig, by_id):
    errs = _card_problems(key, fig, by_id)
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
        errs += _ctg_case_problems(key, fig)
    elif mode == 'rate_by_state':
        errs += _rate_by_state_problems(key, fig)
    elif mode == 'time_share':
        errs += _time_share_problems(key, fig)
    else:
        errs += _rate_table_problems(key, fig)
    if mode != 'percent_of_basic':
        errs += [f'{key}: {field} belongs on a percent_of_basic figure' for field in CTG_CASE_KEYS if field in fig]
    if mode != 'time_share' and 'time_shares' in fig:
        errs.append(f'{key}: time_shares belongs on a time_share figure')
    return errs


def _ctg_case_problems(key, fig):
    """The other two CTG cases sit beside the normal percent so the app holds no rate: island percent, and local move = 1/divisor."""
    errs = []
    if 'island_percent' in fig:
        island = fig['island_percent']
        if not (_positive_int(island) and fig.get('percent', 0) <= island <= 100):
            errs.append(f'{key}: island_percent must be a whole number from percent to 100')
    if 'local_move_divisor' in fig:
        divisor = fig['local_move_divisor']
        if not (_positive_int(divisor) and 2 <= divisor <= MAX_LOCAL_MOVE_DIVISOR):
            errs.append(f'{key}: local_move_divisor must be a whole number from 2 to {MAX_LOCAL_MOVE_DIVISOR}')
    return errs


def _time_share_problems(key, fig):
    """A part of the full day's rate by time away, in whole minutes so a boundary is exact: a share applies from its `from_minutes` to the next."""
    shares = fig.get('time_shares')
    if not isinstance(shares, list) or not shares:
        return [f'{key}: no time_shares']
    errs = []
    for share in shares:
        start, pct = share.get('from_minutes'), share.get('percent')
        errs += [f'{key}: a share has the unknown field {extra!r}' for extra in sorted(set(share) - SHARE_KEYS)]
        if not (isinstance(start, int) and not isinstance(start, bool) and start >= 0):
            errs.append(f'{key}: from_minutes must be a whole number of minutes, 0 or more')
        if not (_positive_int(pct) and pct <= 100):
            errs.append(f'{key}: percent must be a whole number from 1 to 100')
    starts, pcts = [s.get('from_minutes') for s in shares], [s.get('percent') for s in shares]
    if errs:
        return errs
    if starts[0] != 0:
        errs.append(f'{key}: the first share must start at from_minutes 0, or some durations have no share')
    if starts != sorted(set(starts)):
        errs.append(f'{key}: from_minutes must strictly ascend')
    if pcts != sorted(pcts):
        errs.append(f'{key}: percent must not fall as the time away grows')
    if pcts[-1] != 100:
        errs.append(f'{key}: the last share must be the full day (percent 100)')
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
        errs += _amount_problems(key, band, mode)
        if not band.get('levels'):
            errs.append(f'{key}: a band has no levels')
        for level in band.get('levels', []):
            for other in (city, 'ANY') if city != 'ANY' else CITY_CLASSES:
                if (level, other) in seen:
                    errs.append(f'{key}: level {level} claimed twice ({other})')
            seen[(level, city)] = True
    return errs


def _amount_problems(key, band, mode):
    """A band is a rupee base, or (da_step only) a plain-text limit with no amount, for levels the rules cap without a rupee figure."""
    if 'limit' not in band:
        return [] if _positive_int(band.get('base')) else [f'{key}: band needs a base (a positive whole number) or a limit text']
    limit = band['limit']
    if mode != 'da_step' or 'base' in band:
        return [f'{key}: a band takes a limit only in place of a base, and only in da_step mode']
    if not (isinstance(limit, str) and 0 < len(limit) <= MAX_LIMIT_CHARS and not set('<>') & set(limit)):
        return [f'{key}: band limit must be plain text of 1 to {MAX_LIMIT_CHARS} characters']
    return []


def _rate_by_state_problems(key, fig):
    rates = fig.get('rates')
    if not isinstance(rates, dict) or not rates:
        return [f'{key}: no rates']
    errs = [f'{key}: no rate for {state}' for state in RMA_STATES if state not in rates]
    errs += [f'{key}: unknown state {state!r} (spell it as the circular does)' for state in rates if state not in RMA_STATES]
    for state, rate in rates.items():
        if not (_positive_int(rate) and rate <= MAX_RATE_PER_KM):
            errs.append(f'{key}: rate for {state} must be a whole number from 1 to {MAX_RATE_PER_KM}')
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
            at = f'{key}: class {name} step at DA {s.get("from_da_percent")}'
            if not _is_date(s.get('effective_from')):
                errs.append(f'{at} needs an effective_from date')
            if 'evidence' in s and s['evidence'] not in EVIDENCE:
                errs.append(f'{at} has unknown evidence {s["evidence"]!r}')
            if s.get('evidence') == 'OWNER_CONFIRMED' and not _is_date(s.get('confirmed')):
                errs.append(f'{at} is OWNER_CONFIRMED but has no confirmed date')
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
    shipped_step = {'per_da_percent': step['per_da_percent'], 'increase_percent': step['increase_percent'],
                    'first_step_effective_from': step['first_step']['effective_from']}
    return {'da_step': shipped_step, 'figures': out}

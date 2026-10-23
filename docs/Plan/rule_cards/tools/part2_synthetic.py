"""Synthetic Part II rules and templates for tests (Part II plan, phase 2a): invented codes and invented wording, so no manual text
sits in a fixture and an edit to the real content can never break a test. The Python tests mutate copies of these; `main` writes the
section the Kotlin engine tests load (shared/src/androidUnitTest/resources/part2/synthetic_section.json), built by the real
`part2_templates.for_bundle`, so a test of the app exercises the same JSON the shipping tools write. test_part2_templates.py fails if
that file is stale. Run `python3 part2_synthetic.py` after editing this file.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import part2_templates
from config import REPO_ROOT

SECTION_PATH = os.path.join(REPO_ROOT, 'shared', 'src', 'androidUnitTest', 'resources', 'part2', 'synthetic_section.json')

GROUPS = {'A': 'Money', 'AM': 'Money, wanted by MISO', 'B': 'Claims', 'C': 'No money', 'CM': 'No money, wanted by MISO'}
LIMITS = {'narrative': 300, 'label': 60, 'help': 200, 'slots': 8, 'links': 6, 'rules': 4, 'note': 20}

# (serial, code, group, page); a group of None is a serial the manual prints none for.
_ROWS = [('1.1', 'ALPHAGRANT', 'A', 28), ('1.2', 'ALPHAEXT', 'A', 28), ('1.3', 'BETARATION', 'A', 29), ('1.4', 'BETASTOP', 'AM', 29),
         ('2.1', 'GAMMACLAIM', 'B', 39), ('2.2', 'DELTANOTE', 'C', 40), ('3.1', 'REJOINAL', 'CM', 46), ('3.28', 'STOPNOGROUP', None, 57),
         ('4.1', 'ZETAPLAIN', 'C', 50)]  # ZETAPLAIN has no template: a link to it is listed, not drafted


def codes():
    rows = [{'serial': s, 'chapter': int(s.split('.')[0]), 'codes': [c], 'group': g, 'name': f'Invented thing {c}', 'page': p}
            for s, c, g, p in _ROWS]
    return {'version': 1, 'groups': dict(GROUPS), 'serials': rows}


def rules():
    return {'version': 1, 'limits': dict(LIMITS), 'rules': [
        {'id': 'cap_days', 'kind': 'check', 'severity': 'warn', 'basis': 'manual', 'page': 115, 'note': '2'},
        {'id': 'grant_quoted', 'kind': 'check', 'severity': 'ack', 'basis': 'manual', 'page': 131},
        {'id': 'on_ration', 'kind': 'condition', 'basis': 'manual', 'page': 97, 'note': '1.1'},
        {'id': 'in_hill_station', 'kind': 'condition', 'basis': 'practice'}]}


def _slot(sid, kind, label, help_text, **extra):
    return {'id': sid, 'kind': kind, 'label': label, 'help': help_text, **extra}


def _col(slot=None):
    return {'need': 'none'} if slot is None else {'need': 'required', 'slot': slot}


def templates():
    leave_from = _slot('leave_from', 'date', 'First day', 'The first day of the invented thing.')
    leave_to = _slot('leave_to', 'date', 'Last day', 'The last day of the invented thing.')
    grant = _slot('grant', 'ref', 'Grant order', 'The order that granted it, once published.')
    return [
        {'code': 'ALPHAGRANT', 'page': 28, 'narrative': 'Granted the first thing for {days} days in {year} from {leave_from} to {leave_to}',
         'slots': [leave_from, leave_to, _slot('days', 'days', 'Days', 'Counted for you, both days included.', between=['leave_from', 'leave_to']),
                   _slot('year', 'number', 'Year', 'The year it belongs to.')],
         'from': _col('leave_from'), 'to': _col('leave_to'), 'data': [_col('days'), _col('year'), _col(), _col()],
         'rules': ['cap_days'], 'links': [
             {'code': 'BETARATION', 'basis': 'manual', 'page': 97, 'note': '1.1', 'when': 'on_ration'},
             {'code': 'BETASTOP', 'basis': 'practice', 'when': 'in_hill_station'},
             {'code': 'ZETAPLAIN', 'basis': 'practice', 'when': 'in_hill_station'}]},
        {'code': 'ALPHAEXT', 'page': 29, 'narrative': 'Extended the first thing vide {grant.no} dated {grant.date} up to {new_to}',
         'slots': [grant, _slot('ext_from', 'date', 'From', 'The day the extension starts.'), _slot('new_to', 'date', 'Up to', 'The new last day.'),
                   _slot('sanction', 'ref', 'Sanction', 'The signal that allowed it.')],
         'from': _col('ext_from'), 'to': _col('new_to'), 'data': [_col('grant.no'), _col('grant.date'), _col(), _col()],
         'authority': 'Sanction {sanction.no} dated {sanction.date}', 'rules': ['grant_quoted'], 'links': []},
        {'code': 'BETARATION', 'page': 29, 'narrative': 'Granted the second thing w.e.f. {away_from}',
         'slots': [_slot('away_from', 'date', 'First day away', 'Counts any days before the leave starts.')],
         'from': _col('away_from'), 'to': _col(), 'data': [_col() for _ in range(4)],
         'certificate': 'Certified that the second thing is in order', 'rules': [], 'links': []},
        {'code': 'BETASTOP', 'page': 30, 'narrative': 'Stopped the second thing w.e.f. {rejoin}',
         'slots': [_slot('rejoin', 'date', 'Back on', 'The day you are back.')],
         'from': _col('rejoin'), 'to': _col(), 'data': [_col() for _ in range(4)], 'rules': [], 'links': []},
        {'code': 'GAMMACLAIM', 'page': 39, 'narrative': 'Claimed the third thing for {claim_on}',
         'slots': [_slot('claim_on', 'date', 'Claim date', 'The day the claim is for.')],
         'from': _col('claim_on'), 'to': _col(), 'data': [_col() for _ in range(4)], 'rules': [], 'links': []},
        {'code': 'DELTANOTE', 'page': 40, 'narrative': 'Noted the fourth thing on {noted_on} as {remark}',
         'slots': [_slot('noted_on', 'date', 'Noted on', 'The day it was noted.'), _slot('remark', 'text', 'Remark', 'A few plain words.')],
         'from': _col('noted_on'), 'to': _col(), 'data': [_col('remark'), _col(), _col(), _col()], 'rules': [], 'links': []},
        {'code': 'REJOINAL', 'print': 'REJOIN', 'page': 46, 'narrative': 'Rejoined w.e.f. {rejoin_on} vide {grant.no} dated {grant.date}',
         'slots': [_slot('rejoin_on', 'date', 'Back on', 'The day you are back.'), grant],
         'from': _col('rejoin_on'), 'to': _col(), 'data': [_col('grant.no'), _col('grant.date'), _col(), _col()],
         'rules': ['grant_quoted'], 'links': []},
        {'code': 'STOPNOGROUP', 'page': 57, 'narrative': 'Stopped the fifth thing w.e.f. {stop_on}',
         'slots': [_slot('stop_on', 'date', 'Stopped on', 'The day it stopped.')],
         'from': _col('stop_on'), 'to': _col(), 'data': [_col() for _ in range(4)], 'rules': [], 'links': []},
    ]


def section():
    """The whole synthetic `part2` section: a small lookup shaped like the real one, plus the rules and templates the real tools ship."""
    lookup = {'version': 1, 'limits': {'text': 300, 'title_words': 7, 'ribbons_min': 1, 'ribbons_max': 3, 'cases_per_ribbon': 3,
                                      'case_serials_min': 2, 'case_serials_max': 8},
              'groups': dict(GROUPS), 'serials': [{k: v for k, v in row.items() if v is not None} for row in codes()['serials']],
              'ribbons': [{'id': 'leave', 'title': 'The first ribbon', 'built': True, 'cases': [
                  {'id': 'leave_one', 'title': 'First case', 'serials': ['1.1', '1.2', '1.3', '1.4'], 'also': [], 'lookup': []},
                  {'id': 'leave_two', 'title': 'Second case', 'serials': ['2.1', '2.2', '3.1', '3.28', '4.1'], 'also': [], 'lookup': ['3.28']}]}]}
    return {**lookup, **part2_templates.for_bundle(templates(), rules(), codes())}


def render():
    return json.dumps(section(), ensure_ascii=False, separators=(',', ':')) + '\n'


if __name__ == '__main__':
    os.makedirs(os.path.dirname(SECTION_PATH), exist_ok=True)
    with open(SECTION_PATH, 'w', encoding='utf-8') as fh:
        fh.write(render())
    print('wrote', os.path.relpath(SECTION_PATH, REPO_ROOT))

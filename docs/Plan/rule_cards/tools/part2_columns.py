"""Reading rules for the Annexure III cells of ODP-2026 (Part II Orders Bank, phase 1): from the manual's printed cell text to
{need, caption} per column. Pure functions, no manual text and no PDF here, so CI can run them; the PDF reader is part2_columns_extract.py
(local only) and the committed result is part2/columns.json.

need     required | optional | none.  `none` is a blank cell or any spelling of "Not required"; it carries no caption.
         `optional` is a cell marked "(Optional)" or one that asks for the prefix or suffix days (the manual's notes say "if any").
         Everything else the manual prints in a column is `required`.
caption  the manual's own words minus the "Required as" boilerplate (plan 13, owner decision 12). Only the breaks a narrow column put
         inside a word or a date are mended (normalise), never a rewording.
"""
import html
import json
import re

VERSION = 1
SOURCE = 'Documentation Procedure (Officers) 2026, Annexure III'
NEEDS = ('required', 'optional', 'none')
FIGURE_REASON = 'A cell prints an example figure where a caption would be; its caption is left blank.'

DATE = re.compile(r'([Dd][Dd])\s*/\s*([Mm][Mm])\s*/\s*([Yy]\s?[Yy]\s?[Yy]\s?[Yy])')
HYPHEN_WRAP = re.compile(r'(\w{3,})- ([a-z]{2,})')
# A word the column cut in two with no hyphen. A closed list, each read by hand at its row on 2026-10-30; the extractor prints every
# caption word it cannot find in the manual's own vocabulary so a new cut is seen, never guessed.
WORD_CUTS = (
    ('Suspensio n', 'Suspension'), ('Internatio nal', 'International'), ('Commissio n', 'Commission'), ('Nominati on', 'Nomination'),
    ('enclose d', 'enclosed'), ('relation ship', 'relationship'), ('belo nging', 'belonging'), ('Compete nt', 'Competent'),
    ('DGAFM S', 'DGAFMS'), ('DGA FMS', 'DGAFMS'), ('Ar my', 'Army'), ('SOSCR S/SOSC RS1', 'SOSCRS/SOSCRS1'),
    ('accumulatio n', 'accumulation'), ('suspensio n', 'suspension'), ('Diagnos is', 'Diagnosis'), ('Diagnosi s', 'Diagnosis'),
    ('Qualificatio n', 'Qualification'), ('Promotio n', 'Promotion'), ('Nationalit y', 'Nationality'), ('nationalit y', 'nationality'),
    ('Daughte r', 'Daughter'), ('permanen t/ temporar y', 'permanent/ temporary'), ('Employme nt', 'Employment'),
    ('Employmen t', 'Employment'), ('Re- Employ', 'Re-Employ'), ('hospitaliza tion', 'hospitalization'),
    ('hospitalizati on', 'hospitalization'), ('Embodi ment', 'Embodiment'), ('Implemen tation', 'Implementation'),
    ('Operat ional', 'Operational'), ('numbe r', 'number'), ('Occurrenc e', 'Occurrence'), ('re- marriage', 're-marriage'),
    ('Nomina tion', 'Nomination'), ('(1 Waiver st / 2 nd Waiver / 3 rd Waiver)', '(1st Waiver / 2nd Waiver / 3rd Waiver)'),
    ('1 /2 st nd spell', '1st/ 2nd spell'),
)
NOT_REQUIRED = re.compile(r'^Not\s+(req|red)', re.I)
BOILERPLATE = re.compile(r'\b(?:Required|Reqd\.?|Requires)\s+as(?![A-Za-z])\s*', re.I)
OPTIONAL_MARK = re.compile(r'\s*\(optional\)', re.I)
PREFIX_SUFFIX = re.compile(r'^(?:pre-?fix|suf+ix)\b', re.I)
FIGURE = re.compile(r'^[\d,./\s-]*\d[\d,./\s-]*$')


def normalise(text):
    t = re.sub(r'\s+', ' ', html.unescape(text or '')).strip().replace('<', '').replace('>', '')
    t = DATE.sub(lambda m: f'{m.group(1)}/{m.group(2)}/{re.sub(chr(32), "", m.group(3))}', t)
    t = re.sub(r'\b([Rr]equire) ([ds])\b', r'\1\2', t)
    t = HYPHEN_WRAP.sub(r'\1\2', t)
    for cut, whole in WORD_CUTS:
        t = t.replace(cut, whole)
    return t


def _strip(text):
    caption = BOILERPLATE.sub('', text, count=1)
    while BOILERPLATE.match(caption):
        caption = BOILERPLATE.sub('', caption, count=1)
    return caption.strip()


def prints_a_figure(text):
    """True when the cell shows an example value (220, 26/01/2014) where a caption should be."""
    t = normalise(text)
    return not NOT_REQUIRED.match(t) and bool(FIGURE.match(_strip(t)))


def classify(text):
    t = normalise(text)
    if not t or NOT_REQUIRED.match(t):
        return 'none', ''
    caption = _strip(t)
    optional = bool(OPTIONAL_MARK.search(caption)) or bool(PREFIX_SUFFIX.match(caption))
    caption = OPTIONAL_MARK.sub('', caption).strip()
    if FIGURE.match(caption):
        caption = ''
    return ('optional' if optional else 'required'), caption


def build_row(raw, codes_row, hand=None):
    """One columns.json row from the extractor's raw cells (From, To, Data 1-4) and the codes.json row; `hand` is a
    {'reason', 'cols'} reading made by eye from the page, which replaces the rule-based one."""
    pairs = [tuple(c) for c in hand['cols']] if hand else [classify(c) for c in raw['cells']]
    cols = [{'need': need, 'caption': caption} for need, caption in pairs]
    row = {'serial': codes_row['serial'], 'codes': codes_row['codes'], 'page': codes_row['page'],
           'from': cols[0], 'to': cols[1], 'data': cols[2:]}
    if hand:
        row['hand'] = hand['reason']
    elif any(prints_a_figure(c) for c in raw['cells']):
        row['hand'] = FIGURE_REASON
    return row


def render(data):
    """One row per line, so a review diff reads row by row."""
    head = json.dumps({k: v for k, v in data.items() if k != 'rows'}, ensure_ascii=False)[:-1]
    rows = ',\n'.join(' ' + json.dumps(r, ensure_ascii=False, separators=(',', ':')) for r in data['rows'])
    return f'{head},\n "rows": [\n{rows}\n ]}}\n'

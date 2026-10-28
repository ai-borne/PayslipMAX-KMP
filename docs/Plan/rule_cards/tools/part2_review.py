#!/usr/bin/env python3
"""The owner-review pack for the Part II content batches (Part II plan, phase 2b): part2/part2_review.md.

The owner checks every narrative, column and citation against the printed ODP-2026 page before a batch counts as done. This lays each
shipped template out next to the page it cites: the serial, group and page, the narrative and certificate as they print, each column
(From, To, Data 1-4) with what it prints, every slot with its label and help, the checks with their severity and citation, and the
linked orders with their basis. The judgment calls (each settled by the owner) come first. The pack holds only what ships, so it carries nothing
the bundle does not, and part2/part2_review.md is committed so a stale pack fails a test (test_part2_review.py).
Usage: part2_review.py [--check]    (--check exits non-zero if the committed pack is out of date)
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import part2_codes_check
import part2_rules
import part2_templates
from config import CARDS_DIR

REVIEW_PATH = os.path.join(CARDS_DIR, 'part2', 'part2_review.md')
COLUMNS = ('From', 'To', 'Data 1', 'Data 2', 'Data 3', 'Data 4')
NEED_WORDS = {'required': 'required', 'optional': 'optional', 'none': 'not required'}

# Judgment calls the template author made where the manual is silent, ambiguous or says two things. The owner settled each on 2026-10-23
# (DECISIONS Round 11 and 12); they stay here so the review checks that each reads right on the printed page.
OPEN_POINTS = [
    'Prefix and suffix (settled): From and To exclude them; Data 1 holds the first prefix day, Data 2 the last suffix day, both left empty when there is none, and the narrative prints "Nil". The manual says "Required as prefix" for Data 1; a leave with no prefix is common, so the column is optional.',
    '5.5 GTDSPCL (settled): the manual prints its "w.e.f." block twice in the narrative. The template prints it once.',
    'Order number "----/----" (settled; 5.23, 5.24, 5.25 and the note on 5.3 col 9): read as the order number and its date, printed "no. dated date" in the narrative and the Data column.',
    '5.25 RECALL From date (settled): the day the officer actually left the leave station (note 3 and col 4 of the row), not the date of the recall signal (note 1).',
    '5.24 VOLREJ and 5.25 RECALL (settled): the manual writes "Annual/Furlough Leave" in the narrative. The template asks the officer for the kind of leave and prints it, as HRMS does ("Rejoined from CL/AL").',
    'Ration Money Allowance certificate (settled; note 8, p.99) on GTDRSHNA: items 1 to 5 are printed. Item 6 (posted to a non-military station) and item 7 (prior approval of the Station Commander) are conditional and are left out; the clerk adds them where they apply. Item 1 takes the kind of leave as typed.',
    'Ration four dates: SORS and GTDRSHNA from the first day away (the earlier of the first prefix day and the first day of leave); TORS and STPRSHNA from the day after the last day away (the later of the last day of leave and the last suffix day). An extension (EXTNAL), voluntary rejoining or recall does not move them: it asks whether the earlier order also granted or stopped them and, on Yes, warns that those rows are cancelled and notified afresh with CANCELS (para 11, p.8).',
    'R&H (practice only, settled): the stop and the re-grant link from annual leave and annual leave carried forward only, never from casual leave. The re-grant is under the officer\'s own GTDRH code and prints the station, rate, Corps notification and serial from p.46-48. Data 3 prints "Corps, notification no., date" in that order (settled; the manual does not give the HRMS format).',
    'Checks (settled): acknowledged before printing (ack): annual leave over 60 days a year (note 2) and casual leave beside another leave (p.117). Shown only (warn): prefix and suffix over 5 days (note 9), a special casual leave over 30 days (row 5.5, "upto 30 days") and a grant order not yet quoted (notes 7 and 2).',
    'CANCEL and CANCELS (settled, DECISIONS Round 22): para 12.3 (p.8) calls the one-occurrence code "CANCELCS", but the annexure (p.227), the code list and HRMS call it CANCELS, so the slip prints CANCELS. The order being cancelled is quoted in the narrative (note 2) and in cols 8 and 9 (note 3); CANCEL puts "ALL" in col 6, CANCELS the occurrence code in col 6 and its serial in col 7 (notes 3 and 5). From and To are not required. The certificate prints as the manual words it, with the order and serial filled in and no full stop.',
    'Time bar (settled, DECISIONS Round 22 and 23): notes 4 (p.226) and 6 (p.227) say a time-bar sanction "may be enclosed" for an occurrence more than 2 years old, so the check is a warn, never an ack. Para 13 counts the 2 years from the occurrence. The officer may type the occurrence date (optional, never printed); the check then counts from the earlier of that date and the date of the order being cancelled, which is never earlier than the occurrence. Without it the check uses the order\'s date alone and the copy says the occurrence may be older.',
    'Typed code of CANCELS (settled, DECISIONS Round 23): the code goes in col 6 as typed. A warn, cancels_code_unknown (p.227 note 5, where col 6 carries the occurrence code), fires when it is not a code in the manual\'s list or one the slip prints (REJOIN); it never blocks the slip.',
    'Casual leave beside another leave is checked against the annual set (GTDAL, GTDALCF, EXTNAL) and against every other Chapter 5 leave grant (phase 8).',
    'Area re-grants, which leaves carry them (phase 9, owner chose): every leave that returns the officer to the unit links HAA (p.49 note ii), SCCIA (p.53 note 2), the field-area allowances (p.55 note 2) and Transport Allowance (p.71 notes iii and v), all cited to the manual: annual leave and its carry-forward, casual and special casual leave, furlough, maternity, special maternity, child care, EOL, child adoption, paternity and the three study leaves. Not the extension, voluntary-rejoining, recall or terminal-leave orders. R&H stays annual leave only. The officer names their own code for each area; a code not named drafts nothing.',
    'Casual-leave exceptions (phase 9): no HAA re-grant after a casual leave of fewer than 14 days (p.49 note ii) and no field-area re-grant after one of fewer than 15 (p.55 note 2); SCCIA has no exception (p.53 note 2). Special casual leave is not casual leave here: the manual lists them apart (ration notes 1.5 and 1.8) and so do the Leave Rules, so it always draws the re-grant. Hospitalisation is not a leave order this release drafts, so the "or hospitalization" half of the field-area exception is not applied (Phase 11 item 49).',
    'Where the printed ODP page and another source disagree (phase 9; the owner ruled on 2026-10-27 that ODP 2026 governs): the slip follows the printed page, as DECISIONS Round 30 settled. The Handbook on Pay & Allowances 2023 (p.141) lets an absence of up to 14 days continue HAA, which would make 14 days need no re-grant, while the ODP says "less than 14 days"; and MoD letter of 24 Jan 2020 (handbook p.136) lets casual leave of up to 20 days continue the field-area allowances, while the ODP says "less than 15 days".',
    'No stop for HAA, SCCIA or the field areas (settled, Round 9): the officer leaves the area on leave, but the re-grant alone is drafted until there is evidence of what HRMS publishes. STPHH, STPSCCIA and STPFIELD are never drafted from leave.',
    'Transport Allowance dates (phase 9; the ODP only says "from the date admissible" and "absence covering a full calendar month", so the exact dates fill that gap from the Handbook on Pay & Allowances 2023 pp.124-125 and the PCDA(O) FAQ): the allowance is lost only for a calendar month wholly covered by leave and a part month is paid in full. STPTRAN is therefore dated the first day of the first month the absence covers in full, and the grant the first day of the month after the last such month. The absence runs from the first prefix day to the last suffix day. Nothing is drafted when no month is covered in full (05 Jul to 03 Aug) and one is drafted for 25 Jun to 05 Aug.',
    'Transport Allowance columns (phase 9): the reason is fixed words of ours in column 3 and column 7 ("absence on leave covering a full calendar month", "rejoining from leave after an absence covering a full calendar month"). Station name in column 8. Categories 3 and 4 print the nature of the disability in columns 3 and 9 (note iv); category 5 prints the rank in columns 3 and 7 (note vi) and in column 9 (the table); categories 1 and 2 print nothing in column 9. The stop prints the station in column 8 as the table asks.',
    'HAA, SCCIA, field-area and Transport Allowance certificates (phase 9): HAA prints the one certificate the ODP carries (items i to iv, p.50), with only the category line of the officer\'s own code; item v (daily allowance not claimed on temporary duty) is left to the clerk, and the height and the Div/Bde stay blank. Annexure IV Sr 1 and 1A, which notes iv and v name, are not in the PDF. SCCIA prints items 1, 2 (the first alternative) and 3; the "OR" alternative for local units is left to the clerk. The field-area certificate prints items 1 to 3 with the area word of the code. Transport Allowance: items i, ii and iv for categories 1 and 2, v added for 3 and 4, vi in place of iv for 5; iii (a course) is never printed; the posting date in ii and the staff-car date in vi stay blank.',
    'Field-area columns (phase 9): GTDHAFA puts the end of the Government sanction in To (note 3) and an optional operation name in Data 4 (note 5); GTDCFAA and GTDCMFA have no To. HAA Data 1 to 4 follow the printed table (station code, rate, Corps and notification, serial), not note i, which also mentions the location and height. SCCIA puts the operation where the manual writes "OP" and a row of dots in angle brackets, and in Data 3 (notes 3 and 4); the printed "(at all places)" beside the peace-area From date is dropped.',
]


def _slot_words(slot):
    kind, between = slot['kind'], ', '.join(slot.get('between') or [])
    if kind == 'days':
        return f'worked out: days from {between}, both days counted'
    if kind in ('earliest', 'latest'):
        return f'worked out: {kind} of {between}'
    if kind == 'next_day':
        return f'worked out: the day after {between}'
    words = [kind]
    if 'if_missing' in slot:
        words.append(f'optional, prints "{slot["if_missing"]}" when left out')
    if 'for' in slot:
        words.append(f'only for the `{slot["for"]}` check, not printed')
    return '; '.join(words)


def _prints(col):
    if col.get('slot'):
        return f'`{{{col["slot"]}}}`'
    return f'`{col["text"]}`' if col.get('text') else ''


def _citation(rule):
    if rule.get('basis') == 'practice':
        return 'seen in practice, not in the manual'
    return f'ODP-2026 p.{rule["page"]}' + (f' note {rule["note"]}' if rule.get('note') else '')


def _link_citation(link):
    if link['basis'] == 'practice':
        return 'seen in practice, not in the manual'
    return f'ODP-2026 p.{link["page"]}' + (f' note {link["note"]}' if link.get('note') else '')


def _section(t, rules, codes):
    row = next((r for r in codes['serials'] if t['code'] in r['codes']), None)
    group = row['group']
    where = f"ODP-2026 serial {row['serial']} · " + (f"Group {group} ({codes['groups'][group]})" if group else 'no group printed') + f" · printed page {t['page']}"
    out = [f"## {t['code']} — {row['name']}", '', where, '']
    if t.get('print'):
        out += [f'Printed on the slip as `{t["print"]}`.', '']
    out += ['Narrative:', '', f'> {t["narrative"]}', '']
    if t.get('authority'):
        out += ['Authority line:', '', f'> {t["authority"]}', '']
    if t.get('certificate'):
        out += ['Certificate:', '', f'> {t["certificate"]}', '']
    out += ['| Column | Need | Prints |', '| --- | --- | --- |']
    cols = [t['from'], t['to']] + t['data']
    out += [f'| {name} | {NEED_WORDS[c["need"]]} | {_prints(c)} |' for name, c in zip(COLUMNS, cols)]
    out += ['', '| Slot | Asks | Kind |', '| --- | --- | --- |']
    out += [f'| `{s["id"]}` | {s["label"]}: {s["help"]} | {_slot_words(s)} |' for s in t['slots']]
    by_id = {r['id']: r for r in rules['rules']}
    if t.get('rules'):
        out += ['', 'Checks:', '']
        out += [f'- `{rid}`: {by_id[rid]["severity"]}, {_citation(by_id[rid])}' for rid in t['rules']]
    if t.get('links'):
        out += ['', 'Linked orders:', '']
        for link in t['links']:
            when = f' (when `{link["when"]}`)' if link.get('when') else ''
            out.append(f'- -> `{link["code"]}`{when}: {_link_citation(link)}')
    return out + ['']


def cited_pages(templates, rules):
    """Every printed page a template, rule or link cites, so the owner can open exactly those pages of the PDF."""
    pages = {t['page'] for t in templates} | {r['page'] for r in rules['rules'] if r.get('page')}
    pages |= {l['page'] for t in templates for l in t.get('links', []) if l.get('page')}
    return sorted(pages)


def render(templates, rules, codes):
    out = ['# Part II content review pack', '',
           'Generated by `tools/part2_review.py` from `part2/templates/*.json` and `part2/rules.json`; do not edit. Open the PDF at each',
           f'printed page named in a section ({", ".join(str(p) for p in cited_pages(templates, rules))}) and check the narrative, every',
           'column and every citation against it. A correction goes in the template JSON, and its golden test changes in the same commit.',
           '', '## Judgment calls to check', ''] + [f'- {p}' for p in OPEN_POINTS] + ['']
    for t in templates:
        out += _section(t, rules, codes)
    return '\n'.join(out).rstrip() + '\n'


def main(argv):
    text = render(part2_templates.load_templates(), part2_rules.load_rules(), part2_codes_check.load())
    if '--check' in argv:
        with open(REVIEW_PATH, encoding='utf-8') as fh:
            if fh.read() != text:
                sys.exit('part2_review.md is out of date: run python3 docs/Plan/rule_cards/tools/part2_review.py')
        print('part2_review.md is current')
        return
    with open(REVIEW_PATH, 'w', encoding='utf-8') as fh:
        fh.write(text)
    print('wrote', os.path.relpath(REVIEW_PATH))


if __name__ == '__main__':
    main(sys.argv[1:])

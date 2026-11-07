"""Casualties whose Annexure III row cannot be read by the rules in part2_columns.py, each read by eye from the printed page and its
notes on 2026-10-30. {serial: {'reason': ..., 'cols': [(need, caption) x 6: From, To, Data 1-4]}}. Captions are the notes' own words.
Every row listed here is always on the owner's spot-check sheet."""
R, O, N = 'required', 'optional', 'none'
NOTES = 'The table prints example values; read from the notes under it.'

HAND = {
    '3.53': {'reason': 'Data 4 is asked for by category, so it is optional; both "required as" markers dropped.',
             'cols': [(R, 'dd/mm/yyyy'), (N, ''), (N, ''), (R, '(reason for grant)'), (R, '.. (name of the station)'),
                      (O, 'For Cat-3 & 4, Name/ Type of the disability For Cat-5, Rank of the Officer')]},
    '5.26': {'reason': 'The table prints example figures in Data 1-4; captions are the words of the cells (a) to (d) and note 2 beneath.',
             'cols': [(R, 'dd/mm/yyyy'), (N, ''), (R, 'Annual Leave accumulated for current year'),
                      (R, 'Total Annual Leave Accumulation at the credit of officer'),
                      (R, 'accumulation of previous year'), (R, 'Annual Leave encashment claimed on LTC during current year')]},
    '13.31': {'reason': 'The header numbers run into the cells here; read from the page and notes 1-2 (Data 4: certificate cases, col. 9).',
              'cols': [(R, 'dd/mm/yyyy'), (N, ''), (R, 'Name of Course'), (R, '% of marks obtained'), (R, 'language'),
                       (O, 'participation / awarded certificate of proficiency/ grading')]},
    '13.32': {'reason': NOTES + ' Notes 2-7 (p.219); To is calculated by the system (note 3).',
              'cols': [(R, 'dt of purchase of IT device'), (N, ''), (R, 'Cost of IT device incl all taxes'),
                       (R, 'Date of retirement in present rank'), (O, 'Dt of last purchase, if any'),
                       (O, 'Ref PTO of last IT device disposal')]},
    '13.33': {'reason': NOTES + ' Notes 1-6 (p.220); To remains blank.',
              'cols': [(R, 'date of disposal'), (N, ''), (R, 'Original cost (incl all taxes) of procurement'),
                       (R, 'Dt of retirement'), (R, 'Dt of procurement'), (R, 'Ref PTO of LAPPROC')]},
    '13.34': {'reason': NOTES + ' Notes 2-4 (p.221); Data 3 prints 32,525 and no note names it; Data 4 and To are blank.',
              'cols': [(R, 'exit/ SoS dt from service'), (N, ''), (R, 'Original cost of procurement (incl all taxes)'),
                       (R, 'Date of procurement/ issue'), (R, ''), (N, '')]},
}


# Where the printed page and the old template disagree, the page wins (plan 13, owner decision 10). Each entry is
# (code, column) -> (what the table says, what the oracle says, why); a stale entry fails, so nothing hides here.
SHARED = 'one printed cell (p.70) serves all five variants: "For Cat-3 & 4 ... For Cat-5 ..."; the oracle split it per variant'
KNOWN_DISAGREEMENTS = {
    ('GTDHAFA', 'd4'): ('none', 'optional', 'p.54 prints "Not required" in Data 4'),
    ('GTDTRAN1', 'd4'): ('optional', 'none', SHARED),
    ('GTDTRAN2', 'd4'): ('optional', 'none', SHARED),
    ('GTDTRAN3', 'd4'): ('optional', 'required', SHARED),
    ('GTDTRAN4', 'd4'): ('optional', 'required', SHARED),
    ('GTDTRAN5', 'd4'): ('optional', 'required', SHARED),
    ('LVELTCAD', 'd3'): ('none', 'optional', 'p.134 prints "Not Required" in Data 3'),
    ('LVELTCAD', 'd4'): ('none', 'required', 'p.134 prints "Not Required" in Data 4'),
}

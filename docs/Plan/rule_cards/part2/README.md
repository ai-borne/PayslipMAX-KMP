# Part II data (Orders Bank)

Source of truth for the Part II wiki. Everything here is data about the ODP 2026 manual; the manual itself and its page images are never
committed. Plan: `docs/Plan/13_OrdersBank_PhasePlan.md`, Phase 1 in `13a_OrdersBank_Execution_P0-P2.md`.

| File | What | Made by | CI |
|---|---|---|---|
| `codes.json` | 303 serials, 321 codes, chapter, group, the manual's name, detail page | `tools/part2_codes.py` (local sources) | shape check |
| `nav.json` | the 10 ribbons over those serials | hand-authored, `tools/part2_nav.py` checks | yes |
| `columns.json` | Annexure III: what From, To and Data 1-4 need for each of the 303 serials, with the manual's own captions | `tools/part2_columns_extract.py` (local PDF), rules in `tools/part2_columns.py`, hand readings in `tools/part2_columns_hand.py` | `tools/part2_columns_check.py` |
| `about.json` | the 30 plain-English pages (48 serials): what it is, when you see it, what to keep ready, one thing to watch | written by hand from the ODP and PCDA handbooks; sources only in a local sheet | `tools/part2_about_check.py` |
| `related.json` | "usually published with" links for 15 casualties, each `manual` (with page) or `practice` | exported once from the old templates | `tools/part2_related_check.py` |

`columns.json` has one row per line on purpose (a review diff reads row by row), so it is over 300 lines; `test_part2_file_sizes.py` lists it in
`ONE_ROW_PER_LINE` and still limits it to three times the limit. A column is `required`, `optional` or `none`; a `none` column has no caption.
A row with a `hand` field was read by eye from the page or its notes, or prints an example figure in place of a caption; the owner's
spot-check always includes those rows. Captions are the manual's own words minus the "Required as" boilerplate; the only edits are mending
words and dates a narrow column cut in two and dropping the `<` `>` of placeholders such as `<Type of Leave>`.

To rebuild `columns.json` (needs the owner's local PDF, `ODP_PDF=/path/to/Documentation Procedure Offrs-2026.pdf`):
`python3 -I tools/part2_columns_extract.py raw.json` then `python3 tools/part2_columns_extract.py --build raw.json`. The raw file and the
PDF stay out of git; neither script runs in CI.

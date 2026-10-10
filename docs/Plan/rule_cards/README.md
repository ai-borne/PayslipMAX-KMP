# Rule cards (Claim Guide dataset): start here

Everything for this project lives in this folder, except the extracted source text (copyrighted, kept outside git).

| Path | What |
|---|---|
| `authoring/*.txt` | Card text: the source of truth. Edit these only. |
| `rulebook.json`, `RULEBOOK.md` | Generated. Never hand-edit. |
| `composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json` | The app bundle, generated from `rulebook.json` by `tools/bundle.py`. Never hand-edit. |
| `nav.json` | Tile navigation (areas > cases) for the Guide UI. Authored; `compile.py` validates it and writes it into `rulebook.json`. |
| `ssot.json`, `register.json` | The 474 source entries, topics, coverage targets. |
| `tools/ids_lock.json` | Every card ID that has shipped. An ID may only leave via `--- retire RB-x reason` in an authoring file; `refresh.py` adds new ones. |
| `tools/` | All scripts (below). Run from anywhere; paths resolve from the script. |
| `13_review_queue.md` | Cards with open points (generated). |
| `14_review_decisions.md` | Pending review decisions (fill in, then ask to apply). |
| `15_confirmed_rulesets.md` | Points already confirmed and applied. |
| `17_expert_review_intake.md` | Template for the expert's replies, one row per card ID, and the steps to apply them. |
| `18_content_update_runbook.md` | **How to ship a content change**: report, intake, edit, `refresh.py`, change log, gate, release; the 8th CPC playbook; what the owner does by hand. |
| `HANDOFF.md` | Current state, decisions, to-do. Read before working. |
| `00-11_*.md`, `authentic_cards.json` | Earlier inventory and background. |

## Commands (repo root)
After any content edit run one command: `python3 docs/Plan/rule_cards/tools/refresh.py` (rebuilds everything, runs all checks;
`--check` writes nothing and is what CI runs). The individual steps:
```
python3 docs/Plan/rule_cards/tools/compile.py --check     # validate (add --uncovered to list gaps)
python3 docs/Plan/rule_cards/tools/compile.py             # write rulebook.json
python3 docs/Plan/rule_cards/tools/render_md.py           # write RULEBOOK.md
python3 docs/Plan/rule_cards/tools/bundle.py              # write the app bundle (--check: is it stale?)
python3 docs/Plan/rule_cards/tools/copycheck.py           # own-words guard; must print CLEAN
python3 -m unittest discover -s docs/Plan/rule_cards/tools -p 'test_*.py'   # all tool tests (CI runs these)
python3 docs/Plan/rule_cards/tools/review_queue.py        # regenerate 13_review_queue.md
python3 docs/Plan/rule_cards/tools/review_pack.py --pdf    # expert hand-out in review/ (git-ignored; omit --pdf for HTML only)
python3 docs/Plan/rule_cards/tools/rates_report.py        # 8th CPC sweep list: every card that could quote a rate, plus every figure
python3 docs/Plan/rule_cards/tools/todo_pay.py            # uncovered pay entries by topic
python3 docs/Plan/rule_cards/tools/packet.py SS-P001,SS-P002   # source text behind entries
python3 docs/Plan/rule_cards/tools/pa.py "Joining Time" 400    # a P&A handbook section
```

## Rule changes (dated rules and the change log)
A card line may carry `effective=YYYY-MM-DD`. A newer card with `replaces=RB-old effective=...` retires the old one from the tiles
without deleting it: the old card keeps its data and gains `replaced_by` and `until` (the new card's date). `authoring/changes.txt`
holds the hand-written dated change log (`== 2026-11-15`, then `- text [RB-x, RB-y]`); the bundle ships the newest 12 entries.
`compile.py` checks every id, date order and cycle. Every shipped card also gets a `rev` (hash of its text) in the bundle.

## Source text (outside the repo)
`~/Downloads/rulecards_workdir/` holds the handbook, FAQ and OCR'd TR 2014 text. Override with
`RULECARDS_SOURCES=/path`. `compile.py` and `render_md.py` do not need it; `packet.py`, `pa.py`,
`todo_pay.py` and `copycheck.py` do. If it is missing, ask the owner; it is rebuilt from the PCDAO PDFs.

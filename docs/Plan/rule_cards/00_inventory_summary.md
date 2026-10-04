# Rule cards - inventory and canonical register (2026-10-03)

Purpose: size and de-duplicate the rule base behind the "Claim Guide" / rule-card feature. A **rule card** is one
situation in the officer's words: one-line answer, authority to cite, documents to attach, a "watch out" line.
No card text has been written or verified yet. This folder is the raw inventory plus the canonical register.

## The SSOT list (current deliverable)

**One list, supported by TR rules and letters, complemented by the FAQ and the PCDA(O) handbooks.** Nothing is dropped for lacking a
primary letter; every entry carries labels showing what backs it (support is a label, not a filter).

| | Topics | Entries |
|---|---|---|
| Travel (TD, permanent move, LTC, claims) | 67 | 347 (115 from FAQ, 224 handbook-only, 8 TR-only) |
| Pay and allowances | 95 (19 with entries) | 127 (FAQ) + 76 handbook-only topics still to be broken into entries |
| **Total today** | 162 | **474** |

Support labels across entries: strong 109, good 169, basic 196. Lists: [09_ssot_travel.md](09_ssot_travel.md),
[10_ssot_pay.md](10_ssot_pay.md), data in [ssot.json](ssot.json). Each entry holds the source ids (FAQ id, handbook para, TR rule, letter key) for
later re-checking; the simplified answer text (our own words) is the next phase.

Where the layers overlap, they were merged: 13 handbook paragraphs folded into the FAQ they restate, 3 duplicate handbook titles collapsed.
Beyond that the FAQ and the handbook rarely say the same thing (most handbook paragraphs have almost no overlap with any FAQ in their topic),
so they were kept as complementary entries under one topic.

Supporting evidence from the earlier authority check (kept as labels and notes, not gates): [07_authentic_cards.md](07_authentic_cards.md)
(the 47 TR + 20 letter-anchored FAQ situations with the strongest backing) and [08_order_letter_verification.md](08_order_letter_verification.md)
(what each MoD/MoF/DoPT letter does and does not support; escalated rupee figures; TR rules amended by the 2017-18 letters).

## Files

| File | What it is |
|---|---|
| [09_ssot_travel.md](09_ssot_travel.md), [10_ssot_pay.md](10_ssot_pay.md), [ssot.json](ssot.json) | **The SSOT list** (topics and entries with support labels) |
| [07_authentic_cards.md](07_authentic_cards.md) | Strongest-backed subset (62 ready, 5 on hold) |
| [08_order_letter_verification.md](08_order_letter_verification.md) | Primary-letter check of the 21 order cards: what each letter does and does not support |
| [05_register_travel.md](05_register_travel.md) | **Canonical travel register (SSOT index):** 67 topics, each with its TR rule anchors and linked FAQ / handbook items |
| [06_register_pay.md](06_register_pay.md) | Canonical pay-and-allowances register (topic level) |
| [register.json](register.json) | Machine-readable register (topics, TR rule dispositions, pay topics, item ids, review queue) |
| [04_catalog_tr2014_rules.md](04_catalog_tr2014_rules.md) | Every live TR 2014 rule with its disposition (IN / OUT / NICHE) |
| [01_catalog_faq.md](01_catalog_faq.md), [02_catalog_ta_handbook.md](02_catalog_ta_handbook.md), [03_catalog_pa_handbook_topics.md](03_catalog_pa_handbook_topics.md) | Raw per-source catalogs (inputs to the register) |

## Why the list looked huge: it is layers, not copies

| Source | Raw units | Role |
|---|---|---|
| A. PCDA(O) Pune website FAQ (251 Q&A, updated to 2026-06) | 251 (116 travel, 135 pay) | Situations in officers' words |
| B. Handbook on Travelling Allowances 2023 | 311 paragraphs | Claim practice, 7th-CPC restatements |
| C. Handbook on Pay & Allowances 2023 | 117 index topics | Pay, leave, allowances, DSOP, HRA, ... |
| D. Travel Regulations 2014 (scan, OCR'd locally) | 289 live rules (27 BLANK) | The paragraph numbers officers cite |

Total raw units: 251 + 311 + 117 + 289 = **968**. The copies are few; the redundancy is **many-to-one**: the TR defines a
rule, the TA handbook restates it, and FAQ entries apply it to situations. Near-duplicate text is rare
(**7 removable items** out of 378 travel items, found by similarity ≥ 0.70). Collapsing by *rule* is what shrinks the list.

## Result

**Travel domain.** 289 TR rules + 116 FAQs + 311 handbook paragraphs (716 units) collapse to **67 canonical topics**:
51 TR-anchored and 16 order-based (topics that exist only in post-2014 MoD/DoPT/Army letters, so no TR rule).

| TR rule disposition (all 289 live rules, none dropped) | Count |
|---|---|
| IN: Army-officer-facing, mapped to a topic | 149 (into 51 topics) |
| OUT: civilians, PBOR/JCO/ex-servicemen, Navy/AF, recruiting, stores, prisoners | 99 |
| NICHE: overseas postings (rules 244-279 etc.), deferred | 41 |

| Source items (travel) | Explicit TR cite | Keyword / order / section rule | Not assigned |
|---|---|---|---|
| FAQ (116) | 62 | 53 (9 keyword, 38 order, 6 section default) | 1 out of scope |
| TA handbook (311) | 165 | 75 (21 keyword, 54 order) | 71 (46 form fields or table rows, 12 niche or out, 4 cross-pay, 9 unassigned) |

Confidence: explicit-cite assignments are high; keyword, order and section-default assignments are **medium** and need a
read before cards are written. 9 handbook items stay unassigned (headings and form fragments) and are listed in
`register.json` under `unassigned`.

**Where the substance is.** Six topics hold about 40% of attached items: Anywhere-in-India LTC (60 items), Home Town LTC (59),
definitions (26), permanent-move TA (26), daily allowance on TD (22), LTC general (21). Median topic: 4 items. Seven topics have no FAQ or
handbook coverage (TR-only): GEN-04, MOD-06, MOD-07, PDM-04, TD-12, LTC-06, TPT-01.

**Pay domain** (topic level, see 06): 95 rule-bearing topics IN, 14 DEFER (tax, insurance, pension, ECHS), 8 OUT. 135 FAQs attach through 20 FAQ sections (a few sections map to two topics).
Individual rules inside topics are **not yet extracted**: the handbook is mostly tables. Needs a second pass: **Chapter 13 (7th CPC
pay and allowances, pp. 77-104) has no sub-topics in the index** and holds the core pay rules; chapters 21 (DSOP), 22 (accommodation),
23 (HRA) are also large.

### Card volume estimate (revised from the earlier 650-750)
- Travel: about 355 distinct source items attach to topics, but many are procedure or table fragments; after merging
  near-duplicates and dropping non-cards, **about 220-270 cards**, plus 1-3 TR-only cards for each of the 7 uncovered topics.
- Pay: **about 300-400 cards** (unchanged; weakest estimate until chapter 13 is decomposed).
- **Total about 550-650.** First-release slice: the TR-anchored TD + permanent-move + LTC topics (13 + 7 + 8 = 28 topics, 200 distinct items) is about 100-140 cards; adding the order topics TD claims depend on (16 more topics, 292 items in all) is about 150-200 cards.

## Cross-reference findings
1. All 25 distinct TR rule numbers cited by the FAQ resolve to a heading in the OCR'd TR 2014. Existence of a cited rule can
   be checked automatically. Whether the rule says what the card claims needs a human read.
2. **Correction:** an earlier version of this doc said the FAQ's "TR 230A" is a BLANK rule. That was a parser error. TR 230A
   is a real rule ("Grant of Transport Allowance to ..."); it and 230B anchor topic RR-TPT-01.
3. TR 2014 rates are by Grade Pay (pre-7th CPC), so they are stale. Cards take rates from the 7th-CPC orders and use the TR only
   for rule numbers and conditions. Topic RR-ORD-* entries carry the order leads (unverified).
4. TR 258 (Composite Transfer Grant) is the **overseas** rate rule. The FAQ's domestic CTG answers rest on a 7th-CPC order, hence RR-ORD-06.
5. FAQ "Rule 40 Note 2", "124", "127", "150", "157", "177A-D" etc. all land in chapters THREE/FOUR as expected; the FAQ also mixes
   "TR 1991 Edn" and "TR 2014" references, so each card needs an edition tag.
6. The OCR missed rules 72 and 84; the other 287 headings (incl. 41 lettered rules like 61-A, 177A) were recovered.

## Decisions recorded
- **Copyright approach (user, 2026-10-03):** cards are written in our own words from the facts. The user-facing CITE line names
  the **primary authority** (TR rule, MoD/DoPT letter, Army order), never a handbook or website. No source passages are copied or
  lightly reworded. Provenance (FAQ id, handbook para/page, TR page checked) is kept in hidden card metadata for re-verification only.
- The Pay & Allowances Handbook says it "should not ... be quoted as an authority"; it is a lead to the authority, not the authority.
  (`PayAuthorities.kt` still cites it in parentheses next to the MoD letter; not changed, flagged to the user.)

## Method and limits
- TR text from local OCR (macOS Vision), column-ordered, **not proofread**; titles may be truncated.
- Handbook B units are regex-detected numbered paragraphs; 43 are over 300 words and will split into several cards.
- FAQ and handbook items were mapped to topics by (1) explicit TR cites, (2) LTC sub-rule detection (177A-D), (3) specific keyword
  rules, (4) post-TR order patterns, (5) FAQ section defaults. Text similarity was tested and **rejected for auto-assignment**
  (top-1 accuracy 58% even with leave-one-out enrichment). The topic list and scope lists (IN/OUT/NICHE) are hand-curated.
- "Authority leads" in 05 are pattern-extracted from FAQ/handbook text: unverified, partly noisy.
- The build scripts and the OCR text live in the session scratchpad and are **not committed** (the OCR text derives from the
  copyrighted PDFs). Only the register (our own mapping) is committed here.

## Next steps (for the user to decide)
1. Review the medium-confidence assignments for the first-release topics (TD, PDM, LTC) and the 9 unassigned items.
2. Decompose pay chapter 13 so the pay register reaches rule level.
3. Verify the order-topic authorities (the 16 RR-ORD topics) from primary letters.
4. Draft and verify the first 20 cards from RR-TD-02, RR-ORD-03/04/05 (food, hotel, own car) as a format test.

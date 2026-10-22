# Part II Order: ribbon layout and build order (discussion proposal)

> [!NOTE]
> Agreed by the owner on 2026-10-22 (`DECISIONS.md`, Rounds 8–9), with these amendments, which win where this file differs:
> the Leave release also drafts the ration four, R&H stop + re-grant ("seen in practice") and the HAA/field/SCCIA re-grant
> (not just links); the slip prints `REJOIN`, not `REJOINAL`; CANCEL/CANCELS ship with Leave; Q1–Q10 below are closed.
> The `layout.py` and `master.tsv` it cites (the hand-checked 303-serial list) are kept locally, outside git, in
> `~/Downloads/rulecards_workdir/part2/agent_layout_2026-10-22/`.

Proposal for the owner, 2026-10-22. Not a plan, and nothing in the repo has changed. Every code and
note below was read from ODP-2026 (the text dump `full_text_layout.txt`, with the PDF re-extracted by
`pdftotext` for unclear rows). Page numbers are the manual's printed page numbers.

## 1. Summary

- **303 Annexure II serials** across 14 chapters (not 302). That is **321 distinct HRMS codes**, because 8
  serials carry variant codes (3.27 TLA1-3, 3.51 BCAS0-2, 3.53 TRAN1-5, 3.67 QPAY1-5, 3.80 ISDA1-3,
  3.90 HARD1-2, 3.94 STFA1-2, 10.9 WAIVER1-3). Serial 9.7 `WTHDPAY` is in the index (p.23, detail p.171)
  and in the flawed JSON, so the decision record's "9.7 not found" and "302" need correcting.
- **10 ribbons, 48 cases, 303 serials, each homed exactly once** (checked by script, section 5). 53 serials
  are marked lookup-only: 50 in five lookup-only cases, plus 3 inside other cases.
- Sizes follow the Guide's (9 areas, 44 cases, 2 to 7 cases per area): 3 to 7 cases per ribbon, and 2 to
  14 homed codes per case.
- **Build order:** Leave → Postings and moves + Field, high-altitude and hard areas (one release) →
  Promotion, courses and awards → Family and children → Leaving the Army → Hospital and health →
  Allowances for your role → Deputation and special postings → Records and unit-only orders (lookup
  ships from release 1; its few drafts come last).

### Conventions borrowed from the Claim Guide

- `nav.json`: the Guide has areas, then cases. A case has a `title`, a short `sub`, its homed members, and
  `also` cross-links. `tools/nav.py` fails the build if an item has no home or two homes, or if a limit is
  broken. The Guide's limits are 8 to 10 areas, at most 8 cases per area, 4 to 16 cards per case, a title
  of at most 7 words and a sub of at most 8.
- Guide titles are plain-English noun phrases ("Annual, casual and sick leave", "Who can take LTC, and
  when"). The proposal uses the same style. Proposed `sub` text: chapter and pages, e.g. "Ch 5, pp 114-116".
- Proposed Part II limits: 8 to 10 ribbons, at most 8 cases per ribbon, **2 to 16** homed codes per case,
  and a title of at most 7 words. The minimum drops from the Guide's 4 to 2 because some code families are
  naturally pairs (cancellation, IT devices, last orders before leaving). See Q1.
- Display order is the build order, as the Guide shows areas in bundle order. The ribbon's homed codes are
  the "cards". An "also" entry is a link to a code homed elsewhere.

## 2. Ribbon order and rationale

Scores: H = high, M = medium, L = low. For authoring cost, H means expensive. "Nudge" means
`ServiceTimeline` already detects something that could open the ribbon.

| # | Ribbon | Codes | Frequency in a career | Money at stake | Authoring cost | Synergy with app | Nudge |
|---|---|---|---|---|---|---|---|
| 1 | Leave (decided) | 30 | H (every year) | H | M | Leave Bank, Claim Guide leave cards | none |
| 2 | Postings and moves | 23 | H (every 2-3 yrs) | H | H (HRA certificates pp.73-80) | Guide move/home-hra cards, Pay Audit | TPTA city change, quarters in/out |
| 3 | Field, high-altitude and hard areas | 34 | H (each field tenure, re-grants after leave) | H (missed cessation → recovery) | M (R&H 11 grants share one template; corps notification data + certificates) | Pay Audit `ServiceTimeline`, Guide allow-rh/allow-field | R&H and field allowance spans start/stop |
| 4 | Promotion, courses and awards | 31 | M (about 6-8 promotions; courses often) | H (DNI/DOP option, 3-month window) | M | Pay Audit `PayFixationCalculator`, Guide pay-promo | promotion/level change |
| 5 | Family and children | 33 | M (marriage, births; CEA claims every year) | M | L-M (mostly Group C, few data fields) | Guide home-cea cards | none |
| 6 | Leaving the Army | 20 | L (once) | H | L | Retirement Calculators, Leave Bank (ENCASH already in Leave) | none |
| 7 | Hospital and health | 18 | L-M | M-H (regularisation at furlough rates) | H (chained order nos., 5 regularisation codes) | Leave (AL debits), Leave Bank | none |
| 8 | Allowances for your role | 34 | M for aviators, paras, SF and specialists; nil for others | M-H | H (monthly/yearly re-publication rules) | Guide allow-air/allow-tech | none |
| 9 | Deputation and special postings | 28 | L | M | M | none | none |
| 10 | Records and unit-only orders | 52 | L (drafts); lookup | L | L (42 of its 52 codes are lookup-only) | none | none |

Why this order:
- **2 and 3 together.** The manual makes a posting-out order carry the allowance stoppages (POSOUT note 4,
  p.200; STPFIELD notes 2-3, p.56). A Moves ribbon without the Chapter 3 stop codes would reproduce the
  owner's first failure, "missed cessation → recovery". Field also has the strongest payslip nudges and
  the Leave link (F1 below). If one release is too big, ship Moves with only R&H, field, HAA and SCCIA
  first (26 codes), then the rest of the ribbon.
- **4 before 5.** A promotion happens less often than a family event, but it is the only remaining ribbon
  with a nudge, a 3-month deadline (p.110) and an existing calculator (DNI vs DOP).
- **5 before 6.** CEA and hostel claims recur every year for most officers. Exit happens once, and its
  biggest money item (ENCASH) already ships with Leave.
- **6 before 7.** Leaving the Army is small and cheap, and Retirement Calculators make it more useful.
  Hospital is costlier to write because each order quotes the admission order (note 2, p.142).
- **8 and 9 last** among the draft ribbons: they serve niche groups, and `ServiceTimeline` cannot detect
  them.
- **10.** Code lookup must cover every code from release 1 (Round 2/3). The lookup-only cases are cheap
  minimal code pages, so they can ship early. Its drafts (PAN, mobile, e-mail, cancellation) can come
  later, apart from cancellation (Q6).

## 3. Ribbons, cases and codes

`[B]` = Group B claim, which needs its own order (section 4, G1). "(lookup)" = the officer never drafts
it: the unit, a board, MS Branch or PCDA(O) originates it. A case marked *(lookup-only)* has code pages
and no draft. Case titles follow the Guide's style.

### 1. Leave

- **Annual and casual leave**: 5.1 GTDAL, 5.2 GTDALCF, 5.3 EXTNAL, 5.4 GTDCL, 5.5 GTDSPCL. Also: 6.6 GTDSL, 6.12 GTDALHOS, 13.5 JNTIME
- **Back from leave, early or recalled**: 5.23 REJOINAL, 5.24 VOLREJ, 5.25 RECALL. Also: 3.13 GTDHH11, 3.14 GTDHH31, 3.15 GTDHH32, 3.19 GTDCIAFD, 3.20 GTDCIAMF, 3.21 GTDCIAPC, 3.23 GTDHAFA, 3.24 GTDCFAA, 3.25 GTDCMFA, 3.53 GTDTRAN1
- **Furlough, study, EOL and terminal leave**: 5.6 FURLEV, 5.18 EXTNFUR, 5.15 STDLEV, 5.16 STDLVAL, 5.17 STDLVFUR, 5.10 EOL, 5.11 TERLEV. Also: 3.62 STPSPLP, 13.5 JNTIME, 2.2 RETIRE
- **Maternity, paternity and child care**: 5.7 MTRNLV, 5.8 SPMTRNLV, 5.9 GTDCCL, 5.12 CHADPLVE, 5.13 PTRNLV, 5.14 PATLCHAD. Also: 11.9 BTHCHL, 11.14 ADOPTION
- **Late back or absent without leave**: 5.19 OSL, 5.20 REJOSL, 5.21 AWL, 5.22 REJAWL
- **Year-end leave and encashment**: 5.26 ACCLVE, 5.27 LVELTCAD, 5.28 LVELTC, 5.29 CLVLTCAD, 5.30 ENCASH. Also: 2.2 RETIRE

### 2. Postings and moves

- **Posted out, posted in**: 13.2 POSOUT, 13.1 POSIN, 13.5 JNTIME, 13.6 EXTRJT. Also: 3.86 SORS, 3.87 TORS, 3.12 STPRH, 3.16 STPHH, 3.22 STPSCCIA, 3.26 STPFIELD, 3.54 STPTRAN, 3.58 STPHRA, 4.1 ASMAPPT, 4.2 RELAPPT, 13.3 SPR
- **HRA, SPR, furniture and transit stay**: 3.55 GTDHRAX, 3.56 GTDHRAY, 3.57 GTDHRAZ, 3.58 STPHRA, 13.3 SPR, 13.4 CHNGSPR, 8.1 REIMBFUR [B], 8.10 REIMACCN [B]
- **Transport and ration allowances**: 3.53 GTDTRAN1/GTDTRAN2/GTDTRAN3/GTDTRAN4/GTDTRAN5, 3.54 STPTRAN, 3.82 GTDRSHNA, 3.83 STPRSHNA, 3.84 GTDSRA, 3.85 STPSRA, 3.86 SORS, 3.87 TORS
- **Temporary duty and attachments**: 13.7 TYDUTY, 13.8 ATCH, 13.9 CSAT. Also: 3.82 GTDRSHNA

### 3. Field, high-altitude and hard areas

- **Risk and hardship allowance**: 3.1 GTDRH11, 3.2 GTDRH12, 3.3 GTDRH13, 3.4 GTDRH21, 3.5 GTDRH22, 3.6 GTDRH23, 3.7 GTDRH31, 3.8 GTDRH32, 3.9 GTDRH33, 3.10 GTDRH77, 3.11 GTDRH60, 3.12 STPRH. Also: 13.2 POSOUT, 13.1 POSIN
- **High altitude and Siachen**: 3.13 GTDHH11, 3.14 GTDHH31, 3.15 GTDHH32, 3.16 STPHH, 3.17 GTDSICHA, 3.18 STPSICHA. Also: 5.23 REJOINAL
- **Field area allowances**: 3.23 GTDHAFA, 3.24 GTDCFAA, 3.25 GTDCMFA, 3.26 STPFIELD. Also: 13.2 POSOUT, 13.3 SPR, 5.23 REJOINAL
- **Counter-insurgency allowance (SCCIA)**: 3.19 GTDCIAFD, 3.20 GTDCIAMF, 3.21 GTDCIAPC, 3.22 STPSCCIA. Also: 5.23 REJOINAL
- **Tough location and island postings**: 3.27 GTDTLA1/GTDTLA2/GTDTLA3, 3.28 STPTLA, 3.80 GTDISDA1/GTDISDA2/GTDISDA3, 3.81 STPISDA
- **Difficult and hard areas**: 3.47 GTDDAA, 3.48 STPDAA, 3.90 GTDHARD1/GTDHARD2, 3.91 STPHARDA. Also: 3.97 GTDHARDS, 3.98 STPHARDS

### 4. Promotion, courses and awards

- **Promotion, appointments and antedates**: 4.1 ASMAPPT, 4.2 RELAPPT, 4.3 SUBPROM, 4.4 OPTFXDNI, 4.5 OPTFXDOP, 13.20 ANTEPROM, 13.21 ANTEPAY, 13.22 ANTEPEN. Also: 1.11 SSCGTDPC
- **Going on a course**: 12.1 PROCRS, 12.2 REJCRS, 12.4 SOSCRS, 12.5 SOSCRS1, 12.6 TOSCRS. Also: 3.62 STPSPLP, 3.33 GTDPARA, 3.34 STPPARA
- **Qualifications and achievements**: 12.3 ACQUAL, 13.31 LANGPROF, 13.26 SPORTPAR, 13.27 ACSPLACH, 3.79 SPLINCR. Also: 3.71 GTDLNGL, 3.65 GTDPSTDG, 3.63 GTDPSTDP
- **Gallantry awards and medals**: 7.1 GALPVC, 7.2 GALMVC, 7.3 GALAC, 7.4 GALKC, 7.5 GALVC, 7.6 GALSC, 7.7 GALSENA, 7.8 GALBAR, 7.9 GALASSPP, 7.10 MEDAL, 7.11 FORMEDAL, 7.12 DISTSENA, 7.13 DISTGBAR

### 5. Leaving the Army

- **Retiring, resigning or released**: 2.1 RELEASE, 2.2 RETIRE, 2.3 PRETIRE, 2.4 RESIGN, 2.5 PMTABSN, 2.9 INVALID. Also: 5.11 TERLEV, 5.30 ENCASH, 13.34 LAPDISPF
- **Last orders before you leave**: 13.19 RETADD, 13.34 LAPDISPF. Also: 5.30 ENCASH
- **Staying on: extension, PC, re-employment**: 1.7 EXTNSSC, 1.11 SSCGTDPC, 1.12 GTDREMPL, 1.13 EXTNREMP
- **Death, missing or captured** *(lookup-only)*: 2.6 DEATH, 2.7 KILLACTN, 2.8 DESERTER, 2.10 MISSING, 1.15 REMISSIN, 1.16 REPDESRT, 13.15 POW, 13.16 REPOW

### 6. Family and children

- **Marriage, separation and divorce**: 11.1 MARRIAGE, 11.2 MARSEROF, 11.5 SEPARATE, 11.6 DIVORCE, 11.7 REMAR, 11.8 REMAROF, 11.18 CHNGNTSP. Also: 11.4 NOKCHNG
- **Next of kin and nominations**: 11.3 NOK, 11.4 NOKCHNG, 13.28 NOMNDCRG, 13.29 NOMNAGIF, 13.30 NOMNDSOP
- **Children**: 11.9 BTHCHL, 11.14 ADOPTION, 11.15 STEPCHLD, 11.16 DIVYNCHL, 3.96 GTDSACC [B]. Also: 5.7 MTRNLV, 5.9 GTDCCL, 5.12 CHADPLVE, 5.13 PTRNLV, 5.14 PATLCHAD
- **Children's education claims**: 8.2 REIMBCEA [B], 8.3 RIMBCEAT [B], 8.4 REIMCEAD [B], 8.5 REMCEATD [B], 8.6 HOSTSUB [B], 8.7 HOSTSUBT [B], 8.8 HOSTSUBD [B], 8.9 HSTSUBTD [B]
- **Death in the family**: 11.10 DTHSPOUS, 11.11 DTHFAMEM, 11.12 DTHNOK, 11.13 DTHCHL. Also: 11.4 NOKCHNG
- **Family details and home address**: 11.17 DEPNDNT, 11.19 DOBCHNGF, 11.20 NAMCHNG, 11.21 HOMCHNG

### 7. Hospital and health

- **Admitted to hospital**: 6.1 HOSADM, 6.2 SLHOSADM, 6.3 MEDTRAN
- **Discharge, sick leave and back to duty**: 6.4 HOSDISCH, 6.5 SLDISCH, 6.6 GTDSL, 6.7 EXTNSL, 6.8 RPDUHSP, 6.9 SLC
- **Regularising time in hospital**: 6.10 REGAL, 6.11 REGALMS, 6.12 GTDALHOS, 6.13 GTDSLHOS, 6.14 HOSFURPY. Also: 5.1 GTDAL
- **Medical category, injury and battle casualty**: 6.15 MEDCAT, 13.11 INJURY, 13.12 BATTLCAS, 13.17 WOUNDACT

### 8. Allowances for your role

- **Flying, para and special forces**: 3.29 GTDFLPAY, 3.30 STPFLPAY, 3.31 GTDFTEA, 3.32 STPFTEA, 3.33 GTDPARA, 3.34 STPPARA, 3.35 GTDPARES, 3.36 STPPARES, 3.37 GTDSFALC, 3.38 STPSFALC, 3.39 GTDTPILA, 3.40 STPTPILA, 3.41 GTDPAJIA, 3.42 STPPAJIA
- **Specialist and instructor allowances**: 3.59 GTDSPLP1, 3.60 GTDSPLP2, 3.61 GTDSPLP3, 3.62 STPSPLP, 3.73 GTDTRGA, 3.74 STPTRGA, 3.75 GTDTRGNC, 3.76 STPTRGNC, 3.77 GTDTRGO, 3.78 STPTRGO
- **Qualification, PG, technical and language pay**: 3.63 GTDPSTDP, 3.64 STPPSTDP, 3.65 GTDPSTDG, 3.66 STPPSTDG, 3.67 GTDQPAY1/GTDQPAY2/GTDQPAY3/GTDQPAY4/GTDQPAY5, 3.68 STPQPAY, 3.69 GTDTECI, 3.70 GTDTECII, 3.71 GTDLNGL, 3.72 STPLNGL. Also: 12.3 ACQUAL, 13.31 LANGPROF

### 9. Deputation and special postings

- **Deputation and secondment**: 2.11 SECNDMNT, 2.12 PRMSECND, 2.13 DEPUTE, 2.14 DEPUOTHR. Also: 3.54 STPTRAN
- **UN, embassy and Bhutan postings**: 2.15 SECONDUN, 2.16 EMBASSY, 3.51 GTDBCAS0/GTDBCAS1/GTDBCAS2, 3.52 STPBCA
- **Deputation allowance and coming back**: 3.43 GTDDEP1, 3.44 GTDDEP2, 3.45 GTDDEP3, 3.46 STPDEP, 1.14 REVDEP, 1.17 REVSECND
- **NSG, SFF, SPIC and other special units**: 3.49 GTDHAZ, 3.50 STPHAZ, 3.88 GTDNSG, 3.89 STPNSG, 3.92 GTDSECUR, 3.93 STPSECUR, 3.94 GTDSTFA1/GTDSTFA2, 3.95 STPSTFA, 3.97 GTDHARDS, 3.98 STPHARDS, 3.99 GTDNTSSA, 3.100 STPNTSSA, 3.101 GTDSPIC, 3.102 STPSPIC

### 10. Records and unit-only orders

- **ID, contact and service record**: 13.23 PAN, 13.24 MOBILE, 13.25 EMAILID, 13.10 CHNGRELI, 13.13 CHNCORPS (lookup), 13.18 FOMSERV
- **Laptop and IT devices**: 13.32 LAPPROC (lookup), 13.33 LAPDISP (lookup). Also: 13.34 LAPDISPF
- **Cancelling a published order**: 14.1 CANCEL, 14.2 CANCELS
- **Commissioning and cadet stipend** *(lookup-only)*: 1.1 GTDPC, 1.2 GTDSSC, 1.3 GTDSSCMT, 1.4 GTDSSMNT, 1.5 GTDSSCWT, 1.6 GTDSSWNT, 1.8 GTDSPLC, 1.9 GTDSC, 1.10 REPDUTY, 1.18 GTDSTP, 1.19 STPSTP, 13.14 ALOTPRNO
- **Discipline: suspension and pay withheld** *(lookup-only)*: 9.1 ATTDIS, 9.2 REJATDIS, 9.3 SUSARST, 9.4 RESUARST, 9.5 SUSPNDTY, 9.6 RESUSPND, 9.7 WTHDPAY, 9.8 RWTHDPAY
- **Discipline: trial, punishment and exit** *(lookup-only)*: 9.9 GCMAWARD, 9.10 REPRIMND, 9.11 SREPRMND, 9.12 CONVCT, 9.13 FORFEIT, 9.14 PENLDEDN, 9.15 COMPRETD, 9.16 DISMISS, 9.17 TERMN, 9.18 REMOVE, 9.19 CASHRNG, 9.20 REINSTTS
- **Territorial Army** *(lookup-only)*: 10.1 GTDTAC, 10.2 TAEMB, 10.3 TADISEMB, 10.4 ATC, 10.5 EXTN, 10.6 SUL, 10.7 TADISCHG, 10.8 PCTRG20A, 10.9 WAIVER1/WAIVER2/WAIVER3, 10.10 RELTAC

### Notes on specific cases

- **Posted out, posted in: "Seen in practice" bundle** (labelled separately from manual links, as the
  owner decided). One real HRMS posting-out order held five occurrences with the same from-date: 13.2
  POSOUT, 3.86 SORS, 3.12 STPRH, 4.2 RELAPPT and 3.54 STPTRAN, under the header groups A/AM/C. The manual
  backs this in general terms only:
  - POSOUT note 4 (p.200): "Cessation of all location based, appointment based and unit specific
    allowances etc. will be notified with occurrence of POSOUT".
  - Para 7.1 (p.5): A, AM, C and CM may share one order.

  The manual never lists these five codes as a set. The case shows the manual's rule with its citation,
  then the five-code bundle as "Seen in practice".
- **Authority line** (the slip carries one per occurrence). The manual names an authority in these cases:
  - Moves: POSIN/POSOUT, the IHQ of MoD (Army)/DGAFMS posting letter (note 3, p.200); the practice order
    quoted an MS Branch signal. EXTRJT, an MS Branch letter (note 6, p.205). JNTIME over 30 days, a
    sanction letter (note 3, p.203). CHNGSPR, the sanction letter (note 4, p.202).
  - Promotion: SUBPROM, the promotion sanction letter (note 2, p.109).
  - Leaving: RELEASE/RETIRE/PRETIRE/RESIGN, the IHQ letter (note 1, p.39). INVALID, the IHQ/DGAFMS letter
    (notes 3 and 5, p.41).
  - Deputation: DEPUTE, the IHQ letter (note 2, p.43). REVDEP, the reversion letter (note 2, p.35).
  - Most other cases need no authority line.
- **Last Part II order no./date.** Every order must quote the officer's last Part II order (para 6.7 and
  para 7.9, p.5). The vault's latest saved order fills it on every slip. When the vault is empty, the slip
  leaves a blank for the clerk.
- **Codes whose group is not printed.** 3.28 STPTLA and 11.8 REMAROF have a blank group in Annexure II,
  in both the text dump and a fresh `pdftotext` (`-layout` and `-raw`). See Q5.

## 4. Linked orders the manual states

Group and order rules that decide which codes can share one draft:
- **G1 (para 7.1, p.5).** Group B claims (11 serials: 3.96 and 8.1-8.10) "should not be combined with"
  A/AM/C/CM. A, AM, C and CM may share one order, and the order takes A (or AM) if any occurrence is
  A/AM. The draft engine must therefore split any slip that mixes in a `[B]` code, e.g. a posting-out
  slip and the transit-accommodation claim 8.10. Para 7.1 also names a group "D", which para 3 and
  Annexure II never define.
- **G2 (para 6.4, p.4).** All occurrences of one event go in the same order, in chronological order. A
  missing or misordered occurrence gets the order rejected.
- **G3 (para 6.2, p.4).** One order per officer. **G4 (para 11, p.8):** no amendments; cancel (14.1/14.2)
  and notify afresh.

Cross-ribbon flows (only links the manual states):

| # | Flow | Manual citation |
|---|---|---|
| F1 | **Leave → Field ribbon.** On rejoining from leave, the area allowance is published again. HAA: except CL under 14 days. SCCIA: after leave, hospitalisation or courses. HAFA/CFAA/CMFA: except CL or hospitalisation under 15 days. TPTA: after an absence covering a whole calendar month; STPTRAN covers such absences | HAA note ii p.49; SCCIA note 2 p.53; field note 2 p.55; TPTA notes iii and v p.71 |
| F2 | **Posting out → stop allowances.** All location-, appointment- and unit-based allowances stop with POSOUT. Moving between field area types needs STPFIELD, and the new grant alone "will not suffice". STPHRA on POSOUT. STPTRAN on leaving the station finally. STPSFALC and STPPARA on posting out. STPSPLP on giving up the annotated appointment | POSOUT note 4 p.200; STPFIELD notes 2-3 p.56; STPHRA note 1 p.73; TPTA note v p.71; p.64; STPPARA note 4 p.62; STPSPLP note vi p.82 |
| F3 | **Posting in → grants on entering the area** (HAA, SCCIA, field, Siachen, ISDA, TPTA). TOS date = SOS date of the previous unit | p.49, 53, 55; GTDSICHA note 1 p.51; ISDA note i p.96; TPTA note iii p.71; POSIN note 2 p.200 |
| F4 | **Posting to field → SPR within 4 months.** Without it, HRA and family/baggage/car move claims are lost for that tenure. HRA for the SPR then rests on it | SPR notes 5.1-5.3 p.201; HRA certificates pp.75-80 |
| F5 | **Leave ↔ joining time.** Joining time is taken last, after AL and furlough, and never combined with CL or used after study leave. EXTRJT quotes the JNTIME order | JNTIME notes 2 and 4 p.203; EXTRJT note 4 p.204 |
| F6 | **Promotion → pay fixation option.** SUBPROM is "followed by" OPTFXDNI or OPTFXDOP with the option certificate, within 3 months | SUBPROM note 1 p.109, notes 3-5 p.110; p.112 note 1 |
| F7 | **Courses.** REJCRS quotes the PROCRS order; TOSCRS quotes SOSCRS/SOSCRS1. Para, Para Reserve and SF allowances stop on going on a course and may be re-granted on rejoining. Specialist allowance stops on study leave. Flying allowance is not paid during study leave | REJCRS cols 8-9 p.196; TOSCRS p.198; para note 5 p.62; note vi p.82; note 4 p.58 |
| F8 | **Hospital → Leave.** HOSADM's order no. is quoted by 6.2-6.6 and 6.8. Discharge, RPDUHSP and regularisation go in one order. REGAL is followed by GTDALHOS/GTDSLHOS/HOSFURPY (60 days AL + 120 days SL). AL adjusted for hospitalisation counts toward the 60-day AL cap | note 2 p.142; note 3 p.147; note 5 p.148; REGALMS note 2 p.149; GTDAL note 2 p.115 |
| F9 | **Exit.** LAPDISPF goes in the same order as the exit and comes before it, even if no device was issued. ENCASH on retirement/release needs the year-wise break-up. Terminal leave and AL are published together. TPTA is never granted with exits, UN/foreign deputation or BCA. No encashment with COMPRETD/TERMN/REMOVE | LAPDISPF note 1 p.221; ENCASH notes 1-2 p.141; note 10 p.119; TPTA note vii p.71; p.174 note 3, p.175 note 3, p.176 note 2 |
| F10 | **Deputation ↔ return.** REVDEP needs DEPUTE/DEPUOTHR to have been notified. BCA stops when the officer is SOS or out of Bhutan for more than 14 days | REVDEP note 1 p.35; STPBCA note i p.69 |
| F11 | **Family.** MARSEROF follows and quotes the MARRIAGE order. NOKCHNG is only for remarriage after the spouse's death or divorce | MARSEROF note 2 p.183; NOKCHNG note p.184 |
| F12 | **Same-ribbon pairs worth showing:** grant ↔ stop (ration: "GTDRSHNA would invariably be followed by" STPRSHNA, note 4 p.98); flying grant and stop together, published monthly (notes 1-2 p.58); MISSING → REMISSIN quotes it (notes 2-3 p.36) | as cited |

The Leave-internal links (grant → rejoin/extend/recall; REJOINAL, VOLREJ and RECALL quote the grant) are
already in the decision record.

## 5. Coverage check

The script `layout.py` holds the layout and checks it against `master.tsv`, a hand-built
serial→code→group list:

```
serials in Annexure II: 303 | homed: 303 | missing: [] | errors: []   (no double homes, no unknown
ribbons 10 cases 48                                                    serials, ≤8 cases, titles ≤7 words)
  leave 6 cases/30 · moves 4/23 · field 6/34 · career 4/31 · exit 4/20 · family 6/33 · health 4/18
  role 3/34 · depu 4/28 · records 7/52
groups: A 204, AM 23, B 11, C 53, CM 10, not printed 2 (3.28, 11.8)
per chapter: 1:19 2:16 3:102 4:5 5:30 6:15 7:13 8:10 9:20 10:10 11:21 12:6 13:34 14:2 = 303
```

How `master.tsv` was verified:
1. A regex over the Annexure II index (text lines 481-1222) found 303 serial rows with no gaps in any
   chapter.
2. Every code in `master.tsv` was matched as a whole word in the index text, and again in the Annexure
   III detail tables. There were 13 misses, all caused by column wrap; each was inspected by hand at its
   detail row (e.g. `TADISCH`, `MARSERO`, `ALOTPRN`).
3. The list was compared with the flawed JSON. The 14 known missing serials were confirmed, along with
   the 4 truncated variants (3.27, 3.51, 3.90, 3.94) and 10.9. Nothing else differed.
4. The blank groups of 3.28 and 11.8 were re-read in a fresh `pdftotext` of the PDF.

**Unresolved:** none for homing. Not verified: each group letter was read from the text and not checked
row by row against the PDF image.

## 6. Open questions for the owner

1. **Part II limits.** Accept a minimum of 2 codes per case (the Guide uses 4)? *Recommend yes:* a
   minimum of 4 would force unnatural merges such as cancellation with ID details.
2. **One release for Moves + Field?** *Recommend yes* (see section 2). If too big, ship R&H, field, HAA
   and SCCIA with Moves first.
3. **Leave release and F1.** A REJOINAL in a field, HAA or CI area needs the allowance re-granted, but the
   Field ribbon is not built yet. Show an "also publish" link to the code page, with its page citation
   and no draft? *Recommend yes:* the link is manual-stated, and without it the Leave slip silently
   misses a money order.
4. **Which tiles appear before their ribbon is built?** *Recommend:* tiles only for built ribbons, plus
   ribbon 10 (lookup by design). Search reaches all 303 code pages from release 1.
5. **Blank groups (3.28 STPTLA, 11.8 REMAROF).** *Recommend* showing "Group not printed in ODP-2026" and
   not inferring A or C. Group decides the G1 split, and the content is manual-only.
6. **Cancellation (14.1/14.2).** Home it in Records but offer it from any vault order ("this order was
   wrong → cancel and re-draft", para 11 p.8)? Ship with release 2 or with Leave? *Recommend release 2:*
   it is small, and a wrong leave order is the commonest case.
7. **Awards (Ch 7) and TA (Ch 10).** *Recommend:* Ch 10 stays lookup-only (unit-driven), and Ch 7 drafts
   only 7.9 GALASSPP (monetary allowance) at first, with the other 12 as lookup.
8. **Stale cross-references inside notes.** Some notes use old serial numbers:
   - TERLEV note 10 (p.119) says "5.1 and 5.8"; 5.8 is now SPMTRNLV.
   - CHNCORPS notes (p.207) say "13.9" and "13.12" for CHNGRELI and CHNCORPS.
   - The language note (p.90) says "3.86 to 3.88".
   - The SECNDMNT note (p.41) says "2.10".
   - The ration note (p.100) says "3.107(c)".

   *Recommend:* cite the note's page, map it to the current code by its occurrence name, and list each
   one in the review pack for the owner to confirm.
9. **Stop allowances on proceeding on leave?** The manual states the re-grant on rejoining (F1), and STPFIELD
   note 2 says "on all occasions when the Officer leaves" the area. It does not say whether going on
   leave counts. *Recommend:* claim only the re-grant, and do not add a stop on proceeding on leave
   unless the owner reads the manual otherwise.
10. **Correct the decision record:** 303 serials, and 9.7 `WTHDPAY` exists (p.171).

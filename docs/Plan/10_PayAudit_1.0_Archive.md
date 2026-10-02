# `feature/pay_audit_1.0` — archive note

Decision date: 2026-10-01. Base for Pay Audit: `feature/pay-audit` (see `09_PayAudit_PhasePlan.md`).

## Where it lives

- Branch `feature/pay_audit_1.0` is on the remote, unmerged and undeleted.
- Annotated tag `archive/pay-audit-pcdao-1.0` marks its tip.
- No new commits are made on it. Revive or cherry-pick from the tag.

## Why it was dropped

It added a separate `pcdao/` engine (situation tiles, an "Unclaimed ₹" counter, 18 % penal-interest hazards).
An audit found confirmed false rupee figures:

- ₹22,13,800 of fake back-dues.
- ₹3,37,200 of "HRA unclaimed".
- DA computed on Basic Pay only.
- Pre-7th Pay Commission payslips audited with 7th CPC rules.

This breaks the Pay Audit principles: a finding is shown only when the payslips prove it and it cites a verified
authority; one false "you are owed ₹X" costs more trust than ten missed findings.
`feature/pay-audit` extends the existing `DeterministicIntelligenceEngine` instead and has 0 false findings on the
corpus (`PayAuditCorpusPrecisionTest`) and on the developer's real months (n = 1).

## What was ported (rewritten for the timeline/evidence model)

- Dashboard discovery banner (`DashboardAuditBannerCard`), pointing at `Screen.PayAudit`.
- "Audit this month" card on the payslip detail screen (`ReplicaAuditActionCard`).
- First-open orientation sheet and its `OnboardingManager`/`OnboardingStorage` flag (Android and iOS actuals).
- The month-picker idea (`AuditMonthSelector`), rebuilt to list only months that have a payslip.

Copy was rewritten: no tiles, no "unclaimed ₹". Premium gating is unchanged (`ANOMALY_DETECTION`, findings only).

## What was not ported

Situation tiles, mission presets, the "Unclaimed ₹" counter, 18 % penal hazards, collision auditors, the CEA
multi-month check (needs user input), the forfeiture tracker, and the CI "intelligence watchdog" workflow. DSOP ₹5
lakh already exists on `feature/pay-audit` (`DsopRoomCalculator`, `DsopComplianceAuditor`).

A piece may be cherry-picked later only if it passes `PayAuditCorpusPrecisionTest`.

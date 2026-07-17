# cloud-itonami-isco-3353

Open Occupation Blueprint for **ISCO-08 3353**: Government Social Benefits Officials.

This repository designs a forkable OSS business for a government social-benefits case-documentation and logistics-coordination practice: an intake and case-logistics robot manages application records, caseworker-appointment scheduling and office-supply coordination under a governor-gated actor — and structurally **never** approves, denies or terminates a benefit itself.

## This actor does not decide who receives benefits

Government Social Benefits Officials determine eligibility for and approve/deny social benefits — decisions with direct, severe impact on vulnerable people's basic subsistence. **This actor is a documentation/logistics-coordination robot ONLY.** It has NO op, anywhere in its allowlist, that resembles approving a benefit, denying a benefit, or terminating an existing benefit. These are **structurally absent from the closed op-allowlist entirely**, not merely gated behind escalation. Any observation the robot logs that suggests an eligibility decision is needed is surfaced ONLY via an always-escalating `:flag-eligibility-review` op that a human caseworker reviews and decides. This mirrors the Wave4 person-facing-service safety guardrail (ADR-2607152500): decisions directly touching a person's safety/dignity/subsistence always exclude the closed op allowlist and always escalate.

**Maturity: `:implemented`.** `src/socialbenefits/` implements the
`SocialBenefitsActor` as a `langgraph.graph/state-graph`
(`socialbenefits.actor`) wired to a `Case Documentation Advisor`
(`socialbenefits.advisor`) and an independent `SocialBenefitsGovernor`
(`socialbenefits.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 25 tests / 60 assertions green (`clojure -M:test`).

HARD invariants (always hold, never overridable): applicant provenance
(a proposal referencing an applicant must resolve to an independently
registered AND verified applicant record), office provenance (same,
for any proposal referencing an office), no-actuation (`:effect` must
be `:propose`), a closed four-op proposal allowlist (any op outside it
— including anything that would approve, deny or terminate a benefit —
is a permanent HARD block, because no such op exists in the allowlist
to begin with), and a content-based scope-exclusion check: any
proposal whose rationale names a finalization/execution action for a
benefit approval, denial or termination is a permanent HARD block,
independent of and in addition to the op-allowlist check. This actor
**never** exercises, simulates exercising, or proposes exercising any
benefits-eligibility-determination authority — it only documents
applications and coordinates caseworker/office logistics.

Always-escalate (human sign-off regardless of confidence, mapping this
repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)): `:flag-
eligibility-review` (surfacing an application that needs human
caseworker eligibility review — new application, status-change
trigger, appeal — always requires human review; never auto-resolved,
never in any phase's auto-commit set — this is the ONLY channel by
which an eligibility-adjacent observation may be surfaced) and any
`:coordinate-supply-order` above the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical/administrative domain work**. Here an intake and case-logistics robot performs application data entry, caseworker-appointment scheduling and office-supply coordination under an actor that proposes actions and an independent **SocialBenefits Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as flagging an application for eligibility review, or an above-threshold supply order) require human sign-off — and no action in this actor's closed op allowlist can ever approve, deny or terminate a benefit.

## Core Contract

```text
benefits application intake + caseworker calendar + office supply policy
        |
        v
Case Documentation Advisor -> SocialBenefits Governor -> log record/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, approve,
deny or terminate a benefit, suppress an operating record, or disclose
sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3353`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.

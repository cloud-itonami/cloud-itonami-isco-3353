# Operator Guide

## First Deployment

1. Define the operator's service area and intake process.
2. Define consent and purpose categories.
3. Run synthetic operating cases.
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions —
   including every `:flag-eligibility-review` and every above-threshold
   `:coordinate-supply-order`.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path
- provenance for all operating records
- human review for high-risk cases
- audit export for all gated actions

## No Eligibility-Determination Authority

This actor is a documentation/logistics-coordination robot ONLY. Operators
MUST NOT configure, extend or fork this actor to add an op that approves,
denies or terminates a benefit, or that otherwise exercises benefits-
eligibility-determination authority. Any such change removes the structural
guarantee this repository is built around and voids certification (see
[`GOVERNANCE.md`](../GOVERNANCE.md)). Every eligibility-adjacent observation
must route through `:flag-eligibility-review` to a human caseworker.

## Certification

Certified operators must prove that the governor gates every safety-critical
robot action, that safety-critical risks escalate to humans, and that no
build of this actor has ever added an op resembling a benefit approval,
denial or termination to the closed allowlist.

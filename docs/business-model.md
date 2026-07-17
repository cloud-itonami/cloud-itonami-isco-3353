# Business Model: Government Social Benefits Documentation and Logistics-Coordination Practice

## Classification

- Repository: `cloud-itonami-isco-3353`
- ISCO-08: `3353`
- Occupation: Government Social Benefits Officials
- Social impact: benefits-access-continuity, caseworker-capacity, audit-transparency

## Customer

- government benefits agencies / offices
- benefits applicants (indirect beneficiaries — never the actor's decision subject)

## Offer

- benefits-application intake and status data entry
- caseworker-appointment scheduling coordination
- eligibility-review flagging (surfacing applications for human caseworker review)
- office-equipment procurement coordination

## Revenue

- monthly agency retainer
- per-case documentation-coordination fee

## Trust Controls

- **no eligibility-determination authority exists in this actor.** The closed
  proposal-op allowlist never includes an op that could approve, deny or
  terminate a benefit — such capabilities are structurally absent, not merely
  gated.
- no proposal commits or escalates without an independently registered AND
  verified applicant/office record
- `:flag-eligibility-review` always requires human caseworker sign-off, never
  auto-resolved — this is the only channel by which an eligibility-adjacent
  observation may be surfaced
- office-equipment supply orders above the registered cost threshold always
  require human sign-off
- application-documentation and logistics-coordination records are auditable,
  not editable

# Contributing

`cloud-itonami-isco-3353` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
kbb -M:dev:test
kbb -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real applicant, caseworker or operator data, credentials or
  operating documents.
- Keep production writes and disclosures behind SocialBenefits Governor.
- **Never add an op that approves, denies or terminates a benefit, or that
  otherwise exercises benefits-eligibility-determination authority.** This
  actor's closed proposal-op allowlist is a hard scope boundary, not a
  starting point to extend. Any PR that proposes such an op will be rejected.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates

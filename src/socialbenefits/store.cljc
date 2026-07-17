(ns socialbenefits.store
  "SSoT for the ISCO-08 3353 government social benefits officials
  documentation/logistics-coordination actor (itonami actor pattern,
  ADR-2607121000 / CLAUDE.md Actors section; README's 'Robotics
  premise' — an intake and case-logistics robot performs application
  data entry, appointment scheduling and office-supply coordination
  under this advisor/governor pair, which never dispatches hardware
  itself and NEVER exercises, simulates exercising, or proposes
  exercising any benefits-eligibility-determination authority — this
  actor cannot approve, deny or terminate a benefit; every such
  decision is a permanently out-of-scope, structurally absent
  capability, always surfaced to a human caseworker via
  `:flag-eligibility-review` instead). Modeled on
  cloud-itonami-isco-3333's placementops.store (closed op allowlist +
  independently-verified provenance), itself modeled on
  cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    applicant — a registered benefits applicant {:applicant-id :name
                :verified? boolean}. Independently registered/verified
                identity, never trusted from the proposal alone. This
                actor never determines this applicant's eligibility —
                it only logs, schedules and flags for human review.
    office    — a registered benefits office/agency location
                {:office-id :name :verified? boolean}. Independently
                registered/verified, never trusted from the proposal
                alone. Office-equipment supply orders are coordinated
                against a registered office record.
    record    — a committed operating record (application-record log,
                caseworker-appointment scheduling proposal,
                eligibility-review flag, or supply-order coordination
                proposal) — written ONLY via commit-record!. A
                committed record is NEVER a benefit approval, denial or
                termination — this actor documents and coordinates
                logistics, it does not decide who receives benefits.
    ledger    — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (applicant [s applicant-id])
  (office [s office-id])
  (records-of [s applicant-id])
  (ledger [s])
  (register-applicant! [s a])
  (register-office! [s o])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (applicant [_ applicant-id] (get-in @a [:applicants applicant-id]))
  (office [_ office-id] (get-in @a [:offices office-id]))
  (records-of [_ applicant-id] (filter #(= applicant-id (:applicant-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-applicant! [s ap]
    (swap! a assoc-in [:applicants (:applicant-id ap)] ap) s)
  (register-office! [s o]
    (swap! a assoc-in [:offices (:office-id o)] o) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:applicants {} :offices {} :records [] :ledger []}
                                   seed)))))

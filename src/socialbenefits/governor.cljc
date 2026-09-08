(ns socialbenefits.governor
  "SocialBenefitsGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating every
  documentation/logistics-coordination operation an advisor may
  propose. The governor never dispatches hardware itself and NEVER
  lets a proposal exercise, simulate exercising, or propose exercising
  ANY benefits-eligibility-determination authority — approving,
  denying or terminating a benefit is permanently out of scope for
  this actor, not merely gated behind escalation. This mirrors the
  Wave4 person-facing-service safety guardrail (ADR-2607152500):
  decisions directly touching a person's safety/dignity/subsistence
  always exclude the closed op allowlist and always escalate. Modeled
  on cloud-itonami-isco-3333's placementops.governor, with the same
  closed proposal-op allowlist + content-based scope-exclusion shape,
  adapted to this vertical's benefits-eligibility guardrail.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. applicant provenance    — the applicant record must be
                                independently registered AND verified
                                before any proposal referencing it can
                                commit or escalate. Never trusts the
                                proposal's own claim.
    2. office provenance       — same, for any proposal referencing an
                                office (office-equipment supply-order
                                coordination).
    3. no-actuation            — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never itself performs an eligibility
                                determination; it only gates what the
                                advisor may commit).
    4. closed op allowlist     — the proposal's :op must be one of the
                                four ops this actor is scoped to
                                (`closed-op-allowlist` below). This is
                                the STRUCTURAL guarantee: no op that
                                resembles approving, denying or
                                terminating a benefit exists anywhere
                                in this allowlist — such a proposal
                                cannot even reach a check, let alone
                                pass one. Any :op outside the allowlist
                                is a HARD, PERMANENT block.
    5. scope exclusion         — independent, DEFENSE-IN-DEPTH layer on
                                top of #4: even for an otherwise-allowed
                                op, any proposal whose rationale/content
                                names a finalization/execution action
                                for a benefit approval, denial or
                                termination (`scope-excluded-terms`
                                below) is a HARD, PERMANENT block,
                                evaluated unconditionally on content.
                                This actor never exercises eligibility-
                                determination authority — it only
                                documents applications and coordinates
                                caseworker logistics.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off — these
  are :high/:safety-critical regardless of confidence):
    6. :op :flag-eligibility-review (surfacing an application that
                                needs human caseworker eligibility
                                review — new application, status-change
                                trigger, appeal — ALWAYS requires human
                                review; it is never auto-resolved and
                                never appears in any phase's auto-commit
                                set; this is the ONLY path by which an
                                eligibility-adjacent observation may be
                                surfaced, and it always escalates
                                immediately).
    7. an above-threshold :coordinate-supply-order (office-equipment
                                procurement above
                                `supply-cost-escalation-threshold`
                                always needs human sign-off, regardless
                                of confidence).
    8. low confidence (< `confidence-floor`)."
  (:require [kotoba.lang.text :as str]
            [socialbenefits.store :as store]))

(def confidence-floor 0.6)

;; Office-equipment supply orders at or below this estimated cost may be
;; auto-commit-eligible (subject to confidence); above it, ALWAYS
;; escalates to a human regardless of confidence.
(def supply-cost-escalation-threshold 2000)

;; The closed proposal-op allowlist. This governor NEVER allows any op
;; outside this set to commit or even escalate — an op outside this
;; set is a HARD, permanent block (see `hard-violations` :op-not-allowed
;; below), not merely un-auto-committable. This is a documentation/
;; logistics-coordination robot ONLY: it has NO op, anywhere in this
;; allowlist, that resembles approving a benefit, denying a benefit, or
;; terminating an existing benefit. Those capabilities are
;; structurally absent, not gated.
(def closed-op-allowlist
  #{:log-application-record :schedule-caseworker-appointment
    :flag-eligibility-review :coordinate-supply-order})

;; :flag-eligibility-review always escalates to a human — never
;; auto-commit-eligible at any phase. It is the ONLY channel through
;; which an eligibility-adjacent observation may be surfaced.
(def ^:private always-escalate-ops #{:flag-eligibility-review})

;; Scope-exclusion terms, phrased as the FINALIZATION/EXECUTION ACTION
;; (never a bare noun like "benefit" or "eligibility") — a known
;; self-tripping bug class in this fleet: a bare-noun term list can
;; accidentally match inside the mock advisor's own default rationale
;; text for a legitimate proposal (this advisor's default rationale is
;; "documented <op> for applicant <id>[ and office <id>]", which never
;; contains any of these full action phrases). See
;; `socialbenefits.governor-test/
;; default-mock-advisor-proposals-never-self-trip-scope-exclusion`.
(def scope-excluded-terms
  ["approve the benefit claim" "approved the benefit claim"
   "deny the benefit claim" "denied the benefit claim"
   "reject the benefit claim" "rejected the benefit claim"
   "terminate the existing benefit" "terminated the existing benefit"
   "confirm the benefit approval" "confirmed the benefit approval"
   "confirm the benefit denial" "confirmed the benefit denial"
   "execute the benefit denial" "executed the benefit denial"
   "execute the benefit termination" "executed the benefit termination"
   "finalize the eligibility decision" "finalized the eligibility decision"
   "issue the final eligibility determination" "issued the final eligibility determination"
   "cut off the existing benefit" "cut off the applicant's existing benefit"
   "給付を承認した" "給付の承認を確定した"
   "給付を却下した" "給付の却下を確定した"
   "給付を打ち切った" "受給資格を確定した"])

(defn out-of-scope?
  "True if any string field on `proposal` (currently just :rationale)
  contains a scope-excluded finalization/execution phrase for a
  benefit approval, denial or termination."
  [proposal]
  (let [text (str (:rationale proposal))]
    (boolean (some #(str/includes? text %) scope-excluded-terms))))

(defn- hard-violations [{:keys [proposal]} applicant-record office-record]
  (let [{:keys [op applicant-id office-id]} proposal]
    (cond-> []
      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は受給資格判定を直接実行しない）"})

      (not (contains? closed-op-allowlist op))
      (conj {:rule :op-not-allowed :detail "closed allowlist 外の op（給付の承認/却下/打ち切りの直接実行を含む一切の確定は許可されない）"})

      (and applicant-id (nil? applicant-record))
      (conj {:rule :unknown-applicant :detail "未登録 applicant への提案は不可"})

      (and applicant-id applicant-record (not (:verified? applicant-record)))
      (conj {:rule :applicant-unverified :detail "未検証 applicant への提案は不可（登録のみでは不十分）"})

      (and office-id (nil? office-record))
      (conj {:rule :unknown-office :detail "未登録 office への提案は不可"})

      (and office-id office-record (not (:verified? office-record)))
      (conj {:rule :office-unverified :detail "未検証 office への提案は不可（登録のみでは不十分）"})

      (out-of-scope? proposal)
      (conj {:rule :scope-excluded
             :detail "給付の承認/却下/打ち切りを直接確定する提案は恒久的に許可されない（このactorは文書化とロジスティクス調整のみを行う）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `socialbenefits.store/Store`. Pure — never
  mutates the store, never approves, denies or terminates a benefit."
  [_request _context proposal store]
  (let [applicant-record (some->> (:applicant-id proposal) (store/applicant store))
        office-record (some->> (:office-id proposal) (store/office store))
        hard (hard-violations {:proposal proposal} applicant-record office-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        over-threshold-supply-order?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:estimated-cost proposal))
             (> (:estimated-cost proposal) supply-cost-escalation-threshold))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-threshold-supply-order?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-threshold-supply-order?))}))

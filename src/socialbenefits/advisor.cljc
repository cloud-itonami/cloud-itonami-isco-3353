(ns socialbenefits.advisor
  "Case Documentation Advisor — the advisor named in this repository's
  README, proposing a benefits-application documentation/logistics
  operation (log an application record, schedule a caseworker
  appointment, flag an application for human eligibility review, or
  coordinate an office-equipment supply order) from an intake batch,
  caseworker calendar and office supply policy. Swappable mock/llm;
  the advisor ONLY proposes — `socialbenefits.governor` checks
  applicant/office verification and scope independently and always
  escalates eligibility-review flags and above-threshold supply
  orders. Modeled on cloud-itonami-isco-3333's advisor.

  This advisor NEVER proposes approving, denying or terminating a
  benefit — no such op exists anywhere in the closed allowlist below
  (`socialbenefits.governor/closed-op-allowlist`), and the rationale
  text this advisor emits never uses a finalization/execution phrase
  for an eligibility decision
  (`socialbenefits.governor/scope-excluded-terms`), so the advisor's
  own DEFAULT proposals never self-trip the governor's scope-exclusion
  check (see `socialbenefits.governor-test/
  default-mock-advisor-proposals-never-self-trip-scope-exclusion`).
  Any observation suggesting an eligibility decision is needed is
  surfaced ONLY via `:flag-eligibility-review`, which always escalates
  to a human caseworker and never auto-commits.

  A proposal: {:op :log-application-record|:schedule-caseworker-appointment|
               :flag-eligibility-review|:coordinate-supply-order
               :effect :propose :applicant-id str :office-id str
               :estimated-cost number :stake kw :confidence n
               :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake applicant-id office-id estimated-cost] :as request}]
  {:op op
   :effect :propose
   :applicant-id applicant-id
   :office-id office-id
   :estimated-cost estimated-cost
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "documented " (name op) " for applicant " applicant-id
                   (when office-id (str " and office " office-id)))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a government social-benefits case-documentation and
   logistics-coordination advisor. Given a request, propose an :op,
   the :applicant-id and (when relevant) :office-id/:estimated-cost, an
   honest :confidence and a :stake. You are a documentation and
   logistics-coordination robot ONLY — you help process applications,
   log case records, and schedule caseworker appointments. Never
   propose an op outside the closed four-op allowlist, and NEVER
   propose approving, denying or terminating a benefit, or otherwise
   determining an applicant's eligibility — that authority does not
   exist for you; any indication that an eligibility decision is
   needed must be surfaced only via :flag-eligibility-review, which
   always requires human caseworker review regardless of confidence.
   The governor independently verifies applicant/office registration
   and always escalates eligibility-review flags and above-threshold
   supply orders to a human.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))

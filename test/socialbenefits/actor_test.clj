(ns socialbenefits.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [socialbenefits.actor :as actor]
            [socialbenefits.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-applicant! st {:applicant-id "AP-1" :name "Kobo Yamada" :verified? true})
    (store/register-office! st {:office-id "OFF-1" :name "Kobo District Benefits Office" :verified? true})
    st))

(deftest commits-a-verified-applicant-log-record
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :log-application-record :stake :low :applicant-id "AP-1"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "AP-1"))))))

(deftest commits-a-verified-caseworker-appointment-scheduling
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :schedule-caseworker-appointment :stake :low :applicant-id "AP-1"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/records-of st "AP-1"))))))

(deftest commits-an-at-or-below-threshold-supply-order
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :coordinate-supply-order :stake :low :office-id "OFF-1" :estimated-cost 500}
        result (actor/run-request! graph request {} "thread-3")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))))

(deftest holds-an-unverified-applicant-proposal
  (let [st (fresh-store)]
    (store/register-applicant! st {:applicant-id "AP-2" :name "Unverified" :verified? false})
    (let [graph (actor/build-graph {:store st})
          request {:op :log-application-record :stake :low :applicant-id "AP-2"}
          result (actor/run-request! graph request {} "thread-4")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "AP-2"))))))

(deftest holds-an-op-outside-the-closed-allowlist
  (testing "no path through this actor can approve, deny or terminate a benefit"
    (let [st (fresh-store)
          graph (actor/build-graph {:store st})
          request {:op :approve-benefit-claim :stake :low :applicant-id "AP-1"}
          result (actor/run-request! graph request {} "thread-5")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "AP-1"))))))

(deftest interrupts-then-approves-eligibility-review-flag-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :flag-eligibility-review :stake :low :applicant-id "AP-1"}
        interrupted (actor/run-request! graph request {} "thread-6")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "AP-1")))
    (let [resumed (actor/approve! graph "thread-6")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "AP-1")))))))

(deftest interrupts-then-approves-above-threshold-supply-order-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:op :coordinate-supply-order :stake :low :office-id "OFF-1" :estimated-cost 5000}
        interrupted (actor/run-request! graph request {} "thread-7")]
    (is (= :interrupted (:status interrupted)))
    (let [resumed (actor/approve! graph "thread-7")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record]))))))

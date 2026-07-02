(ns reception-contact.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [reception-contact.store :as store]
            [reception-contact.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-caller! st {:caller-id "caller-1" :is-verified? true})
    (store/register-caller! st {:caller-id "caller-2" :is-verified? false})
    st))

(deftest proceeds-on-clean-general-inquiry
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :inquiry :caller-id "caller-1" :category :general
                   :safety-class :low :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-caller
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :inquiry :caller-id "no-such-caller" :category :general
                   :safety-class :low :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-caller (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :inquiry :caller-id "caller-1" :category :general
                   :safety-class :low :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest holds-on-disclosure-to-unverified-caller
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :disclosure :caller-id "caller-2" :data-category :general
                   :safety-class :low :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :verified-disclosure-only (:rule %)) (:violations result)))))

(deftest proceeds-on-disclosure-to-verified-caller
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :disclosure :caller-id "caller-1" :data-category :general
                   :safety-class :low :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest emergency-inquiry-always-escalates
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :inquiry :caller-id "caller-1" :category :emergency
                   :safety-class :none :effect :propose :confidence 1.0}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :emergency-inquiry (:reason result)))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :inquiry :caller-id "caller-1" :category :general
                   :safety-class :none :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-inquiry! st {:inquiry-id "i1" :caller-id "caller-1" :category :general})
    (store/record-disclosure! st {:disclosure-id "d1" :caller-id "caller-1" :data-category :general})
    (is (= 1 (count (store/inquiries-of st "caller-1"))))
    (is (= 1 (count (store/disclosures-of st "caller-1"))))))

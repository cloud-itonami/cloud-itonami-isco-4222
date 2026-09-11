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

(deftest a-proposal-without-confidence-does-not-proceed
  (testing "確信度を言っていない提案は、確信していると言っていないので auto-proceed
            させない。この既定は 2026-07-30 まで 1.0 で、:confidence を持たない提案が
            :proceed していた（ADR-2607309100）。fleet の boolean 方言 346 件はすべて
            0.0 既定で、うち isco-5419 はそれを明示的にテストしている。"
    (let [st (fresh-store)
          env (governor/env-for-store st)
          proposal {:kind :inquiry :caller-id "caller-1" :category :general :safety-class :low :effect :propose}
          result (governor/assess env proposal)]
      (is (= 0.0 (:confidence result))
          "欠落した :confidence は 0.0 であって 1.0 ではない")
      (is (not= :proceed (:decision result))
          "確信度不明の提案が自動で通ってはならない"))))

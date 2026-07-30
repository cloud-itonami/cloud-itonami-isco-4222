(ns reception-contact.governor
  "ReceptionContactGovernor — the independent safety/traceability layer
  for the ISCO-08 4222 independent reception-and-contact actor. The
  Contact Advisor proposes actions (inquiry, disclosure); it has no
  notion of caller provenance, verification state, or emergency risk, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD — the itonami-actor pattern (independent Governor gates a
  proposing actor) applied to this occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. A disclosure to an unverified
  caller is HARD-blocked unconditionally, and an `:emergency` inquiry
  ALWAYS escalates to human sign-off — neither can ever be auto-approved.

  HARD invariants for :reception-contact/propose:
    1. Caller provenance        — an inquiry or disclosure must
       reference a registered caller.
    2. No-actuation              — the proposal must not directly mutate
       an inquiry/disclosure record outside the record-inquiry!/
       record-disclosure! path (effect must be :propose, never a raw
       store write).
    3. Verified-disclosure-only  — a disclosure to a caller whose
       `is-verified?` is not true is unconditionally HELD, regardless of
       safety-class or confidence.
  SOFT:
    4. Emergency inquiries always escalate to human sign-off (no
       autonomous handling of emergency-category inquiries).
    5. Confidence floor → escalate."
  (:require [reception-contact.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- hard-violations [{:keys [caller-fn]} proposal]
  (let [{:keys [kind caller-id safety-class effect]} proposal
        found-caller (caller-fn caller-id)]
    (cond-> []
      (nil? found-caller)
      (conj {:rule :no-caller :detail (str "未登録 caller " caller-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and (= kind :disclosure) found-caller (not (:is-verified? found-caller)))
      (conj {:rule :verified-disclosure-only
             :detail "is-verified? でない caller への disclosure は不可"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:caller-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 0.0)
        emergency? (and (= :inquiry (:kind proposal)) (= :emergency (:category proposal)))]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      emergency?
      {:decision :human-approval :violations [] :confidence confidence
       :reason :emergency-inquiry}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `reception-contact.store/Store` implementation."
  [store]
  {:caller-fn #(store/caller store %)})

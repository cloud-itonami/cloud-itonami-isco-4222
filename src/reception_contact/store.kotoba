(ns reception-contact.store
  "SSoT for the ISCO-08 4222 independent reception-and-contact
  sole-proprietor actor, behind a `Store` protocol so the backend is a
  swap (MemStore default ‖ a real Datomic/kotoba-server backend, per the
  itonami actor pattern).

  Domain = independent reception & contact practice:

    caller           — a registered caller (callerId, isVerified?
                       boolean)
    inquiry          — an inquiry event under a caller (inquiryId,
                       callerId, category #{:general :account
                       :emergency})
    disclosure       — a disclosure event under a caller (disclosureId,
                       callerId, dataCategory)

  The append-only records are the operating ledger: an inquiry or
  disclosure must reference a registered caller, and these records are
  never mutated in place, only appended.")

(defprotocol Store
  (caller [st caller-id])
  (inquiries-of [st caller-id])
  (disclosures-of [st caller-id])
  (register-caller! [st caller])
  (record-inquiry! [st inquiry])
  (record-disclosure! [st disclosure]))

(defrecord MemStore [state]
  Store
  (caller [_ caller-id]
    (get-in @state [:callers caller-id]))
  (inquiries-of [_ caller-id]
    (filter #(= caller-id (:caller-id %)) (:inquiries @state)))
  (disclosures-of [_ caller-id]
    (filter #(= caller-id (:caller-id %)) (:disclosures @state)))
  (register-caller! [_ caller]
    (swap! state assoc-in [:callers (:caller-id caller)] caller))
  (record-inquiry! [_ inquiry]
    (swap! state update :inquiries (fnil conj []) inquiry))
  (record-disclosure! [_ disclosure]
    (swap! state update :disclosures (fnil conj []) disclosure)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:callers {} :inquiries [] :disclosures []} seed)))))

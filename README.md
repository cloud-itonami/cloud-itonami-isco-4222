# cloud-itonami-isco-4222

Open Occupation Blueprint for **ISCO-08 4222**: Contact Centre Information Clerks.

This repository designs a forkable OSS business for an independent reception and contact-centre practice: a reception kiosk robot performs visitor check-in and call routing under a governor-gated actor, so the practice keeps its own visitor and call logs instead of renting a closed reception-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a reception kiosk robot performs visitor check-in, badge issuance and call routing under an actor that proposes
actions and an independent **Reception Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
handling sensitive visitor information, or emergency call routing) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
visitor request + routing directory + disclosure policy
        |
        v
Reception Advisor -> Reception Governor -> route/log, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `4222`). Required capabilities:

- :robotics
- :forms
- :identity
- :audit-ledger
- :bpmn

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/reception_contact/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `reception-contact.store` — `Store` protocol + `MemStore`: registered
  callers (with an `is-verified?` flag), inquiries, disclosures. An
  inquiry/disclosure can only be recorded against a registered caller
  (caller provenance).
- `reception-contact.governor` — `ReceptionContactGovernor`: `assess`
  gates a proposal against the caller env. Hard invariants force `:hold`
  (no caller, direct-write instead of `:propose`, or a disclosure to a
  caller whose `is-verified?` is not true — unconditionally, regardless
  of safety-class or confidence); `:emergency`-category inquiries always
  escalate to `:human-approval` (no autonomous emergency handling);
  low-confidence proposals also escalate.

```bash
kbb -M:test   # 8 tests, 14 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the 22nd `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112`, `-2221`, `-7126`, `-4321`, `-9312`, `-5322`,
`-8332`, `-1321`, `-3253`, `-6210`, `-5223`, `-7231`, `-8121`, `-9111`,
`-2512`, `-1120`, `-4110`, `-3213`, `-5153`, `-7411` and `-2262`
(ADR-2607012000).

## License

AGPL-3.0-or-later.

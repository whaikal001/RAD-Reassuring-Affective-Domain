# Frontend: Pre-LLM Screening UX & Integration

## Goal
Run a lightweight pre-chat gating UX that collects consent, runs the DASS-21 greeting (or short screening), and enforces backend decisions before sending messages to the LLM.

## UX Flow (high level)
- Show consent + privacy notice.
- Language selection and safety reminders.
- Present DASS-21 questions (or an agreed short-form). Allow skip only if policy permits; warn that skipping reduces safety checks.
- Submit answers to `POST /api/screening`.
- Render response:
  - `allow`: open chat UI normally.
  - `prevention`: open chat with prevention templates, show resources, and display monitoring banner.
  - `intervention`: block sending; show crisis contacts and escalate options.

## Frontend Integration Notes
- Call screening before enabling chat input. Example pseudocode:

```ts
const res = await fetch('/api/screening', { method: 'POST', body: JSON.stringify(payload) })
const { action } = await res.json()
if (action === 'allow') enableChat()
if (action === 'prevention') enableChatWithPreventionMode()
if (action === 'intervention') showInterventionModal()
```

- UI: show the DASS-21 as a multi-step form; preserve answers only as needed and send to backend over TLS.
- Accessibility: ensure keyboard navigation and screen-reader labels for each question.

## Notes for Frontend Engineers
- Locate chat bootstrap in `RadAI/Frontend/src/...` and call screening before initializing the LLM chat component.
- Provide a visible banner when the session is under monitoring and a clear path to contact human support.

---
File created to guide frontend integration of the pre-LLM gate.

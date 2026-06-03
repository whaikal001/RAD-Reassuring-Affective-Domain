# RadAI — State Charts

State machines for the RadAI (Reassuring Affective Domain) mental-health chatbot, derived from the
backend implementation. Diagrams use [Mermaid](https://mermaid.js.org/) `stateDiagram-v2` and render
on GitHub, in VS Code (Mermaid preview), and in most Markdown tools.

Source of truth:
- `ChatbotFlowEngine` / `FlowWithAIService` — per-message orchestration
- `PreLLMScreeningController` + `PreLLMScreeningInterceptor` — DASS screening gate
- `MonitoringAndScreeningService` — emotional assessment + pathway
- `ApproachSwitchPolicy` — empathy ↔ sympathy switch (the core FYP mechanic)
- `LoopManager` — conversation loop decisions

---

## 0. Combined state chart (single view)

The whole system as one per-message pipeline: **screening gate → greeting → assess → pathway →
approach (empathy/sympathy) → respond → loop**. The empathy↔sympathy hysteresis is shown as guards
on the `Approach` branches; `prev` = the mode held coming into the turn.

`signal = 0.5 × bandBaseline + 0.5 × intensity`  (band: Normal=1 … Extremely severe=9);
cutoffs `HIGH=7`, `LOW=4`.

```mermaid
stateDiagram-v2
    [*] --> Screening : user opens chat

    Screening : DASS-21 stress screening
    Screening --> Blocked  : Severe / Extremely severe (intervention)
    Screening --> Greeting : Normal / Moderate (allow) or Mild (prevention)
    Blocked --> [*] : LLM blocked + crisis resources

    Greeting : Time-based greeting (first message only)
    Greeting --> Assess
    Assess : Assess emotion, intensity 0-10, stressors, crisis
    Assess --> Pathway

    Pathway : Determine pathway
    Pathway --> Prevention   : intensity LOW
    Pathway --> Intervention : intensity MODERATE / HIGH
    Prevention   --> Approach
    Intervention --> Approach

    Approach : Empathy / Sympathy switch (signal = 0.5*band + 0.5*intensity)
    Approach --> Empathy  : first turn  OR  signal <= 4  OR  (dead-zone AND prev = Empathy)
    Approach --> Sympathy : crisis  OR  signal >= 7  OR  (dead-zone AND prev = Sympathy)

    Empathy  : EMPATHY  (feel-with, presence)
    Sympathy : SYMPATHY (step-in guidance)
    Empathy  --> Respond
    Sympathy --> Respond

    Respond : Generate main content + strategies (AI-first)
    Respond --> Loop

    Loop : LoopManager decision (max 15 cycles)
    Loop --> Assess       : CONTINUE / SWITCH_APPROACH
    Loop --> Intervention : ESCALATE_PATHWAY (approach reset to EMPATHY)
    Loop --> CrisisEsc    : ESCALATE_CRISIS (helpline, safety = critical)
    CrisisEsc --> Assess  : continue with support
    Loop --> [*] : EXIT_LOOP (stable / improved / 30+ min)
    Loop --> [*] : ESCALATE_PROFESSIONAL (max cycles)
```

The sections below break this single chart into its individual machines for detail.

---

## 1. Top-level session lifecycle

```mermaid
stateDiagram-v2
    [*] --> DassScreening : user opens chat

    DassScreening : DASS-21 stress screening
    DassScreening --> Blocked     : Severe / Extremely severe\n(action = intervention)
    DassScreening --> Conversation : Normal / Moderate (allow)\nor Mild (prevention)

    Blocked : LLM access blocked (anonymous users)\nshow crisis resources / helpline
    Blocked --> [*]

    Conversation --> [*] : EXIT_LOOP\n(stable / improved / time-up)
    Conversation --> [*] : ESCALATE_PROFESSIONAL\n(max cycles reached)

    state Conversation {
        [*] --> Greeting : first message (cycleCount == 0)
        Greeting --> Assess
        [*] --> Assess  : later messages

        Assess : Assess emotional state\n(emotion, intensity 0-10, stressors, crisis)
        Assess --> Pathway
        Pathway --> Approach
        Approach --> Respond : main content + strategies (AI-first)
        Respond --> Loop : LoopManager decision
        Loop --> Assess : CONTINUE / SWITCH_APPROACH / ESCALATE_PATHWAY / ESCALATE_CRISIS
    }
```

> The screening gate is enforced for **anonymous** users by `PreLLMScreeningInterceptor`
> (`action = intervention` → HTTP 403). Registered/authenticated users proceed regardless.

---

## 2. DASS screening gate

Stress subscale score → band → action (`PreLLMScreeningController`).

```mermaid
stateDiagram-v2
    [*] --> Score : sum(dass21_answers)

    Score --> Normal           : score 0-7
    Score --> Mild             : score 8-9
    Score --> Moderate         : score 10-12
    Score --> Severe           : score 13-16
    Score --> ExtremelySevere  : score 17+

    Normal          --> Allow         : action = allow
    Moderate        --> Allow         : action = allow
    Mild            --> Prevention    : action = prevention
    Severe          --> Intervention  : action = intervention
    ExtremelySevere --> Intervention  : action = intervention

    Allow        --> [*] : proceed to chat
    Prevention   --> [*] : proceed + preventive guidance
    Intervention --> [*] : block + crisis resources
```

---

## 3. Empathy ↔ Sympathy switch  *(core FYP mechanic)*

`ApproachSwitchPolicy`. Direction: **HIGH** stress/intensity → **SYMPATHY** (step in, guide);
**LOW** → **EMPATHY** (gentle presence). Hysteresis (two cutoffs + dead-zone) prevents flip-flopping.

**Signal** (0–10, "both combined"):
`signal = 0.5 × bandBaseline + 0.5 × messageIntensity`
where `bandBaseline`: Normal=1, Mild=3, Moderate=5, Severe=7, Extremely severe=9
(if the DASS band is unknown, `signal = messageIntensity` alone).

**Cutoffs:** `HIGH_CUT = 7.0`, `LOW_CUT = 4.0`.

```mermaid
stateDiagram-v2
    [*] --> EMPATHY : session start\n(first turn always empathy)

    EMPATHY --> SYMPATHY : signal >= 7  (HIGH cut)
    EMPATHY --> EMPATHY  : signal <= 4  OR  dead-zone (between cuts)

    SYMPATHY --> EMPATHY  : signal <= 4  (LOW cut)
    SYMPATHY --> SYMPATHY : signal >= 7  OR  dead-zone (between cuts)

    EMPATHY  --> SYMPATHY : crisis / suicidal ideation (forced)
    SYMPATHY --> SYMPATHY : crisis / suicidal ideation (forced)

    note right of EMPATHY
        Feel-with: validate, presence.
        Low stress / regulated user.
    end note
    note right of SYMPATHY
        Step-in: practical, step-by-step
        guidance. High stress / crisis.
    end note
```

The four outcomes: **switch E→S**, **switch S→E**, **stay E**, **stay S** — exactly the dead-zone
behaviour verified in `ApproachSwitchPolicyTest`.

---

## 4. Pathway machine

`MonitoringAndScreeningService.determinePathway` + `LoopManager` escalation.

```mermaid
stateDiagram-v2
    [*] --> PREVENTION : intensity LOW

    PREVENTION --> INTERVENTION : intensity escalates to MODERATE / HIGH\n(ESCALATE_PATHWAY, resets approach to EMPATHY)
    PREVENTION --> PREVENTION   : stays LOW
    INTERVENTION --> INTERVENTION : active support continues
```

> Note: on `ESCALATE_PATHWAY` the `LoopManager` resets the approach to **EMPATHY** for the new
> pathway; the empathy↔sympathy machine (§3) then re-evaluates from there on the next turn.

---

## 5. Conversation loop decisions

`LoopManager.makeLoopDecision`. Evaluated once per cycle (`maxCycles = 15`).

```mermaid
stateDiagram-v2
    [*] --> Active

    Active --> Exit               : stable/improved (intensity <= 3, cycle >= 3)\nOR LOW + improving (cycle >= 2)\nOR duration over 30 min
    Active --> EscalateProfessional : max cycles (15) reached
    Active --> Continue           : improving AND intensity <= 4
    Active --> SwitchApproach     : not responding OR 2+ msgs without improvement
    Active --> EscalatePathway    : PREVENTION AND intensity escalated
    Active --> EscalateCrisis     : crisis / suicidal ideation detected
    Active --> Continue           : default

    Continue        --> Active
    SwitchApproach  --> Active : approach = opposite()
    EscalatePathway --> Active : pathway = INTERVENTION
    EscalateCrisis  --> Active : helpline + safetyLevel = critical

    Exit                --> [*]
    EscalateProfessional --> [*]
```

---

## Legend / mapping to code

| State chart | Code |
|---|---|
| §1 lifecycle | `ChatbotFlowEngine.processUserMessage` |
| §2 screening | `PreLLMScreeningController.screen` |
| §3 empathy↔sympathy | `ApproachSwitchPolicy.decide` |
| §4 pathway | `MonitoringAndScreeningService.determinePathway` |
| §5 loop | `LoopManager.makeLoopDecision` |

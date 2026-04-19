# 10 Scenario Examples - Mental Health Chatbot Flow System

## Scenario 1: User Okay (Low Intensity, No Stress)

```
USER PROFILE:
- Intensity Score: 3/10
- Emotion: joy
- Stressors: None detected
- Pathway: PREVENTION
- Approach: EMPATHY

CONVERSATION FLOW:
┌─────────────────────────────────────────────────────────┐
│ [Greeting - Time-based]                                 │
│ "Good afternoon! ☀️ How's your day going so far?        │
│ What's on your mind?"                                   │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Assessment]                                             │
│ "Current Assessment:                                    │
│  Emotion: joy | Intensity: 3/10                         │
│  Pathway: PREVENTION | Cycle: 1"                        │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Main Content]                                           │
│ "I'm so glad to hear you're feeling good!               │
│ That's wonderful. 💙"                                   │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Follow-up]                                              │
│ "What's made you feel this way today?"                  │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Exit Decision]                                          │
│ Loop ends: User stable, low intensity, positive mood    │
│ shouldContinueLoop: false                               │
│ isSessionEnding: true                                   │
│ endingReason: "stable"                                  │
└─────────────────────────────────────────────────────────┘

CLOSURE MESSAGE:
"You're in a great place right now. That's wonderful! 💙
Remember, I'm here whenever you need to talk. Take care
of yourself and enjoy the rest of your day!"
```

---

## Scenario 2: User Not Okay (High Intensity)

```
USER PROFILE:
- Intensity Score: 8/10
- Emotion: anxiety
- Stressors: work deadline, sleep deprivation
- Pathway: INTERVENTION
- Approach: EMPATHY (switching to SYMPATHY if needed)

CONVERSATION FLOW:
┌─────────────────────────────────────────────────────────┐
│ [Greeting]                                               │
│ "Good evening! 🌆 I'm glad you're reaching out.        │
│ What's been on your mind?"                              │
└─────────────────────────────────────────────────────────┘
         ↓
[User: "I can't sleep because I'm so anxious about my
 work deadline tomorrow. I feel like everything is
 falling apart"]
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Assessment]                                             │
│ "Current Assessment:                                    │
│  Emotion: anxiety | Intensity: 8/10                     │
│  Main Stressor: work                                    │
│  Pathway: INTERVENTION | Approach: EMPATHY"             │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Empathy Opening]                                        │
│ "I understand how anxiety can make everything feel      │
│ impossible. Your worry is real and valid. What you're   │
│ experiencing right now makes complete sense."           │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Main Content]                                           │
│ "I can see you're going through a really difficult      │
│ time right now. This level of anxiety needs some        │
│ active support. Let's focus on what you need in this    │
│ moment."                                                 │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Strategies]                                             │
│ "Things that might help right now:                      │
│                                                          │
│ • Try the 5-4-3-2-1 grounding exercise:                │
│   Name 5 things you see, 4 you can touch,              │
│   3 you hear, 2 you smell, 1 you taste                 │
│                                                          │
│ • Practice box breathing: breathe in for 4 counts,     │
│   hold for 4, out for 4, hold for 4                    │
│                                                          │
│ • Move your body – even a short walk can help calm    │
│   your nervous system"                                  │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Follow-up]                                              │
│ "What's the main thing you're most worried about right  │
│ now?"                                                    │
└─────────────────────────────────────────────────────────┘
         ↓
[User: "I can't focus. It just feels like no matter what
 I do, it won't be enough"]
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Loop Management - Cycle 2]                              │
│ • User responded but showing signs of overwhelm         │
│ • Intensity remains HIGH (still 8/10)                   │
│ • Decision: CONTINUE_LOOP with current approach        │
│ • Empathy still working, user is engaging              │
└─────────────────────────────────────────────────────────┘
         ↓
[Continue loop... Eventually user stabilizes or reaches
 exit condition]
```

---

## Scenario 3: User Okay Turns to Not Okay (Mid-Conversation Escalation)

```
CONVERSATION PROGRESSION:

CYCLE 1:
[User: "I'm doing alright today, just some mild stress"]
└─ Intensity: 4/10
└─ Pathway: PREVENTION
└─ Approach: EMPATHY

CYCLE 2:
[User: "Well, actually... my professor just emailed me.
 She's questioning my entire research methodology and
 wants to meet tomorrow. I think she might fail me.
 I can't breathe right now."]
└─ Intensity: ESCALATES to 9/10
└─ Detection: Crisis indicators, respiratory distress
└─ Pathway: RE-ASSESSED to INTERVENTION
└─ Approach: Remains EMPATHY initially

┌─────────────────────────────────────────────────────────┐
│ [RE-ASSESSMENT MESSAGE]                                  │
│ "I can see things have shifted. Let me be more direct   │
│ in supporting you through this difficult moment."       │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Pathway Switch: Prevention → Intervention]              │
│ • Reset approach to EMPATHY (start fresh)               │
│ • Activate crisis response protocol                     │
│ • Focus on immediate stabilization                      │
└─────────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────────┐
│ [Intervention Response]                                  │
│ "I can feel the panic in what you're sharing right now. │
│ This is a really intense moment, and you're not alone   │
│ in this.                                                 │
│                                                          │
│ Let's focus on immediate grounding:                     │
│ • Take 3-5 slow breaths with me right now             │
│ • Find something solid to hold onto                    │
│ • Remember: You have time to prepare and respond"      │
└─────────────────────────────────────────────────────────┘
```

---

## Scenario 4: Low Intensity + Mild Stress (Prevention Pathway)

```
USER PROFILE:
- Initial Message: "I have a lot on my plate but
  managing okay"
- Intensity Score: 5/10 (borderline)
- Stressors: Multiple assignments, work hours
- Stress Detected: YES
- Pathway: PREVENTION (because stress indicators present)
- Approach: EMPATHY with coping strategies

RESPONSE:
┌─────────────────────────────────────────────────────────┐
│ "It sounds like stress is building up. Let's work       │
│ together on some strategies that might help you feel    │
│ more grounded.                                          │
│                                                          │
│ Breaking things down into smaller, manageable pieces    │
│ can make a huge difference.                             │
│                                                          │
│ **Things that might help:**                             │
│ • Brain dump: write down everything on your mind       │
│ • Identify just ONE most urgent thing                  │
│ • Take 3 deep breaths and reset your focus             │
│                                                          │
│ What feels most urgent to address right now?"           │
└─────────────────────────────────────────────────────────┘

LOOP BEHAVIOR:
- If user responds positively to strategies: CONTINUE
- If intensity decreases in next cycle: Provide
  reinforcement and prepare for exit
- If stress indicators worsen: Escalate to INTERVENTION
- Expected duration: 2-3 cycles
```

---

## Scenario 5: Prevention Success (Quick Stabilization)

```
CYCLE 1:
[User: "I'm stressed about my group project"]
└─ Intensity: 5/10
└─ Pathway: PREVENTION
└─ Strategy: Help prioritize tasks

CYCLE 2:
[User: "Actually, I think I just needed to talk about it.
 We can divide the work. It doesn't seem so bad now."]
└─ Intensity: DECREASES to 3/10
└─ User Response: Positive engagement
└─ Improvement Score: +40%

┌─────────────────────────────────────────────────────────┐
│ [Reinforcement Message]                                 │
│ "I'm really proud of how you worked through this.       │
│ You showed real strength today by breaking it down      │
│ and finding a plan. 💙                                  │
│                                                          │
│ Keep using these strategies. You've got this!"          │
└─────────────────────────────────────────────────────────┘

SESSION STATUS:
└─ shouldContinueLoop: false
└─ isSessionEnding: true
└─ endingReason: "improved"
└─ Total Duration: 2 cycles / ~5 minutes
```

---

## Scenario 6: Prevention Fails → Escalates to Intervention

```
CYCLE 1:
[User: "I'm worried about my exam but I think I can handle it"]
└─ Intensity: 5/10
└─ Pathway: PREVENTION
└─ Approach: EMPATHY with prevention strategies

CYCLE 2:
[User: "Actually, I've been having panic attacks every day
 this week. I can't eat. I'm not sleeping. I don't think
 I can do this."]
└─ Intensity: ESCALATES to 8/10
└─ Condition: Prevention strategies failed
└─ User Response: Deteriorating

┌─────────────────────────────────────────────────────────┐
│ [Loop Decision: ESCALATE_PATHWAY]                       │
│                                                          │
│ Current: PREVENTION → Switching to: INTERVENTION       │
│                                                          │
│ Reason: "Intensity increased despite prevention       │
│  approach - escalate to intervention"                  │
└─────────────────────────────────────────────────────────┘

CYCLE 3:
┌─────────────────────────────────────────────────────────┐
│ [Intervention Response - EMPATHY]                       │
│ "I can really sense that you're feeling completely     │
│ overwhelmed right now. This panic cycle you're in is   │
│ serious, and you need direct support.                  │
│                                                          │
│ These physical symptoms (racing heart, difficulty      │
│ breathing) are your nervous system in overdrive.       │
│ We need to address this differently.                   │
│                                                          │
│ Immediate grounding:                                   │
│ • Box breathing (4-4-4-4)                             │
│ • Progressive muscle relaxation                        │
│ • Hold something cold                                 │
│                                                          │
│ This level of anxiety may benefit from professional   │
│ help - would you consider talking to a counselor?"    │
└─────────────────────────────────────────────────────────┘
```

---

## Scenario 7: Empathy Works (Intervention Success)

```
CONVERSATION PATTERN:

CYCLE 1:
[User: "I don't know why I'm crying. Everything feels
 impossible."]
└─ Intensity: 8/10
└─ Emotion: sadness/hopelessness
└─ Pathway: INTERVENTION
└─ Approach: EMPATHY

┌─────────────────────────────────────────────────────────┐
│ [Empathic Response]                                      │
│ "I can feel the weight in what you're sharing. These   │
│ feelings deserve space and compassion. What you're     │
│ experiencing right now is real, and I'm here for you." │
└─────────────────────────────────────────────────────────┘

CYCLE 2:
[User: "Thank you for understanding. I think I just needed
 someone to really hear me."]
└─ Intensity: DECREASES to 6/10
└─ User Engagement: Positive
└─ Empathy Assessment: WORKING ✓

CYCLE 3:
[User: "I'm starting to think I can handle this. Maybe I
 should reach out to my friend Sarah too."]
└─ Intensity: DECREASES to 4/10
└─ User Engagement: Taking action
└─ Empathy Assessment: HIGHLY EFFECTIVE ✓

┌─────────────────────────────────────────────────────────┐
│ [Positive Closure]                                       │
│ "I'm so proud of the strength you're showing. You've   │
│ moved from feeling hopeless to thinking about reaching  │
│ out - that's real progress. 💙                          │
│                                                          │
│ Keep connecting with people who care about you.        │
│ You're not alone in this."                              │
└─────────────────────────────────────────────────────────┘

OUTCOME:
└─ Total Cycles: 3
└─ Intensity Reduction: 8/10 → 4/10
└─ Approach: EMPATHY (never needed to switch)
└─ Result: User stabilized and taking positive action
```

---

## Scenario 8: Empathy Fails → Switch to Sympathy

```
CYCLE 1:
[User: "I'm so frustrated. Everything is falling apart."]
└─ Intensity: 8/10
└─ Emotion: anger/frustration
└─ Pathway: INTERVENTION
└─ Approach: EMPATHY (initial)

┌─────────────────────────────────────────────────────────┐
│ [Empathy Opening]                                        │
│ "I hear your frustration. What you're feeling makes    │
│ complete sense given the situation."                    │
└─────────────────────────────────────────────────────────┘

CYCLE 2:
[User: "You don't understand. No one gets it. I'm done
 trying to explain. Stop pretending you know what I'm
 going through."]
└─ Resistance Detected: YES ✓
└─ Empathy Effectiveness: NOT WORKING ✗
└─ Indicators:
   - "You don't understand"
   - Increased agitation
   - Rejection of conversation

┌─────────────────────────────────────────────────────────┐
│ [Loop Decision: SWITCH_APPROACH]                         │
│                                                          │
│ Current Approach: EMPATHY → Switching to: SYMPATHY    │
│                                                          │
│ Reason: "User explicitly rejects empathy and shows    │
│  increased agitation"                                  │
└─────────────────────────────────────────────────────────┘

CYCLE 3:
┌─────────────────────────────────────────────────────────┐
│ [Sympathy Opening - Different Tone]                      │
│ "I'm truly sorry you're going through such a difficult │
│ time. This is clearly incredibly hard, and I want you  │
│ to know you don't have to face it alone. 💙            │
│                                                          │
│ I can't claim to fully understand what you're going    │
│ through, but I do care about your wellbeing, and I'm   │
│ here for you."                                         │
└─────────────────────────────────────────────────────────┘

CYCLE 4:
[User: "Thanks... I guess. I just feel so alone in this."]
└─ User Response: More open
└─ Engagement: Positive shift
└─ Sympathy Effectiveness: WORKING ✓

Continue with sympathy approach...
```

---

## Scenario 9: Fluctuating States (Multiple Switches)

```
SESSION OVERVIEW: User's emotional state fluctuates
throughout conversation

CYCLE 1: User Okay
└─ Intensity: 4/10
└─ Pathway: PREVENTION
└─ Approach: EMPATHY

CYCLE 2: User Deteriorates
└─ Intensity: 7/10 (escalation detected)
└─ Pathway: INTERVENTION
└─ Approach: EMPATHY (escalate to handle increase)

CYCLE 3: User Improving
└─ Intensity: 5/10 (de-escalation)
└─ Pathway: INTERVENTION (still in intervention)
└─ Approach: EMPATHY (continuing)

CYCLE 4: User Deteriorates Again
└─ Intensity: 8/10 (re-escalation)
└─ Pathway: INTERVENTION (already there)
└─ Approach: SYMPATHY (switch because multiple cycles
                       without full improvement)

CYCLE 5: User Stabilizing
└─ Intensity: 6/10
└─ Pathway: INTERVENTION (transitioning)
└─ Approach: SYMPATHY (continue for stability)

CYCLE 6: User Stable
└─ Intensity: 3/10
└─ Pathway: TRANSITION to PREVENTION
└─ Approach: EMPATHY (shift back to growth-focused)

PATTERN ANALYSIS:
┌─────────────────────────────────────────────────────────┐
│ "Looking at your journey through this conversation:    │
│                                                          │
│ You started uncertain, then went through some intense   │
│ moments, but you kept working with me. The fact that    │
│ you're here and engaging shows real strength. 💙       │
│                                                          │
│ These ups and downs are normal. What matters is         │
│ you're moving toward stability overall."                │
└─────────────────────────────────────────────────────────┘

FINAL STATUS:
└─ Total Cycles: 6
└─ Max Intensity Reached: 8/10
└─ Final Intensity: 3/10
└─ Approach Switches: 1 (EMPATHY → SYMPATHY)
└─ Session Duration: ~20 minutes
└─ Outcome: User stabilized with coping strategies
```

---

## Scenario 10: Chronic Support (Extended Loop)

```
CONTEXT: User returning for 5th conversation this month
with ongoing mental health challenges

SESSION START:
[User: "I'm back. Been better, been worse. My anxiety
 is still really bad though."]
└─ User History: High-frequency user, persistent issues
└─ Intensity: 7/10 (consistent with past sessions)
└─ Previous Approach: Worked well with SYMPATHY
└─ Pathway: INTERVENTION (known to need this)

CYCLE 1-3: Foundation
└─ Review previous coping strategies
└─ Assess progress since last session
└─ Re-establish support relationship
└─ Approach: SYMPATHY (learned preference)

CYCLE 4-6: Active Intervention
└─ Try new coping technique
└─ Address specific triggers
└─ Small incremental improvements (+0.5-1.0 on intensity scale)

CYCLE 7-9: Strategy Reinforcement
└─ Build on what's working
└─ Practice techniques together
└─ Track small wins

CYCLE 10-12: Long-term Planning
┌─────────────────────────────────────────────────────────┐
│ "Over these sessions, I've noticed you respond really  │
│ well to validation and small, concrete steps. Let's    │
│ build a plan that plays to that strength.              │
│                                                          │
│ What would it look like if you had just ONE peaceful  │
│ day a week? Not perfect, just a little relief?"       │
└─────────────────────────────────────────────────────────┘

CYCLES 13-15: Building Resilience
└─ Celebrate incremental progress
└─ Introduce peer support resources
└─ Discuss professional therapy referral
└─ Prepare for independence

FINAL ASSESSMENT (after 15 cycles):
┌─────────────────────────────────────────────────────────┐
│ Intensity Trend: 7/10 → 6/10 → 5/10 → 4/10           │
│ Duration: 45+ minutes                                   │
│ Improvement: Consistent but gradual                     │
│ User Engagement: High throughout                        │
│ Coping Strategies: Multiple identified and practiced    │
│                                                          │
│ Recommendation: "You've shown real commitment to your  │
│ wellbeing. I think professional counseling could       │
│ provide longer-term support for chronic anxiety. I     │
│ can help you find resources. You're not alone, and     │
│ seeking help is a sign of strength." 💙               │
└─────────────────────────────────────────────────────────┘

NEXT STEPS:
└─ Referral to counselor/therapist
└─ Plan check-in conversation next week
└─ Encourage self-care maintenance
└─ Keep emergency resources accessible
```

---

## Key Patterns Summary

| Scenario | Entry | Pathway | Approach | Duration | Exit |
|----------|-------|---------|----------|----------|------|
| 1 | OK | Prevention | Empathy | 1-2 | Immediate |
| 2 | Not OK | Intervention | E→S | 5+ | Stabilize |
| 3 | OK→NotOK | P→I | Empathy | 3+ | Escalate |
| 4 | Low+Stress | Prevention | Empathy | 2-3 | Improve |
| 5 | Low+Stress | Prevention | Empathy | 2-3 | Success |
| 6 | Low | P→I | E+S | 4-6 | Escalate |
| 7 | High | Intervention | Empathy | 3-5 | Empathy works |
| 8 | High | Intervention | E→S | 4+ | Switch works |
| 9 | Fluctuate | P↔I | E↔S | 6+ | Stabilize |
| 10 | Chronic | Intervention | Sympathy | 15 | Refer |


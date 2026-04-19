# Mental Health Chatbot Flow System Implementation

## Overview

This document describes the implementation of the Mental Health Chatbot Flow System based on the provided specification. The system handles emotional assessment, pathway determination (Prevention/Intervention), approach selection (Empathy/Sympathy), and conversation loop management.

## Architecture

### Core Components

#### 1. **ChatbotFlowEngine** (`service/ChatbotFlowEngine.java`)
Main orchestrator that coordinates the entire flow:
- Manages conversation lifecycle
- Coordinates screening and assessment
- Determines pathways and approaches
- Manages conversation loop
- Handles crisis detection and escalation

#### 2. **MonitoringAndScreeningService** (`service/MonitoringAndScreeningService.java`)
Handles emotional state assessment:
- Detects emotion type from user input
- Assesses intensity level (LOW/MODERATE/HIGH)
- Detects stressors and crisis indicators
- Determines appropriate pathway

#### 3. **LoopManager** (`service/LoopManager.java`)
Manages the core conversation loop:
- Determines loop actions (continue, switch, escalate, exit)
- Detects user engagement and improvement
- Handles approach switching based on user response
- Manages exit conditions and session closure

#### 4. **ResponseTemplateService** (`service/ResponseTemplateService.java`)
Generates response templates:
- Empathy and sympathy openings
- Main content for prevention and intervention
- Coping strategies based on emotion/stressor
- Follow-up questions
- Session closure messages

#### 5. **GreetingService** (`service/GreetingService.java`)
Handles time-based greetings:
- Morning (05:00-11:59)
- Afternoon (12:00-17:59)
- Evening (18:00-21:59)
- Night (22:00-04:59)

#### 6. **MonitoringContext** (`model/MonitoringContext.java`)
Tracks conversation state:
- Current emotional state (emotion, intensity, stressor)
- Pathway and approach tracking
- Conversation history
- Loop cycle count
- User engagement metrics

#### 7. **FlowResponse** (`model/FlowResponse.java`)
Response object containing:
- Greeting, assessment, main content, strategies, follow-up
- Pathway and approach metadata
- Loop control information
- Session state

### Enumerations

- **IntensityLevel**: LOW (1-4), MODERATE (5-7), HIGH (8-10)
- **ApproachType**: EMPATHY, SYMPATHY
- **PathwayType**: PREVENTION, INTERVENTION

## Decision Tree

### Initial Pathway Selection

```
IF intensity_level = LOW AND no_stress_detected
  → PREVENTION (brief check-in, positive reinforcement)

IF intensity_level = LOW AND stress_detected
  → PREVENTION (with coping strategies)

IF intensity_level = MODERATE OR HIGH
  → INTERVENTION (active support required)
```

### Approach Selection

```
First interaction: EMPATHY (start with understanding)

Crisis detected: SYMPATHY (caring support)

High intensity: EMPATHY initially, switch to SYMPATHY if ineffective

User resistance to empathy: SWITCH to SYMPATHY
  Indicators:
  - "You don't understand"
  - Increased frustration
  - Emotional withdrawal

User responsive to sympathy: May switch back to EMPATHY
  Indicators:
  - User becoming more open
  - Seeking understanding
  - Asking reflective questions
```

### Loop Logic

```
LOOP_START:
  1. Apply current approach (Empathy OR Sympathy)
  2. Deliver prevention OR intervention content
  3. Assess user response
  4. Check intensity level
  
  IF user_improving AND intensity_decreasing:
    → Continue current approach
  
  IF user_not_responding (2+ messages without improvement):
    → SWITCH approach (Empathy ↔ Sympathy)
  
  IF user_deteriorating OR intensity_escalates:
    → Escalate pathway (Prevention → Intervention)
  
  IF user_stable AND intensity_low:
    → EXIT loop, provide closure
  
  IF max_cycles_reached (15 cycles):
    → Escalate to professional help recommendation
  
  ELSE:
    → REPEAT LOOP_START
```

## REST API Endpoints

### Process User Message
```
POST /api/chat/flow/process

Headers:
  X-User-ID: UUID of the user
  X-Conversation-ID: (Optional) ID of the conversation

Request Body:
{
  "userMessage": "I'm feeling stressed about my assignment",
  "intensityScore": 7,
  "language": "en"
}

Response:
{
  "conversationId": "...",
  "greeting": "Good afternoon! ☀️...",
  "assessment": "**Current Assessment:**...",
  "mainContent": "It sounds like academic pressure is building...",
  "strategies": "**Things that might help right now:**...",
  "followUp": "What's the one thing that would give you most relief?",
  "fullResponse": "...",
  "pathway": "PREVENTION",
  "approach": "EMPATHY",
  "intensity": 7,
  "emotion": "stress",
  "cycleNumber": 1,
  "shouldContinueLoop": true,
  "isSessionEnding": false,
  "metadata": {
    "timestamp": 1234567890
  }
}
```

### End Session
```
POST /api/chat/flow/end-session

Headers:
  X-User-ID: UUID of the user
  X-Conversation-ID: ID of the conversation

Response: 200 OK
```

### Get Context (Debugging)
```
GET /api/chat/flow/context

Headers:
  X-User-ID: UUID of the user

Response:
{
  "conversationId": "...",
  "currentEmotion": "stress",
  "intensityScore": 7,
  "pathway": "PREVENTION",
  "approach": "EMPATHY",
  "cycleNumber": 2,
  "sessionDurationMinutes": 5
}
```

## 10 Supported Scenarios

### Scenario 1: User Okay (Low Intensity, No Stress)
- **Pathway**: PREVENTION
- **Approach**: EMPATHY
- **Action**: Brief check-in, positive reinforcement, optional follow-up scheduling
- **Exit**: Immediate or after 1-2 cycles

### Scenario 2: User Not Okay (High Intensity)
- **Pathway**: INTERVENTION
- **Approach**: EMPATHY → SYMPATHY (if needed)
- **Action**: Direct emotional support, crisis assessment
- **Loop**: Continue until stabilization or escalation

### Scenario 3: User Okay Turns to Not Okay (Mid-Conversation)
- **Detection**: Intensity score suddenly increases
- **Action**: RE-ASSESS, trigger pathway escalation (Prevention → Intervention)
- **Loop**: Switch to INTERVENTION with EMPATHY

### Scenario 4: Low Intensity + Mild Stress (Prevention Needed)
- **Pathway**: PREVENTION
- **Approach**: EMPATHY with coping strategies
- **Action**: Offer breathing exercises, grounding techniques
- **Loop**: Continue prevention until stress reduces

### Scenario 5: Prevention Success (User Stabilizes Quickly)
- **Indicator**: Intensity reduces within 1-2 cycles
- **Action**: Provide reinforcement, self-care tips
- **Exit**: Positive closure within 3 cycles

### Scenario 6: Prevention Fails → Escalates to Intervention
- **Detection**: Intensity increases despite prevention approach
- **Action**: Escalate pathway immediately
- **New Strategy**: INTERVENTION with EMPATHY/SYMPATHY
- **Loop**: Intensive support until de-escalation

### Scenario 7: Empathy Works (Intervention Success)
- **Indicator**: Positive user response to empathetic approach
- **Action**: Continue empathy-based techniques
- **Loop**: 2-3 cycles of empathy, then transition to prevention/maintenance

### Scenario 8: Empathy Fails → Switch to Sympathy
- **Detection**: User rejects empathy, increased frustration, withdrawal
- **Action**: SWITCH APPROACH from EMPATHY to SYMPATHY
- **Response**: "I'm so sorry you're going through this..."
- **Loop**: Continue with sympathy approach

### Scenario 9: Fluctuating States (Multiple Switches)
- **Pattern**: User improves → deteriorates → improves again
- **Action**: RE-ASSESS at each change, apply appropriate pathway/approach
- **Loop**: Multiple cycles with dynamic switching (empathy ↔ sympathy)
- **Management**: Careful monitoring for pattern recognition

### Scenario 10: Chronic Support (Extended Loop)
- **Context**: User with ongoing distress from previous sessions
- **Action**: Review history, assess current state, apply learned preferences
- **Loop**: Extended loop (5+ cycles) with incremental improvements
- **Strategy**: Build long-term coping plan across sessions

## Crisis Detection

The system detects and escalates the following indicators:

1. **Suicidal Ideation**
   - Keywords: "suicide", "kill myself", "end it", "hopeless"
   - Action: Immediate escalation to crisis resources

2. **High Distress**
   - Intensity: 9-10 combined with persistence
   - Action: Recommend professional help after 10+ cycles

3. **User Deterioration**
   - Intensity increasing over cycles
   - Action: Escalate from Prevention to Intervention

## Configuration

### Maximum Loop Cycles
- Default: 15 cycles per session
- After max cycles with HIGH intensity: Escalate to professional help

### Session Duration
- Maximum: 30 minutes
- Auto-exit if exceeded

### Intensity Ranges
- LOW: 1-4 (uses PREVENTION pathway)
- MODERATE: 5-7 (uses INTERVENTION with EMPATHY)
- HIGH: 8-10 (uses INTERVENTION with EMPATHY/SYMPATHY)

## Usage Example

```java
// Create flow engine
ChatbotFlowEngine engine = new ChatbotFlowEngine("en");

// Process user message
UUID userId = UUID.fromString("user-id-here");
String conversationId = UUID.randomUUID().toString();

FlowResponse response = engine.processUserMessage(
    userId,
    conversationId,
    "I'm feeling really stressed about my upcoming exam",
    7  // intensity score
);

// Use response in your application
System.out.println(response.getFullResponse());
System.out.println("Pathway: " + response.getPathway());
System.out.println("Continue loop: " + response.shouldContinueLoop());
```

## Language Support

Currently supports:
- English (en)
- Malay (ms)

Easy to extend for additional languages by modifying:
- `GreetingService`
- `ResponseTemplateService`
- `MonitoringAndScreeningService`

## Testing

The system includes:
- Comprehensive emotion detection
- Stressor identification
- Crisis indicator detection
- User engagement metrics
- Loop action determination
- Approach switching logic

Test coverage includes all 10 scenarios and edge cases.

## Future Enhancements

1. **Database Integration**
   - Store conversation history
   - Track long-term improvement patterns
   - Personalized response templates

2. **Machine Learning**
   - Learn user-specific switching patterns
   - Predict escalation risks
   - Personalize emotion detection

3. **Multi-language Support**
   - Extend to more languages
   - Cultural sensitivity in responses

4. **Integration**
   - Connect with professional counselor network
   - Real-time escalation to humans
   - Crisis hotline integration

5. **Analytics**
   - Conversation analytics dashboard
   - Outcome tracking
   - Effectiveness metrics

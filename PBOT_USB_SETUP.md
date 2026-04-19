# PBOT USB Setup

## 1) Find your COM port
On Windows, open Device Manager and check `Ports (COM & LPT)`.
Example port names: `COM3`, `COM5`, `COM7`.

## 2) Configure backend
Set environment variables before starting backend:

```powershell
$env:PBOT_USB_PORT = "COM5"
$env:PBOT_USB_BAUD_RATE = "115200"
```

Then start backend as usual.

## 3) Connect PBOT from API
List ports:

```http
GET /api/pbot/ports
```

Connect:

```http
POST /api/pbot/connect
Content-Type: application/json

{
  "port": "COM5",
  "baudRate": 115200
}
```

Check status:

```http
GET /api/pbot/status
```

Disconnect:

```http
POST /api/pbot/disconnect
```

## 4) Send commands
Raw command:

```http
POST /api/pbot/raw
Content-Type: application/json

{
  "command": "EMOTION:happy:7"
}
```

Emotion command (recommended):

```http
POST /api/pbot/emotion
Content-Type: application/json

{
  "emotion": "happy",
  "intensity": 7
}
```

## 5) Auto-sync with chatbot
When PBOT is connected, `/api/chat/flow/process` and `/api/chat/flow/process-with-ai` now auto-send emotion updates.

## 6) Important firmware note
`Hardware/pbot_mata.ino` now parses serial commands at `115200`.

Supported commands:

- `EMOTION:happy:7`
- `EMOTION:sad:8`
- `EMOTION:angry:9`
- `EMOTION:normal:5`
- `EMOTION:sus:6`
- `FACE:happy`
- `FACE:normal`
- `BLINK:ON`
- `BLINK:OFF`
- `PING`

Firmware sends acknowledgements like `OK:...` and `PONG`.

package com.radai.service;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class PbotUsbService {
    private static final Logger logger = LoggerFactory.getLogger(PbotUsbService.class);

    private final AtomicReference<SerialPort> activePort = new AtomicReference<>();
    private volatile Thread readerThread;
    private volatile String lastFromBot = "";

    @Value("${pbot.usb.port:}")
    private String defaultPort;

    @Value("${pbot.usb.baud-rate:115200}")
    private int defaultBaudRate;

    @Value("${pbot.usb.write-timeout-ms:1000}")
    private int writeTimeoutMs;

    @Value("${pbot.usb.auto-connect:true}")
    private boolean autoConnect;

    /**
     * Best-effort connect to the configured port once the app is up, so the chatbot drives the
     * physical PBOT without any manual step. Non-fatal: if the robot isn't plugged in (or the port
     * is busy), we just log a warning and the user can connect later via /api/pbot/connect.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void autoConnectOnStartup() {
        if (!autoConnect || defaultPort == null || defaultPort.isBlank()) {
            return;
        }
        // Catch Throwable, not just Exception: the native serial library can raise an Error
        // (e.g. UnsatisfiedLinkError) which must never be allowed to crash application startup.
        try {
            connect(defaultPort, null);
            logger.info("PBOT auto-connected to {} on startup", defaultPort);
        } catch (Throwable t) {
            logger.warn("PBOT auto-connect to {} skipped (connect manually later): {}", defaultPort, t.toString());
        }
    }

    public List<Map<String, String>> listPorts() {
        return Arrays.stream(SerialPort.getCommPorts())
            .map(port -> Map.of(
                "name", safe(port.getSystemPortName()),
                "descriptiveName", safe(port.getDescriptivePortName()),
                "portDescription", safe(port.getPortDescription())
            ))
            .toList();
    }

    public synchronized Map<String, Object> connect(String requestedPort, Integer requestedBaudRate) {
        String targetPort = (requestedPort != null && !requestedPort.isBlank())
            ? requestedPort.trim()
            : defaultPort;

        if (targetPort == null || targetPort.isBlank()) {
            throw new IllegalArgumentException("No USB port provided. Set pbot.usb.port or pass port in request.");
        }

        int baudRate = requestedBaudRate != null ? requestedBaudRate : defaultBaudRate;

        disconnect();

        SerialPort serialPort = SerialPort.getCommPort(targetPort);
        serialPort.setBaudRate(baudRate);
        // Enable reads too, so we can log what the board sends back (PBOT_READY, OK:EMOTION:...).
        serialPort.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 200, writeTimeoutMs);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("Failed to open PBOT USB port: " + targetPort);
        }

        // ESP32 + CH340: the serial driver asserts DTR/RTS on open, and on a dev board those are
        // wired to EN (reset) and GPIO0 (boot). Asserted DTR holds the ESP32 in reset, so it never
        // runs the sketch. De-assert both so the board boots into the app and starts responding.
        // A brief reset pulse then guarantees a clean boot into the firmware (not the bootloader).
        serialPort.clearRTS();   // GPIO0 high -> normal (run) mode
        serialPort.clearDTR();   // EN released -> board runs
        try {
            Thread.sleep(50);
            serialPort.setDTR();     // pull EN low (hold reset) briefly...
            Thread.sleep(80);
            serialPort.clearDTR();   // ...release -> clean reboot into the sketch
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        activePort.set(serialPort);
        startReader(serialPort);
        logger.info("Connected to PBOT on {} at {} baud (listening for board replies)", targetPort, baudRate);

        return status();
    }

    public synchronized void disconnect() {
        stopReader();
        SerialPort port = activePort.getAndSet(null);
        if (port != null && port.isOpen()) {
            port.closePort();
            logger.info("Disconnected PBOT USB port {}", port.getSystemPortName());
        }
    }

    /**
     * Background reader: logs every line the board sends back so we can verify the firmware is
     * alive and receiving. On boot the sketch prints "PBOT_READY"; after each command it replies
     * "OK:EMOTION:...". Seeing these in the backend log = the right board is wired and listening.
     */
    private void startReader(SerialPort port) {
        stopReader();
        Thread t = new Thread(() -> {
            StringBuilder line = new StringBuilder();
            byte[] buf = new byte[256];
            while (port.isOpen() && !Thread.currentThread().isInterrupted()) {
                int n;
                try {
                    n = port.readBytes(buf, buf.length);
                } catch (Exception e) {
                    break;
                }
                if (n < 0) {
                    break;
                }
                for (int i = 0; i < n; i++) {
                    char c = (char) (buf[i] & 0xFF);
                    if (c == '\n') {
                        String s = line.toString().trim();
                        if (!s.isEmpty()) {
                            lastFromBot = s;
                            logger.info("PBOT << {}", s);
                        }
                        line.setLength(0);
                    } else if (c != '\r') {
                        line.append(c);
                        if (line.length() > 512) {
                            line.setLength(0); // guard against runaway buffer
                        }
                    }
                }
            }
        }, "pbot-serial-reader");
        t.setDaemon(true);
        t.start();
        readerThread = t;
    }

    private void stopReader() {
        Thread t = readerThread;
        readerThread = null;
        if (t != null) {
            t.interrupt();
        }
    }

    public synchronized Map<String, Object> sendRaw(String command) {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command cannot be empty.");
        }

        String normalized = command.endsWith("\n") ? command : command + "\n";
        write(normalized);

        return Map.of(
            "sent", normalized.trim(),
            "status", "ok"
        );
    }

    public synchronized Map<String, Object> sendEmotion(String emotion, Integer intensity) {
        if (emotion == null || emotion.isBlank()) {
            throw new IllegalArgumentException("Emotion is required.");
        }

        String mappedEmotion = mapEmotion(emotion);
        int level = intensity != null ? Math.max(0, Math.min(10, intensity)) : 5;
        String payload = String.format(Locale.ROOT, "EMOTION:%s:%d", mappedEmotion, level);
        logger.info("PBOT emotion sync: rawEmotion='{}', mapped='{}', intensity={}", emotion, mappedEmotion, level);
        write(payload + "\n");

        return Map.of(
            "sent", payload,
            "mappedEmotion", mappedEmotion,
            "status", "ok"
        );
    }

    public Map<String, Object> status() {
        SerialPort port = activePort.get();
        boolean connected = port != null && port.isOpen();

        return Map.of(
            "connected", connected,
            "port", connected ? port.getSystemPortName() : "",
            "baudRate", connected ? port.getBaudRate() : defaultBaudRate,
            "lastFromBot", lastFromBot == null ? "" : lastFromBot
        );
    }

    private void write(String payload) {
        SerialPort port = activePort.get();

        if (port == null || !port.isOpen()) {
            throw new IllegalStateException("PBOT USB is not connected.");
        }

        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        int written = port.writeBytes(data, data.length);

        if (written <= 0) {
            throw new IllegalStateException("Failed to write command to PBOT.");
        }
    }

    private String mapEmotion(String emotion) {
        String value = emotion.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "joy", "happy" -> "happy";
            case "calm", "relaxed", "neutral", "normal" -> "normal";

            case "sad", "sadness", "lonely", "loneliness", "hopeless", "depressed", "down", "unhappy" -> "sad";

            case "anger", "angry", "frustrated", "furious", "mad", "hate" -> "angry";

            case "anxious", "anxiety", "stressed", "stress", "worried", "panic", "overwhelmed", "surprise", "surprised", "confused", "confusion" -> "sus";

            case "exhaustion", "exhausted", "tired", "fatigue", "drained", "sleepy" -> "sleep";
            default -> "normal";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}


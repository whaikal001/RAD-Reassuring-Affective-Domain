package com.SocializerAI.service;

import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${pbot.usb.port:}")
    private String defaultPort;

    @Value("${pbot.usb.baud-rate:115200}")
    private int defaultBaudRate;

    @Value("${pbot.usb.write-timeout-ms:1000}")
    private int writeTimeoutMs;

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
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, writeTimeoutMs);

        if (!serialPort.openPort()) {
            throw new IllegalStateException("Failed to open PBOT USB port: " + targetPort);
        }

        activePort.set(serialPort);
        logger.info("Connected to PBOT on {} at {} baud", targetPort, baudRate);

        return status();
    }

    public synchronized void disconnect() {
        SerialPort port = activePort.getAndSet(null);
        if (port != null && port.isOpen()) {
            port.closePort();
            logger.info("Disconnected PBOT USB port {}", port.getSystemPortName());
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
            "baudRate", connected ? port.getBaudRate() : defaultBaudRate
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

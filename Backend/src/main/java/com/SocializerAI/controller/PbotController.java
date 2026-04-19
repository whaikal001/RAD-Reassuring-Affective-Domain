package com.SocializerAI.controller;

import com.SocializerAI.service.PbotUsbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pbot")
public class PbotController {
    private final PbotUsbService pbotUsbService;

    public PbotController(PbotUsbService pbotUsbService) {
        this.pbotUsbService = pbotUsbService;
    }

    @GetMapping("/ports")
    public ResponseEntity<?> listPorts() {
        return ResponseEntity.ok(pbotUsbService.listPorts());
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(pbotUsbService.status());
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connect(@RequestBody(required = false) Map<String, Object> request) {
        String port = request != null ? asString(request.get("port")) : null;
        Integer baudRate = request != null ? asInteger(request.get("baudRate")) : null;
        return ResponseEntity.ok(pbotUsbService.connect(port, baudRate));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<?> disconnect() {
        pbotUsbService.disconnect();
        return ResponseEntity.ok(Map.of("status", "disconnected"));
    }

    @PostMapping("/raw")
    public ResponseEntity<?> sendRaw(@RequestBody Map<String, Object> request) {
        String command = asString(request.get("command"));
        return ResponseEntity.ok(pbotUsbService.sendRaw(command));
    }

    @PostMapping("/emotion")
    public ResponseEntity<?> sendEmotion(@RequestBody Map<String, Object> request) {
        String emotion = asString(request.get("emotion"));
        Integer intensity = asInteger(request.get("intensity"));
        return ResponseEntity.ok(pbotUsbService.sendEmotion(emotion, intensity));
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}

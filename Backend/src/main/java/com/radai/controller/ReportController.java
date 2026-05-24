package com.radai.controller;

import org.springframework.web.bind.annotation.*;
import com.radai.service.ReportService;
import com.radai.model.Report;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService svc;

    public ReportController(ReportService svc){ this.svc = svc; }

    @PostMapping("/generate/{userId}")
    public Report generate(@PathVariable("userId") UUID userId, @RequestBody Map<String,String> body) throws Exception {
        LocalDateTime start = LocalDateTime.parse(body.get("startDate"));
        LocalDateTime end = LocalDateTime.parse(body.get("endDate"));
        String type = body.get("reportType");
        return svc.generateReport(userId, start, end, type);
    }

    @GetMapping("/user/{userId}")
    public List<Report> userReports(@PathVariable("userId") UUID userId){ return svc.getReports(userId); }

    @GetMapping("/{reportId}")
    public Report access(@PathVariable("reportId") UUID reportId){ return svc.accessReport(reportId); }
}


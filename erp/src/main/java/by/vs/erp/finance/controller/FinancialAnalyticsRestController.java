package by.vs.erp.finance.controller;

import by.vs.erp.finance.dto.FinancialReportDto;
import by.vs.erp.finance.service.FinancialAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class FinancialAnalyticsRestController {

    private final FinancialAnalyticsService analyticsService;

    @GetMapping("/report")
    @PreAuthorize(value = "hasRole('ADMIN')")
    public FinancialReportDto getReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return analyticsService.generateReport(start, end);
    }
}


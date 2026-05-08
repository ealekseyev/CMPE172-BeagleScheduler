package com.beaglescheduler.cmpe172project.controller;

import com.beaglescheduler.cmpe172project.service.BookingMetricsService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final BookingMetricsService metricsService;

    public HealthController(JdbcTemplate jdbcTemplate, BookingMetricsService metricsService) {
        this.jdbcTemplate = jdbcTemplate;
        this.metricsService = metricsService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        Map<String, Object> dbComponent = new LinkedHashMap<>();
        boolean dbUp = false;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbComponent.put("status", "UP");
            dbComponent.put("detail", "H2 in-memory database responding");
            dbUp = true;
        } catch (DataAccessException e) {
            dbComponent.put("status", "DOWN");
            dbComponent.put("detail", "DataAccessException: " + e.getMostSpecificCause().getMessage());
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalBookings", metricsService.getTotalBookings());
        metrics.put("failedBookings", metricsService.getFailedBookings());
        metrics.put("avgBookingLatencyMs", Math.round(metricsService.getAvgBookingLatencyMs() * 10.0) / 10.0);

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", dbComponent);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dbUp ? "UP" : "DOWN");
        body.put("timestamp", timestamp);
        body.put("components", components);
        body.put("metrics", metrics);

        HttpStatus status = dbUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(body);
    }
}

package com.beaglescheduler.cmpe172project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Simulates an external notification service boundary (e.g. SendGrid, Twilio).
 * In production this would be a separate host/service; here it's a loopback stub
 * that demonstrates the HTTP distribution boundary without requiring a real provider.
 */
@RestController
public class MockNotificationController {

    private static final Logger log = LoggerFactory.getLogger(MockNotificationController.class);

    @PostMapping("/mock/notify")
    public Map<String, String> notify(@RequestBody Map<String, Object> payload) {
        Object appointmentId = payload.get("appointmentId");
        Object email = payload.get("customerEmail");
        log.info("Mock notification received: appointmentId={}, email={}", appointmentId, email);
        return Map.of(
            "status", "sent",
            "notificationId", UUID.randomUUID().toString()
        );
    }
}

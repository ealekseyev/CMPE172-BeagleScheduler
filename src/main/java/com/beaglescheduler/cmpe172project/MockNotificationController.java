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
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BRIGHT_RED = "\u001B[1;31m";
    private static final String ANSI_BRIGHT_YELLOW = "\u001B[1;33m";

    @PostMapping("/mock/notify")
    public Map<String, String> notify(@RequestBody Map<String, Object> payload) {
        Object notificationId = payload.get("notificationId");
        Object appointmentId = payload.get("appointmentId");
        Object email = payload.get("customerEmail");
        Object channel = payload.get("channel");
        Object type = payload.get("type");

        log.info("Mock notification received: appointmentId={}, email={}", appointmentId, email);
        log.warn("{}\n" +
                "============================================================\n" +
                " MOCK NOTIFICATION SENT\n" +
                " notificationId: {}\n" +
                " appointmentId:  {}\n" +
                " channel:        {}\n" +
                " type:           {}\n" +
                " email:          {}\n" +
                "============================================================{}",
            ANSI_BRIGHT_RED + ANSI_BRIGHT_YELLOW,
            notificationId,
            appointmentId,
            channel,
            type,
            email,
            ANSI_RESET);

        return Map.of(
            "status", "sent",
            "notificationId", UUID.randomUUID().toString()
        );
    }
}

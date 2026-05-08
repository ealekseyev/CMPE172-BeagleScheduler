package com.beaglescheduler.cmpe172project;

import com.beaglescheduler.cmpe172project.model.Appointment;
import com.beaglescheduler.cmpe172project.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Coarse-grained interface: a single call triggers the full notification.
 * Internally it looks up the appointment and POSTs to the mock notification
 * service over HTTP — the same boundary that would point to an external host
 * (SendGrid, Twilio, etc.) in production.
 */
@RestController
public class AppointmentNotificationController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationController.class);

    private final AppointmentRepository appointmentRepository;
    private final RestTemplate restTemplate;

    public AppointmentNotificationController(AppointmentRepository appointmentRepository,
                                             RestTemplate restTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/appointments/{id}/notify")
    public Map<?, ?> notifyAppointment(@PathVariable long id) {
        log.info("Notification triggered for appointmentId={}", id);
        try {
            Appointment appt = appointmentRepository.findById(id);

            Map<String, Object> payload = Map.of(
                "appointmentId", appt.getAppointmentId(),
                "customerEmail", appt.getCustomerEmail() != null ? appt.getCustomerEmail() : "",
                "message", "Your appointment for " + appt.getModelName()
                    + " from " + appt.getStartDate() + " to " + appt.getEndDate()
                    + " is confirmed.",
                "assignedTechnician", appt.getAssignedTechnicianName() != null
                    ? appt.getAssignedTechnicianName() : "Unassigned",
                "machineReady", appt.isMachineReady()
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:8080/mock/notify",
                payload,
                Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Notification failed for appointmentId={}: HTTP {} from /mock/notify",
                    id, response.getStatusCode().value());
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Exception during notification dispatch for appointmentId={}: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}

package com.beaglescheduler.cmpe172project;

import com.beaglescheduler.cmpe172project.model.Appointment;
import com.beaglescheduler.cmpe172project.repository.AppointmentRepository;
import com.beaglescheduler.cmpe172project.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Coarse-grained trigger endpoint used by the Admin "Notify" button.
 * Writes a pickup_reminder notification to the outbox; the background
 * dispatcher (NotificationService) forwards it to /mock/notify and marks it sent.
 */
@RestController
public class AppointmentNotificationController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotificationController.class);

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    public AppointmentNotificationController(AppointmentRepository appointmentRepository,
                                             NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }

    @PostMapping("/appointments/{id}/notify")
    public Map<String, Object> notifyAppointment(@PathVariable long id) {
        log.info("Manual notification triggered for appointmentId={}", id);
        Appointment appt = appointmentRepository.findById(id);

        String payload = String.format(
            "{\"appointmentId\":%d,\"customerEmail\":\"%s\",\"machine\":\"%s\",\"model\":\"%s\",\"start\":\"%s\",\"end\":\"%s\"}",
            appt.getAppointmentId(),
            appt.getCustomerEmail() != null ? appt.getCustomerEmail() : "",
            appt.getSerialNumber()  != null ? appt.getSerialNumber()  : "",
            appt.getModelName()     != null ? appt.getModelName()     : "",
            appt.getStartDate(), appt.getEndDate()
        );

        notificationService.queue(appt.getAppointmentId(), appt.getCustomerId(),
            "email", "pickup_reminder", payload);

        return Map.of("status", "queued", "appointmentId", id);
    }
}

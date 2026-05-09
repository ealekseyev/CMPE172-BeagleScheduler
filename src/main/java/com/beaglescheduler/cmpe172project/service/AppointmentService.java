package com.beaglescheduler.cmpe172project.service;

import com.beaglescheduler.cmpe172project.model.AppUser;
import com.beaglescheduler.cmpe172project.model.Appointment;
import com.beaglescheduler.cmpe172project.model.AvailabilitySlot;
import com.beaglescheduler.cmpe172project.repository.AppUserRepository;
import com.beaglescheduler.cmpe172project.repository.AppointmentRepository;
import com.beaglescheduler.cmpe172project.repository.AvailabilitySlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AvailabilitySlotRepository slotRepo;
    private final AppUserRepository userRepo;
    private final AppointmentRepository apptRepo;
    private final BookingMetricsService metricsService;
    private final NotificationService notificationService;

    public AppointmentService(AvailabilitySlotRepository slotRepo,
                              AppUserRepository userRepo,
                              AppointmentRepository apptRepo,
                              BookingMetricsService metricsService,
                              NotificationService notificationService) {
        this.slotRepo = slotRepo;
        this.userRepo = userRepo;
        this.apptRepo = apptRepo;
        this.metricsService = metricsService;
        this.notificationService = notificationService;
    }

    /**
     * Books an appointment for the given slot.
     * The UPDATE + INSERT are wrapped in a single transaction.
     * If the slot was already taken (0 rows updated), throws SlotUnavailableException.
     * After saving, assigns a technician via round-robin on appointment ID.
     */
    @Transactional
    public Appointment bookAppointment(long slotId, String name, String email, String notes) {
        log.info("Booking attempt: slotId={}, customerEmail={}", slotId, email);
        long startNs = System.nanoTime();

        try {
            // 1. Atomically mark slot unavailable; 0 rows = slot already taken
            int updated = slotRepo.markUnavailable(slotId);
            if (updated == 0) {
                log.warn("Slot already taken: slotId={}, requestedBy={} (concurrent conflict)", slotId, email);
                metricsService.recordFailure();
                throw new SlotUnavailableException(slotId);
            }
            log.info("Slot successfully marked unavailable: slotId={}", slotId);

            // 2. Fetch slot details for machine_id
            AvailabilitySlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

            // 3. Upsert customer by email
            AppUser user = new AppUser();
            user.setName(name);
            user.setEmail(email);
            user = userRepo.save(user);

            // 4. Insert appointment
            Appointment appt = new Appointment();
            appt.setSlotId(slotId);
            appt.setCustomerId(user.getUserId());
            appt.setMachineId(slot.getMachineId());
            appt.setStatus("CONFIRMED");
            appt.setCustomerNotes(notes);
            appt = apptRepo.save(appt);

            // 5. Assign technician via round-robin on appointment ID
            List<AppUser> technicians = userRepo.findByRole("TECHNICIAN");
            if (!technicians.isEmpty()) {
                int index = (int)(appt.getAppointmentId() % technicians.size());
                AppUser tech = technicians.get(index);
                apptRepo.assignTechnician(appt.getAppointmentId(), tech.getUserId());
                log.info("Assigned technician {} to appointmentId={}", tech.getName(), appt.getAppointmentId());
            }

            // 6. Return enriched appointment with display fields (including technician)
            Appointment result = apptRepo.findById(appt.getAppointmentId());

            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            metricsService.recordSuccess(latencyMs);
            log.info("Booking confirmed: appointmentId={}, slotId={}, machine={}, days={}, totalCost=${}, tech={}",
                result.getAppointmentId(), slotId, result.getSerialNumber(),
                result.getDurationDays(), String.format("%.2f", result.getTotalCost()),
                result.getAssignedTechnicianName());

            // 7. Queue notifications inside the transaction (outbox pattern)
            String payload = buildPayload(result);
            notificationService.queue(result.getAppointmentId(), result.getCustomerId(),
                "email", "booking_confirmed", payload);
            if (result.getAssignedTechnicianId() != 0) {
                notificationService.queue(result.getAppointmentId(), result.getAssignedTechnicianId(),
                    "email", "technician_assignment", payload);
            }

            return result;

        } catch (DataAccessException e) {
            log.error("Database error during booking for slotId={}: {}", slotId, e.getMostSpecificCause().getMessage(), e);
            metricsService.recordFailure();
            throw e;
        }
    }

    @Transactional
    public void cancelAppointment(long id) {
        Appointment appt = apptRepo.findById(id);
        apptRepo.cancelAppointment(id);
        apptRepo.reopenSlot(appt.getSlotId());
        notificationService.queue(id, appt.getCustomerId(), "email", "booking_cancelled",
            buildPayload(appt));
        log.info("Appointment cancelled and slot reopened: appointmentId={}, slotId={}", id, appt.getSlotId());
    }

    public void markMachineReady(long id) {
        Appointment appt = apptRepo.findById(id);
        apptRepo.markMachineReady(id);
        notificationService.queue(id, appt.getCustomerId(), "email", "readiness_alert",
            buildPayload(appt));
        log.info("Machine marked ready for appointmentId={}", id);
    }

    @Transactional
    public Appointment rescheduleAppointment(long oldApptId, long newSlotId) {
        Appointment oldAppt = apptRepo.findById(oldApptId);
        if (!"CONFIRMED".equals(oldAppt.getStatus())) {
            throw new IllegalStateException("Only CONFIRMED appointments can be rescheduled.");
        }
        // Cancel old + reopen its slot (no cancel notification)
        apptRepo.cancelAppointment(oldApptId);
        apptRepo.reopenSlot(oldAppt.getSlotId());

        // Claim new slot atomically
        int rows = slotRepo.markUnavailable(newSlotId);
        if (rows == 0) throw new SlotUnavailableException(newSlotId);

        AvailabilitySlot newSlot = slotRepo.findById(newSlotId)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + newSlotId));

        // New appointment — same customer + notes
        Appointment newAppt = new Appointment();
        newAppt.setSlotId(newSlotId);
        newAppt.setCustomerId(oldAppt.getCustomerId());
        newAppt.setMachineId(newSlot.getMachineId());
        newAppt.setStatus("CONFIRMED");
        newAppt.setCustomerNotes(oldAppt.getCustomerNotes());
        newAppt = apptRepo.save(newAppt);

        // Round-robin technician
        List<AppUser> techs = userRepo.findByRole("TECHNICIAN");
        if (!techs.isEmpty()) {
            AppUser tech = techs.get((int)(newAppt.getAppointmentId() % techs.size()));
            apptRepo.assignTechnician(newAppt.getAppointmentId(), tech.getUserId());
        }

        Appointment result = apptRepo.findById(newAppt.getAppointmentId());
        notificationService.queue(result.getAppointmentId(), result.getCustomerId(),
            "email", "booking_rescheduled", buildPayload(result));
        log.info("Appointment rescheduled: old={}, new={}, newSlot={}",
            oldApptId, result.getAppointmentId(), newSlotId);
        return result;
    }

    public Appointment getAppointmentById(long id) {
        return apptRepo.findById(id);
    }

    public List<Appointment> getAllAppointments() {
        return apptRepo.findAll();
    }

    public List<Appointment> getAppointmentsForTechnician(long techId) {
        return apptRepo.findByAssignedTechnician(techId);
    }

    public List<Appointment> getAppointmentsForCustomer(long customerId) {
        return apptRepo.findByCustomer(customerId);
    }

    private String buildPayload(Appointment appt) {
        return String.format(
            "{\"appointmentId\":%d,\"customerEmail\":\"%s\",\"machine\":\"%s\",\"model\":\"%s\",\"start\":\"%s\",\"end\":\"%s\"}",
            appt.getAppointmentId(),
            appt.getCustomerEmail() != null ? appt.getCustomerEmail() : "",
            appt.getSerialNumber()  != null ? appt.getSerialNumber()  : "",
            appt.getModelName()     != null ? appt.getModelName()     : "",
            appt.getStartDate(), appt.getEndDate()
        );
    }
}

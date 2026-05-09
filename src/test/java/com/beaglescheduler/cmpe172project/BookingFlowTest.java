package com.beaglescheduler.cmpe172project;

import com.beaglescheduler.cmpe172project.model.Appointment;
import com.beaglescheduler.cmpe172project.model.AvailabilitySlot;
import com.beaglescheduler.cmpe172project.repository.AvailabilitySlotRepository;
import com.beaglescheduler.cmpe172project.service.AppointmentService;
import com.beaglescheduler.cmpe172project.service.SlotUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests covering the three core requirements:
 *   1. Booking flow (happy path)
 *   2. Double-booking prevention
 *   3. Mock remote service call (distribution boundary)
 *
 * Each test runs inside a transaction that is rolled back after the method,
 * so the seed data is always in its original state at the start of each test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingFlowTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AvailabilitySlotRepository slotRepo;

    @Autowired
    private MockMvc mockMvc;

    /**
     * Happy-path booking: a valid available slot must produce a CONFIRMED
     * appointment and atomically flip the slot to unavailable.
     */
    @Test
    void bookAppointment_happyPath_confirmsAppointmentAndMarksSlotUnavailable() {
        Appointment appt = appointmentService.bookAppointment(
                1L, "Jane Test", "jane@test.com", "please be ready early");

        assertThat(appt.getAppointmentId()).isPositive();
        assertThat(appt.getStatus()).isEqualTo("CONFIRMED");
        assertThat(appt.getSlotId()).isEqualTo(1L);

        AvailabilitySlot slot = slotRepo.findById(1L).orElseThrow();
        assertThat(slot.isAvailable()).isFalse();
    }

    /**
     * Double-booking prevention: attempting to book a slot that was just
     * claimed must throw SlotUnavailableException, not silently succeed.
     */
    @Test
    void bookAppointment_doubleBooking_throwsSlotUnavailableException() {
        // First booking claims slot 2
        appointmentService.bookAppointment(2L, "Alice", "alice@test.com", "");

        // Concurrent second attempt on the same slot must be rejected
        assertThatThrownBy(() ->
                appointmentService.bookAppointment(2L, "Bob", "bob@test.com", ""))
                .isInstanceOf(SlotUnavailableException.class);
    }

    /**
     * Mock remote service call: POST /mock/notify (the distribution boundary
     * stub) must accept a JSON payload and return HTTP 200 with status "sent".
     */
    @Test
    void mockNotifyEndpoint_validPayload_returnsSent() throws Exception {
        String payload = """
                {
                  "appointmentId": 1,
                  "notificationType": "booking_confirmed",
                  "channel": "email",
                  "customerEmail": "test@example.com",
                  "machine": "JD6M-2021-001"
                }
                """;

        mockMvc.perform(post("/mock/notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.notificationId").isNotEmpty());
    }
}

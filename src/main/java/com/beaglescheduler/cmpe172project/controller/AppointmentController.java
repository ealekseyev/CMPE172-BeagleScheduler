package com.beaglescheduler.cmpe172project.controller;

import com.beaglescheduler.cmpe172project.model.AvailabilitySlot;
import com.beaglescheduler.cmpe172project.model.Appointment;
import com.beaglescheduler.cmpe172project.service.AppointmentService;
import com.beaglescheduler.cmpe172project.service.SlotService;
import com.beaglescheduler.cmpe172project.service.SlotUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final SlotService slotService;
    private final AppointmentService appointmentService;

    public AppointmentController(SlotService slotService, AppointmentService appointmentService) {
        this.slotService = slotService;
        this.appointmentService = appointmentService;
    }

    @GetMapping("/slots")
    public String listSlots(Model model) {
        List<AvailabilitySlot> slots = slotService.getAvailableSlots();
        model.addAttribute("slots", slots);
        log.info("GET /slots — returned {} available slots", slots.size());
        return "slots";
    }

    @GetMapping("/book")
    public String bookForm(@RequestParam long slotId, Model model) {
        Optional<AvailabilitySlot> slot = slotService.findById(slotId);
        if (slot.isEmpty() || !slot.get().isAvailable()) {
            log.warn("Booking form requested for non-existent or unavailable slotId={}", slotId);
            return "redirect:/slots";
        }
        log.info("Booking form served for slotId={}", slotId);
        model.addAttribute("slot", slot.get());
        return "book";
    }

    @PostMapping("/book")
    public String submitBooking(@RequestParam long slotId,
                                @RequestParam String customerName,
                                @RequestParam String email,
                                @RequestParam(required = false) String customerNotes,
                                RedirectAttributes redirectAttrs) {
        try {
            Appointment appt = appointmentService.bookAppointment(slotId, customerName, email, customerNotes);
            redirectAttrs.addFlashAttribute("appointment", appt);
            return "redirect:/confirmation";
        } catch (SlotUnavailableException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/slots";
        } catch (DataAccessException e) {
            redirectAttrs.addFlashAttribute("error", "Booking unavailable, please try again");
            return "redirect:/slots";
        }
    }

    @GetMapping("/confirmation")
    public String confirmation(Model model) {
        // appointment is added via flash attribute from POST /book
        return "confirmation";
    }

    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        return "appointments";
    }
}

package com.beaglescheduler.cmpe172project.controller;

import com.beaglescheduler.cmpe172project.model.AppUser;
import com.beaglescheduler.cmpe172project.model.AvailabilitySlot;
import com.beaglescheduler.cmpe172project.model.Appointment;
import com.beaglescheduler.cmpe172project.repository.AppUserRepository;
import com.beaglescheduler.cmpe172project.service.AppointmentService;
import com.beaglescheduler.cmpe172project.service.NotificationService;
import com.beaglescheduler.cmpe172project.service.SlotService;
import com.beaglescheduler.cmpe172project.service.SlotUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
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
    private final AppUserRepository userRepo;
    private final NotificationService notificationService;

    public AppointmentController(SlotService slotService,
                                 AppointmentService appointmentService,
                                 AppUserRepository userRepo,
                                 NotificationService notificationService) {
        this.slotService = slotService;
        this.appointmentService = appointmentService;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
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
    public String listAppointments() {
        return "redirect:/my-appointments";
    }

    @GetMapping("/my-appointments")
    public String myAppointments(Authentication auth, Model model) {
        AppUser user = resolveUser(auth);
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            model.addAttribute("appointments", appointmentService.getAllAppointments());
        } else if (user == null) {
            model.addAttribute("appointments", List.of());
        } else {
            model.addAttribute("appointments",
                appointmentService.getAppointmentsForCustomer(user.getUserId()));
        }
        model.addAttribute("isAdmin", isAdmin);
        return "my-appointments";
    }

    @PostMapping("/my-appointments/cancel/{id}")
    public String cancelMyAppointment(@PathVariable long id, Authentication auth,
                                      RedirectAttributes redirectAttrs) {
        AppUser user = resolveUser(auth);
        if (user == null) {
            redirectAttrs.addFlashAttribute("error", "User not found.");
            return "redirect:/my-appointments";
        }
        Appointment appt = appointmentService.getAppointmentById(id);
        if (appt == null || appt.getCustomerId() != user.getUserId()) {
            redirectAttrs.addFlashAttribute("error", "Not authorized to cancel this appointment.");
            return "redirect:/my-appointments";
        }
        appointmentService.cancelAppointment(id);
        redirectAttrs.addFlashAttribute("success", "Appointment #" + id + " has been cancelled.");
        return "redirect:/my-appointments";
    }

    @GetMapping("/my-appointments/reschedule/{id}")
    public String rescheduleForm(@PathVariable long id, Authentication auth, Model model,
                                 RedirectAttributes redirectAttrs) {
        AppUser user = resolveUser(auth);
        if (user == null) {
            redirectAttrs.addFlashAttribute("error", "User not found.");
            return "redirect:/my-appointments";
        }
        Appointment appt = appointmentService.getAppointmentById(id);
        if (appt == null || appt.getCustomerId() != user.getUserId()) {
            redirectAttrs.addFlashAttribute("error", "Not authorized to reschedule this appointment.");
            return "redirect:/my-appointments";
        }
        if (!"CONFIRMED".equals(appt.getStatus())) {
            redirectAttrs.addFlashAttribute("error", "Only CONFIRMED appointments can be rescheduled.");
            return "redirect:/my-appointments";
        }
        model.addAttribute("appointment", appt);
        model.addAttribute("slots", slotService.getAvailableSlots());
        return "reschedule";
    }

    @PostMapping("/my-appointments/reschedule/{id}")
    public String submitReschedule(@PathVariable long id, @RequestParam long newSlotId,
                                   Authentication auth, RedirectAttributes redirectAttrs) {
        AppUser user = resolveUser(auth);
        if (user == null) {
            redirectAttrs.addFlashAttribute("error", "User not found.");
            return "redirect:/my-appointments";
        }
        Appointment appt = appointmentService.getAppointmentById(id);
        if (appt == null || appt.getCustomerId() != user.getUserId()) {
            redirectAttrs.addFlashAttribute("error", "Not authorized to reschedule this appointment.");
            return "redirect:/my-appointments";
        }
        try {
            Appointment newAppt = appointmentService.rescheduleAppointment(id, newSlotId);
            redirectAttrs.addFlashAttribute("appointment", newAppt);
            return "redirect:/confirmation";
        } catch (SlotUnavailableException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/my-appointments/reschedule/" + id;
        }
    }

    private AppUser resolveUser(Authentication auth) {
        if (auth == null) return null;
        return userRepo.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/my-notifications")
    public String myNotifications(Authentication auth, Model model) {
        String email = auth.getName();
        AppUser user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            model.addAttribute("notifications", List.of());
        } else {
            model.addAttribute("notifications",
                notificationService.getNotificationsForUser(user.getUserId()));
        }
        return "my-notifications";
    }
}

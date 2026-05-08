package com.beaglescheduler.cmpe172project.controller;

import com.beaglescheduler.cmpe172project.repository.AppUserRepository;
import com.beaglescheduler.cmpe172project.service.AppointmentService;
import com.beaglescheduler.cmpe172project.service.SlotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AppointmentService appointmentService;
    private final AppUserRepository appUserRepository;
    private final RestTemplate restTemplate;
    private final SlotService slotService;

    public AdminController(AppointmentService appointmentService,
                           AppUserRepository appUserRepository,
                           RestTemplate restTemplate,
                           SlotService slotService) {
        this.appointmentService = appointmentService;
        this.appUserRepository = appUserRepository;
        this.restTemplate = restTemplate;
        this.slotService = slotService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("appointments", appointmentService.getAllAppointments());
        model.addAttribute("users", appUserRepository.findAll());
        return "admin/dashboard";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable long id) {
        appointmentService.cancelAppointment(id);
        log.info("Admin cancelled appointmentId={}", id);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/notify/{id}")
    public String notify(@PathVariable long id) {
        try {
            restTemplate.postForEntity(
                "http://localhost:8080/appointments/" + id + "/notify", null, Map.class);
        } catch (Exception e) {
            log.warn("Notification call failed for appointmentId={}: {}", id, e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/slots")
    public String slots(Model model) {
        model.addAttribute("allSlots", slotService.getAllSlots());
        model.addAttribute("machines", slotService.getAllMachines());
        return "admin/slots";
    }

    @PostMapping("/slots/add")
    public String addSlot(@RequestParam long machineId,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        slotService.createSlot(machineId, startDate, endDate);
        return "redirect:/admin/slots";
    }

    @PostMapping("/slots/{id}/delete")
    public String deleteSlot(@PathVariable long id) {
        slotService.deleteSlot(id);
        return "redirect:/admin/slots";
    }
}

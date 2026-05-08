package com.beaglescheduler.cmpe172project.controller;

import com.beaglescheduler.cmpe172project.model.AppUser;
import com.beaglescheduler.cmpe172project.repository.AppUserRepository;
import com.beaglescheduler.cmpe172project.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/technician")
public class TechnicianController {

    private static final Logger log = LoggerFactory.getLogger(TechnicianController.class);

    private final AppointmentService appointmentService;
    private final AppUserRepository appUserRepository;

    public TechnicianController(AppointmentService appointmentService,
                                AppUserRepository appUserRepository) {
        this.appointmentService = appointmentService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        String email = authentication.getName();
        Optional<AppUser> techOpt = appUserRepository.findByEmail(email);
        if (techOpt.isPresent()) {
            AppUser tech = techOpt.get();
            model.addAttribute("appointments",
                appointmentService.getAppointmentsForTechnician(tech.getUserId()));
            model.addAttribute("techName", tech.getName());
        } else {
            model.addAttribute("appointments", List.of());
            model.addAttribute("techName", email);
        }
        return "technician/dashboard";
    }

    @PostMapping("/ready/{id}")
    public String markReady(@PathVariable long id) {
        appointmentService.markMachineReady(id);
        log.info("Technician marked machine ready for appointmentId={}", id);
        return "redirect:/technician/dashboard";
    }
}

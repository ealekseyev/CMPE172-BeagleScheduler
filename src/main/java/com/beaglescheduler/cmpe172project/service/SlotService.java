package com.beaglescheduler.cmpe172project.service;

import com.beaglescheduler.cmpe172project.model.AvailabilitySlot;
import com.beaglescheduler.cmpe172project.model.Machine;
import com.beaglescheduler.cmpe172project.repository.AvailabilitySlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SlotService {

    private final AvailabilitySlotRepository slotRepo;

    public SlotService(AvailabilitySlotRepository slotRepo) {
        this.slotRepo = slotRepo;
    }

    public List<AvailabilitySlot> getAvailableSlots() {
        return slotRepo.findAvailable();
    }

    public Optional<AvailabilitySlot> findById(long slotId) {
        return slotRepo.findById(slotId);
    }

    public List<AvailabilitySlot> getAllSlots() {
        return slotRepo.findAll();
    }

    public List<Machine> getAllMachines() {
        return slotRepo.findAllMachines();
    }

    public void createSlot(long machineId, LocalDate start, LocalDate end) {
        slotRepo.createSlot(machineId, start, end);
    }

    public void deleteSlot(long slotId) {
        slotRepo.deleteSlot(slotId);
    }
}

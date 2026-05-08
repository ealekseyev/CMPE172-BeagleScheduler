package com.beaglescheduler.cmpe172project.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// Carries denormalized display fields from JOIN across appointments, users, machines, slots
public class Appointment {
    private long appointmentId;
    private long slotId;
    private long customerId;
    private long machineId;
    private String status;
    private String customerNotes;
    private LocalDateTime bookedAt;
    // Joined display fields
    private String customerName;
    private String customerEmail;
    private String serialNumber;
    private String modelName;
    private double dailyRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private long assignedTechnicianId;
    private String assignedTechnicianName;
    private boolean machineReady;

    public long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(long appointmentId) { this.appointmentId = appointmentId; }

    public long getSlotId() { return slotId; }
    public void setSlotId(long slotId) { this.slotId = slotId; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public long getMachineId() { return machineId; }
    public void setMachineId(long machineId) { this.machineId = machineId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCustomerNotes() { return customerNotes; }
    public void setCustomerNotes(String customerNotes) { this.customerNotes = customerNotes; }

    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public long getAssignedTechnicianId() { return assignedTechnicianId; }
    public void setAssignedTechnicianId(long assignedTechnicianId) { this.assignedTechnicianId = assignedTechnicianId; }

    public String getAssignedTechnicianName() { return assignedTechnicianName; }
    public void setAssignedTechnicianName(String assignedTechnicianName) { this.assignedTechnicianName = assignedTechnicianName; }

    public boolean isMachineReady() { return machineReady; }
    public void setMachineReady(boolean machineReady) { this.machineReady = machineReady; }

    public long getDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public double getTotalCost() {
        return dailyRate * getDurationDays();
    }
}

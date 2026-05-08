package com.beaglescheduler.cmpe172project.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Denormalized view model: carries display fields from JOIN across slots, machines, services
public class AvailabilitySlot {
    private long slotId;
    private long machineId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean available;
    // Joined display fields
    private String serialNumber;
    private String modelName;
    private double dailyRate;

    public long getSlotId() { return slotId; }
    public void setSlotId(long slotId) { this.slotId = slotId; }

    public long getMachineId() { return machineId; }
    public void setMachineId(long machineId) { this.machineId = machineId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }

    /** Number of days in this rental window (inclusive). */
    public long getDurationDays() {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** Total cost = daily rate × duration. */
    public double getTotalCost() {
        return dailyRate * getDurationDays();
    }
}

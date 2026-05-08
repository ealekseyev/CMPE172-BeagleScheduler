package com.beaglescheduler.cmpe172project.model;

public class MachineModel {
    private long serviceId;
    private String modelName;
    private String description;
    private double dailyRate;

    public long getServiceId() { return serviceId; }
    public void setServiceId(long serviceId) { this.serviceId = serviceId; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }
}

package com.beaglescheduler.cmpe172project.model;

public class Machine {
    private long machineId;
    private long serviceId;
    private String serialNumber;
    private int yearBuilt;
    private String conditionNotes;
    private String modelName;

    public long getMachineId() { return machineId; }
    public void setMachineId(long machineId) { this.machineId = machineId; }

    public long getServiceId() { return serviceId; }
    public void setServiceId(long serviceId) { this.serviceId = serviceId; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public int getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(int yearBuilt) { this.yearBuilt = yearBuilt; }

    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
}

package com.example.barangaygo.models;

public class Slot {
    private String id;
    private String serviceId;
    private String serviceName;
    private String date;        // "YYYY-MM-DD"
    private String timeRange;   // "8:00 AM – 10:00 AM"
    private long maxCapacity;
    private long currentCount;
    private String status;      // "open" | "closed"

    public Slot() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public long getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(long maxCapacity) { this.maxCapacity = maxCapacity; }

    public long getCurrentCount() { return currentCount; }
    public void setCurrentCount(long currentCount) { this.currentCount = currentCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
package com.example.barangaygo.models;

import com.google.firebase.Timestamp;

public class Booking {
    private String id;
    private String userId;
    private String barangayId;
    private String serviceId;
    private String serviceName;
    private String slotId;
    private String queueNumber;
    private String residentName;
    private String status; // waiting, serving, done, skipped
    private long aheadCount;
    private String date;
    private String timeRange;
    private Timestamp createdAt;

    public Booking() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBarangayId() { return barangayId; }
    public void setBarangayId(String barangayId) { this.barangayId = barangayId; }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getQueueNumber() { return queueNumber; }
    public void setQueueNumber(String queueNumber) { this.queueNumber = queueNumber; }

    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getAheadCount() { return aheadCount; }
    public void setAheadCount(long aheadCount) { this.aheadCount = aheadCount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
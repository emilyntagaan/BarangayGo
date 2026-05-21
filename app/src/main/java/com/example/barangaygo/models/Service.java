package com.example.barangaygo.models;

public class Service {
    private String id;
    private String name;
    private String description;
    private java.util.List<String> requirements;
    private double fee;
    private int estimatedMinutes;
    private String timeUnit; // "min" or "hr"
    private boolean isAvailable;
    private String iconKey;

    public Service() {}

    public Service(String id, String name, String description,
                   int estimatedMinutes, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.estimatedMinutes = estimatedMinutes;
        this.isAvailable = isAvailable;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public java.util.List<String> getRequirements() { return requirements; }
    public void setRequirements(java.util.List<String> requirements) { this.requirements = requirements; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public String getTimeUnit() { return timeUnit; }
    public void setTimeUnit(String timeUnit) { this.timeUnit = timeUnit; }

    public String getFormattedTime() {
        if (estimatedMinutes <= 0) return "—";
        if ("hr".equals(timeUnit)) {
            int hrs = estimatedMinutes / 60;
            int mins = estimatedMinutes % 60;
            return mins > 0 ? "~" + hrs + " hr " + mins + " min" : "~" + hrs + " hr";
        }
        return "~" + estimatedMinutes + " min";
    }

    @com.google.firebase.firestore.PropertyName("isAvailable")
    public boolean isAvailable() { return isAvailable; }

    @com.google.firebase.firestore.PropertyName("isAvailable")
    public void setAvailable(boolean available) { isAvailable = available; }

    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
}

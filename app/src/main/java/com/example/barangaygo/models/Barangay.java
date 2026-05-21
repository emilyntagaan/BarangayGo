package com.example.barangaygo.models;

import com.google.firebase.Timestamp;

public class Barangay {
    private String id;
    private String name;
    private String code;
    private String address;
    private String municipality;
    private String province;
    private String contactNumber;
    private String email;
    private String description;
    private String logoUrl;
    private String adminUid;
    private String status;
    private Timestamp createdAt;

    public Barangay() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getAdminUid() { return adminUid; }
    public void setAdminUid(String adminUid) { this.adminUid = adminUid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getDisplayLocation() {
        if (municipality != null && province != null) return municipality + ", " + province;
        if (municipality != null) return municipality;
        if (province != null) return province;
        return "";
    }
}
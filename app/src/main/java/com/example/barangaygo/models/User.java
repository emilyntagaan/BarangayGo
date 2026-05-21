package com.example.barangaygo.models;

import com.google.firebase.Timestamp;

public class User {
    private String id;
    private String name;
    private String email;
    private String contact;
    private String address;
    private String role; // "resident" or "admin"
    private String barangayId;
    private Timestamp createdAt;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getBarangayId() { return barangayId; }
    public void setBarangayId(String barangayId) { this.barangayId = barangayId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() { return "admin".equals(role); }
}

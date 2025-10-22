package com.entities;

import com.enums.Role;

import java.time.LocalDate;

/**
 * @author Bùi Quốc Trụ, Tô Thanh Hậu
 */
public class Staff {
    private final String id;
    private Role role;
    private String username;
    private String fullName;
    private String licenseNumber;
    private String phoneNumber;
    private String email;
    private final LocalDate hireDate;
    private boolean isActive;

    public Staff(String id, Role role, String username, String fullName, String licenseNumber, String phoneNumber, String email, LocalDate hireDate, boolean isActive) {
        this.id = id;
        this.role = role;
        this.username = username;
        this.fullName = fullName;
        this.licenseNumber = licenseNumber;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.hireDate = hireDate;
        this.isActive = isActive;
    }



    public String getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}

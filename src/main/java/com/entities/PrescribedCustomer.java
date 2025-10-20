package com.entities;

import java.time.LocalDateTime;

/**
 * @author Bùi Quốc Trụ
 */
public class PrescribedCustomer {
    private final String id;
    private String name;
    private String phoneNumber;
    private String address;
    private final LocalDateTime creationDate;

    public PrescribedCustomer(String id, String name, String phoneNumber, String address) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.creationDate = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
